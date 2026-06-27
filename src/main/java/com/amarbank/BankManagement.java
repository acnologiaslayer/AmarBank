package com.amarbank;

import java.util.ArrayList;
import java.util.List;

import com.amarbank.exception.AccountNotFoundException;
import com.amarbank.exception.BankException;
import com.amarbank.exception.DataStoreException;
import com.amarbank.exception.DuplicateAccountException;
import com.amarbank.exception.InvalidAmountException;
import com.amarbank.storage.AccountStore;

/**
 * Controller class (per spec).
 *
 * <p>Holds an {@link ArrayList}{@code <Account>} of all active accounts and is
 * responsible for all banking logic plus reading/writing the permanent store.
 * The store is pluggable ({@link AccountStore}); by default it is a CSV file,
 * with an optional SQLite backend.</p>
 *
 * <p>The in-memory list is the single source of truth during a session; every
 * mutating operation persists the whole roster afterwards, so the file/database
 * on disk always reflects the latest state.</p>
 */
public class BankManagement {

    /** All active accounts (required by the spec). */
    private final ArrayList<Account> accounts;

    private final AccountStore store;
    private int nextSequence;

    public BankManagement(AccountStore store) throws DataStoreException {
        this.store = store;
        this.accounts = new ArrayList<>(store.loadAll());
        this.nextSequence = highestExistingSequence() + 1;
    }

    // ---------- account creation ----------

    /**
     * Opens a new account, assigns it the next {@code AMB-XXXXX} number, stores
     * it and persists the roster.
     *
     * @param type           {@code "SAVINGS"} or {@code "LOAN"}
     * @param name           holder name (validated against the spec rules)
     * @param branch         branch name (validated)
     * @param phone          phone number (validated)
     * @param openingValue   opening balance (savings) or loan principal/limit (loan)
     */
    public Account openAccount(String type, String name, String branch, String phone,
                               double openingValue) throws BankException {
        validateFields(name, branch, phone);
        if (!Double.isFinite(openingValue) || openingValue < 0) {
            throw new InvalidAmountException("Opening amount cannot be negative.");
        }

        String number = nextAccountNumber();
        if (findAccount(number) != null) {
            throw new DuplicateAccountException(number);
        }

        Account account;
        if (SavingsAccount.TYPE.equalsIgnoreCase(type)) {
            account = new SavingsAccount(number, name, branch, phone, openingValue);
        } else if (LoanAccount.TYPE.equalsIgnoreCase(type)) {
            // For a loan account the opening figure is the loan limit. No money
            // is owed or disbursed until the customer actually withdraws.
            account = new LoanAccount(number, name, branch, phone, 0.0, openingValue, 0.0);
        } else {
            throw new BankException("Unknown account type: " + type);
        }

        accounts.add(account);
        persist();
        return account;
    }

    // ---------- transactions ----------

    public void deposit(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);
        persist();
    }

    public void withdraw(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);
        persist();
    }

    /** Transfers funds between two accounts using {@link Account#transferFunds}. */
    public void transfer(String fromNumber, String toNumber, double amount) throws BankException {
        Account from = requireAccount(fromNumber);
        Account to = requireAccount(toNumber);
        from.transferFunds(to, amount);
        persist();
    }

    // ---------- modifications ----------

    /** Updates a phone number (validated) and persists. */
    public void updatePhone(String accountNumber, String newPhone) throws BankException {
        if (!Validators.isValidPhone(newPhone)) {
            throw new BankException(Validators.PHONE_RULE);
        }
        Account account = requireAccount(accountNumber);
        account.updatePhone(newPhone.trim());
        persist();
    }

    /** Shifts an account to a new branch (validated) and persists. */
    public void shiftBranch(String accountNumber, String newBranch) throws BankException {
        if (!Validators.isValidBranchName(newBranch)) {
            throw new BankException(Validators.BRANCH_NAME_RULE);
        }
        Account account = requireAccount(accountNumber);
        account.shiftBranch(newBranch.trim());
        persist();
    }

    // ---------- queries ----------

    /** Returns the account or throws if the number is unknown. */
    public Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = findAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Returns every account (creation order) for the display table. */
    public List<Account> listAccounts() {
        return new ArrayList<>(accounts);
    }

    /**
     * Returns accounts whose number, holder name, branch, phone or type
     * contains {@code query} (case-insensitive). A blank query returns all.
     * Backs the "searchable roster" requirement of the Display Panel.
     */
    public List<Account> search(String query) {
        if (query == null || query.isBlank()) {
            return listAccounts();
        }
        String needle = query.trim().toLowerCase();
        List<Account> results = new ArrayList<>();
        for (Account a : accounts) {
            if (a.getAccountNumber().toLowerCase().contains(needle)
                    || a.getAccountHolderName().toLowerCase().contains(needle)
                    || a.getBranch().toLowerCase().contains(needle)
                    || a.getPhone().toLowerCase().contains(needle)
                    || a.getType().toLowerCase().contains(needle)) {
                results.add(a);
            }
        }
        return results;
    }

    /** Where the data is being stored (for the status bar). */
    public String storageDescription() {
        return store.describe();
    }

    // ---------- internals ----------

    private void validateFields(String name, String branch, String phone) throws BankException {
        if (!Validators.isValidHolderName(name)) {
            throw new BankException(Validators.HOLDER_NAME_RULE);
        }
        if (!Validators.isValidBranchName(branch)) {
            throw new BankException(Validators.BRANCH_NAME_RULE);
        }
        if (!Validators.isValidPhone(phone)) {
            throw new BankException(Validators.PHONE_RULE);
        }
    }

    private Account findAccount(String accountNumber) {
        for (Account account : accounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    /** Writes the whole roster to permanent storage (CSV file by default). */
    private void persist() throws DataStoreException {
        store.saveAll(accounts);
    }

    private String nextAccountNumber() {
        return String.format("AMB-%05d", nextSequence++);
    }

    /** Largest 5-digit suffix already in use, so new numbers never collide. */
    private int highestExistingSequence() {
        int highest = 0;
        for (Account account : accounts) {
            String number = account.getAccountNumber();
            if (Validators.isValidAccountNumber(number)) {
                int value = Integer.parseInt(number.substring(4));
                highest = Math.max(highest, value);
            }
        }
        return highest;
    }
}
