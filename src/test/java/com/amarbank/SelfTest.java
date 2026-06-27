package com.amarbank;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.amarbank.exception.BankException;
import com.amarbank.storage.AccountStore;
import com.amarbank.storage.CsvAccountStore;
import com.amarbank.storage.SqliteAccountStore;

/**
 * Headless verification harness for the Amar Bank core logic.
 *
 * <p>This is intentionally dependency-free (no JUnit) so it runs anywhere the
 * compiled classes do. It exercises the regex validators, the Savings/Loan
 * account rules, the CSV persistence round-trip and (when the driver is
 * present) the optional SQLite store. Run with:</p>
 *
 * <pre>java -cp target/classes com.amarbank.SelfTest</pre>
 */
public class SelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testValidators();
        testSavingsRules();
        testLoanRules();
        testTransfer();
        testAccountNumbering();
        testCsvRoundTrip();
        testSqliteRoundTripIfAvailable();

        System.out.printf("%n==== %d passed, %d failed ====%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------- Section 2: regex validators ----------

    private static void testValidators() {
        section("Validators (regex)");

        check("phone 01712345678 valid", Validators.isValidPhone("01712345678"));
        check("phone 01312345678 valid", Validators.isValidPhone("01312345678"));
        check("phone too short invalid", !Validators.isValidPhone("0171234567"));
        check("phone 12 digits invalid", !Validators.isValidPhone("017123456789"));
        check("phone non-01 prefix invalid", !Validators.isValidPhone("02712345678"));
        check("phone 0121... invalid (2nd digit <3)", !Validators.isValidPhone("01212345678"));
        check("phone with letters invalid", !Validators.isValidPhone("0171234567a"));

        check("account AMB-55412 valid", Validators.isValidAccountNumber("AMB-55412"));
        check("account AMB-00001 valid", Validators.isValidAccountNumber("AMB-00001"));
        check("account amb-55412 invalid (lowercase)", !Validators.isValidAccountNumber("amb-55412"));
        check("account AMB-5541 invalid (4 digits)", !Validators.isValidAccountNumber("AMB-5541"));
        check("account AMB-554123 invalid (6 digits)", !Validators.isValidAccountNumber("AMB-554123"));
        check("account AC00001 invalid", !Validators.isValidAccountNumber("AC00001"));

        check("name ALICE RAHMAN valid", Validators.isValidHolderName("ALICE RAHMAN"));
        check("name AB valid (min 2)", Validators.isValidHolderName("AB"));
        check("name A invalid (1 char)", !Validators.isValidHolderName("A"));
        check("name lower invalid", !Validators.isValidHolderName("Alice"));
        check("name with digit invalid", !Validators.isValidHolderName("ALICE1"));
        check("name 51 chars invalid", !Validators.isValidHolderName("A".repeat(51)));

        check("branch Dhaka Main valid", Validators.isValidBranchName("Dhaka Main"));
        check("branch with digit invalid", !Validators.isValidBranchName("Dhaka 1"));
        check("branch empty invalid", !Validators.isValidBranchName(""));
    }

    // ---------- Section 1: account rules ----------

    private static void testSavingsRules() throws BankException {
        section("SavingsAccount rules");
        SavingsAccount s = new SavingsAccount("AMB-00001", "ALICE", "Dhaka", "01712345678", 100.0);

        s.deposit(50);
        check("deposit increases balance", eq(s.getBalance(), 150.0));

        s.withdraw(30);
        check("withdraw decreases balance", eq(s.getBalance(), 120.0));

        check("withdraw beyond funds rejected", throwsBank(() -> s.withdraw(1000)));
        check("balance unchanged after failed withdraw", eq(s.getBalance(), 120.0));
        check("negative deposit rejected", throwsBank(() -> s.deposit(-5)));
        check("zero withdraw rejected", throwsBank(() -> s.withdraw(0)));
    }

    private static void testLoanRules() throws BankException {
        section("LoanAccount rules");
        // limit 10000, nothing owed/disbursed initially
        LoanAccount loan = new LoanAccount("AMB-00002", "BOB", "Dhaka", "01812345678", 0.0, 10000.0, 0.0);

        loan.withdraw(4000); // borrow
        check("borrow increases amountDue", eq(loan.getAmountDue(), 4000.0));
        check("borrow disburses to balance", eq(loan.getBalance(), 4000.0));
        check("withdrawable = limit - due", eq(loan.withdrawableBalance(), 6000.0));

        check("borrow beyond limit rejected", throwsBank(() -> loan.withdraw(7000)));
        check("amountDue unchanged after rejected borrow", eq(loan.getAmountDue(), 4000.0));

        loan.deposit(1500); // repay
        check("repay reduces amountDue", eq(loan.getAmountDue(), 2500.0));
        check("repay reduces balance", eq(loan.getBalance(), 2500.0));

        check("repay beyond due rejected", throwsBank(() -> loan.deposit(99999)));

        // boundary: borrow exactly up to the limit is allowed
        LoanAccount exact = new LoanAccount("AMB-00003", "CARL", "Dhaka", "01912345678", 0.0, 5000.0, 0.0);
        boolean ok = !throwsBank(() -> exact.withdraw(5000));
        check("borrow up to exact limit allowed", ok && eq(exact.getAmountDue(), 5000.0));
        check("any further borrow rejected", throwsBank(() -> exact.withdraw(0.01)));
    }

    private static void testTransfer() throws BankException {
        section("transferFunds");
        SavingsAccount a = new SavingsAccount("AMB-00010", "AA", "Dhaka", "01712345678", 500.0);
        SavingsAccount b = new SavingsAccount("AMB-00011", "BB", "Dhaka", "01712345679", 100.0);
        a.transferFunds(b, 200);
        check("source debited", eq(a.getBalance(), 300.0));
        check("target credited", eq(b.getBalance(), 300.0));
        check("transfer over funds rejected", throwsBank(() -> a.transferFunds(b, 100000)));
        check("self-transfer rejected", throwsBank(() -> a.transferFunds(a, 10)));
    }

    private static void testAccountNumbering() throws Exception {
        section("Account numbering (AMB-XXXXX)");
        Path dir = Files.createTempDirectory("amarbank-num");
        AccountStore store = new CsvAccountStore(dir.resolve("accounts.csv"));
        BankManagement bank = new BankManagement(store);

        Account first = bank.openAccount("SAVINGS", "ALICE", "Dhaka", "01712345678", 100);
        Account second = bank.openAccount("LOAN", "BOB", "Dhaka", "01812345678", 10000);
        check("first number is AMB-00001", first.getAccountNumber().equals("AMB-00001"));
        check("second number is AMB-00002", second.getAccountNumber().equals("AMB-00002"));
        check("numbers match regex", Validators.isValidAccountNumber(first.getAccountNumber())
                && Validators.isValidAccountNumber(second.getAccountNumber()));

        // reload should continue numbering, not collide
        BankManagement reloaded = new BankManagement(new CsvAccountStore(dir.resolve("accounts.csv")));
        Account third = reloaded.openAccount("SAVINGS", "CARL", "Dhaka", "01912345678", 50);
        check("numbering continues after reload (AMB-00003)", third.getAccountNumber().equals("AMB-00003"));

        check("open with bad name rejected", throwsBank(
                () -> bank.openAccount("SAVINGS", "lowercase", "Dhaka", "01712345678", 100)));
        check("open with bad phone rejected", throwsBank(
                () -> bank.openAccount("SAVINGS", "DAVE", "Dhaka", "12345", 100)));
    }

    private static void testCsvRoundTrip() throws Exception {
        section("CSV persistence round-trip");
        Path dir = Files.createTempDirectory("amarbank-csv");
        Path csv = dir.resolve("accounts.csv");

        BankManagement bank = new BankManagement(new CsvAccountStore(csv));
        Account savings = bank.openAccount("SAVINGS", "ALICE RAHMAN", "Dhaka Main", "01712345678", 250.50);
        Account loan = bank.openAccount("LOAN", "BOB KHAN", "Chittagong", "01812345678", 10000);
        bank.withdraw(loan.getAccountNumber(), 3000); // borrow on the loan
        bank.deposit(savings.getAccountNumber(), 100); // top up savings

        check("CSV file was written", Files.exists(csv));
        String content = Files.readString(csv);
        check("CSV has header row", content.startsWith("accountNumber,type,"));

        // Reload into a fresh controller and confirm everything survived.
        BankManagement reloaded = new BankManagement(new CsvAccountStore(csv));
        List<Account> all = reloaded.listAccounts();
        check("two accounts reloaded", all.size() == 2);

        Account rSavings = reloaded.requireAccount(savings.getAccountNumber());
        check("savings balance persisted", eq(rSavings.getBalance(), 350.50));
        check("savings type persisted", rSavings instanceof SavingsAccount);
        check("savings holder persisted", rSavings.getAccountHolderName().equals("ALICE RAHMAN"));

        Account rLoanRaw = reloaded.requireAccount(loan.getAccountNumber());
        check("loan type persisted", rLoanRaw instanceof LoanAccount);
        LoanAccount rLoan = (LoanAccount) rLoanRaw;
        check("loan limit persisted", eq(rLoan.getLoanLimit(), 10000.0));
        check("loan amountDue persisted", eq(rLoan.getAmountDue(), 3000.0));
        check("loan balance persisted", eq(rLoan.getBalance(), 3000.0));

        // search
        check("search by holder finds account", reloaded.search("ALICE").size() == 1);
        check("search by branch finds account", reloaded.search("chittagong").size() == 1);
        check("blank search returns all", reloaded.search("").size() == 2);
    }

    private static void testSqliteRoundTripIfAvailable() throws Exception {
        section("SQLite persistence round-trip (optional)");
        if (!SqliteAccountStore.isAvailable()) {
            System.out.println("  [skipped] SQLite JDBC driver not on classpath.");
            return;
        }
        Path dir = Files.createTempDirectory("amarbank-sqlite");
        Path db = dir.resolve("bank.db");

        BankManagement bank = new BankManagement(new SqliteAccountStore(db));
        Account savings = bank.openAccount("SAVINGS", "ALICE", "Dhaka", "01712345678", 500);
        Account loan = bank.openAccount("LOAN", "BOB", "Dhaka", "01812345678", 8000);
        bank.withdraw(loan.getAccountNumber(), 2000);

        BankManagement reloaded = new BankManagement(new SqliteAccountStore(db));
        check("sqlite reloaded two accounts", reloaded.listAccounts().size() == 2);
        check("sqlite savings balance", eq(reloaded.requireAccount(savings.getAccountNumber()).getBalance(), 500.0));
        LoanAccount rLoan = (LoanAccount) reloaded.requireAccount(loan.getAccountNumber());
        check("sqlite loan amountDue", eq(rLoan.getAmountDue(), 2000.0));
        check("sqlite loan limit", eq(rLoan.getLoanLimit(), 8000.0));
    }

    // ---------- tiny test framework ----------

    private interface BankAction {
        void run() throws BankException;
    }

    private static boolean throwsBank(BankAction action) {
        try {
            action.run();
            return false;
        } catch (BankException e) {
            return true;
        }
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static void section(String name) {
        System.out.println("\n-- " + name + " --");
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + label);
        } else {
            failed++;
            System.out.println("  [FAIL] " + label);
        }
    }
}
