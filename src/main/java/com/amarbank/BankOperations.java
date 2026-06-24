package com.amarbank;

import com.amarbank.exception.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BankOperations {

    private final BankManagement bankManagement;
    private final Map<String, Account> accounts;
    private int nextAccountSequence;

    public BankOperations(BankManagement bankManagement) throws DataStoreException {
        this.bankManagement = bankManagement;
        this.accounts = new LinkedHashMap<>();
        for (Account account : bankManagement.loadAccounts()) {
            this.accounts.put(account.getAccountNumber(), account);
        }
        this.nextAccountSequence = highestExistingSequence() + 1;
    }

    /** Opens a new account and returns it. */
    public Account openAccount(String type, String name, String branch, String phone, double openingBalance)
            throws BankException {
        if (openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }

        String number = nextAccountNumber();
        if (accounts.containsKey(number)) {
            throw new DuplicateAccountException(number);
        }

        Account account = switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, type, name, branch, phone, openingBalance, 0.01);
            case "LOAN" -> new LoanAccount(number, type, name, branch, phone, openingBalance, 10000.0, 0.0);
            default -> throw new BankException("Unknown account type: " + type);
        };

        accounts.put(number, account);
        bankManagement.addAccount(account);
        return account;
    }

    public void deposit(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);
        bankManagement.updateAccount(account);
    }

    public void withdraw(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);
        bankManagement.updateAccount(account);
    }

    /** Moves money between two accounts atomically (in-memory). */
    public void transfer(String fromNumber, String toNumber, double amount)
            throws BankException {
        if (fromNumber.equals(toNumber)) {
            throw new BankException("Cannot transfer to the same account.");
        }
        Account from = requireAccount(fromNumber);
        Account to = requireAccount(toNumber);

        from.withdraw(amount); // validates amount and funds first
        to.deposit(amount);

        bankManagement.updateAccount(from);
        bankManagement.updateAccount(to);
    }

    public void updateAccountDetails(String accountNumber, String branch, String phone) throws BankException {
        Account account = requireAccount(accountNumber);
        if (branch == null || branch.isBlank()) {
            throw new BankException("Branch cannot be empty.");
        }
        if (phone == null || phone.isBlank()) {
            throw new BankException("Phone number cannot be empty.");
        }
        account.shiftBranch(branch.trim());
        account.updatePhone(phone.trim());
        bankManagement.updateAccount(account);
    }

    public void deleteAccount(String accountNumber) throws BankException {
        Account account = requireAccount(accountNumber);
        if (Math.abs(account.getBalance()) > 0.000001) {
            throw new BankException("Only zero-balance accounts can be closed. Current balance: "
                    + String.format("%.2f", account.getBalance()));
        }
        if (account instanceof LoanAccount loanAccount && loanAccount.getAmountDue() > 0.000001) {
            throw new BankException("Loan account cannot be closed while amount due remains: "
                    + String.format("%.2f", loanAccount.getAmountDue()));
        }
        bankManagement.deleteAccount(accountNumber);
        accounts.remove(accountNumber);
    }

    public Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Returns every account, in creation order, for display purposes. */
    public List<Account> listAccounts() {
        return new ArrayList<>(accounts.values());
    }

    // ---------- helpers ----------

    private String nextAccountNumber() {
        return String.format("AC%05d", nextAccountSequence++);
    }

    private int highestExistingSequence() {
        int highest = 0;
        for (String number : accounts.keySet()) {
            try {
                highest = Math.max(highest, Integer.parseInt(number.substring(2)));
            } catch (NumberFormatException ignored) {
                // non-standard account number in the file; skip it
            }
        }
        return highest;
    }
}
