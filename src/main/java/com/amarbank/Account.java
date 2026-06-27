package com.amarbank;

import com.amarbank.exception.BankException;
import com.amarbank.exception.InvalidAmountException;

/**
 * Abstract base class for every account held at Amar Bank.
 *
 * <p>The assignment requires strict encapsulation: all fields are private and
 * are reached only through public getters/setters.</p>
 *
 * <p>Attributes (per spec): {@code accountNumber}, {@code accountHolderName},
 * {@code branch}, {@code phone}, {@code balance}.</p>
 *
 * <p>Abstract methods: {@link #withdraw(double)} and {@link #deposit(double)} -
 * each subclass defines its own financial rules.</p>
 *
 * <p>Common methods: {@link #transferFunds(Account, double)},
 * {@link #updatePhone(String)} and {@link #shiftBranch(String)}.</p>
 */
public abstract class Account {

    private final String accountNumber;
    private String accountHolderName;
    private String branch;
    private String phone;
    private double balance;

    protected Account(String accountNumber, String accountHolderName,
                      String branch, String phone, double balance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.branch = branch;
        this.phone = phone;
        this.balance = balance;
    }

    // ---------- encapsulated state (getters / setters) ----------

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    /** Account category label used for the CSV file and the display table. */
    public abstract String getType();

    // ---------- abstract financial operations ----------

    /** Adds funds to the account using the subclass's deposit rule. */
    public abstract void deposit(double amount) throws BankException;

    /** Removes funds from the account using the subclass's withdraw rule. */
    public abstract void withdraw(double amount) throws BankException;

    // ---------- common operations shared by all accounts ----------

    /**
     * Moves {@code amount} from this account to {@code target}. The source
     * account's {@link #withdraw(double)} rule is applied first (so it can
     * reject the move), then the target's {@link #deposit(double)} rule.
     */
    public void transferFunds(Account target, double amount) throws BankException {
        if (target == null) {
            throw new BankException("Transfer destination account is missing.");
        }
        if (target == this) {
            throw new BankException("Cannot transfer to the same account.");
        }
        validatePositiveAmount(amount);
        withdraw(amount);
        target.deposit(amount);
    }

    public void updatePhone(String newPhone) {
        this.phone = newPhone;
    }

    public void shiftBranch(String newBranch) {
        this.branch = newBranch;
    }

    // ---------- helpers ----------

    /** Rejects amounts that are zero, negative, NaN or infinite. */
    protected void validatePositiveAmount(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    /**
     * The amount that can be withdrawn right now under this account's rules.
     * For savings this is the balance; for a loan it is the remaining headroom
     * under the loan limit. Used by the GUI's balance/summary display.
     */
    public abstract double withdrawableBalance();
}
