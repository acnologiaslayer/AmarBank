package com.amarbank;

import com.amarbank.exception.BankException;

/**
 * A standard savings account.
 *
 * <p>Rules (per spec):</p>
 * <ul>
 *   <li>{@code deposit} increases the balance.</li>
 *   <li>{@code withdraw} decreases the balance <em>only if</em> the account
 *       has sufficient funds.</li>
 * </ul>
 */
public class SavingsAccount extends Account {

    public static final String TYPE = "SAVINGS";

    public SavingsAccount(String accountNumber, String accountHolderName,
                          String branch, String phone, double balance) {
        super(accountNumber, accountHolderName, branch, phone, balance);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void deposit(double amount) throws BankException {
        validatePositiveAmount(amount);
        setBalance(getBalance() + amount);
    }

    @Override
    public void withdraw(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (getBalance() < amount) {
            throw new BankException("Insufficient funds. Available balance: "
                    + String.format("%.2f", getBalance()));
        }
        setBalance(getBalance() - amount);
    }

    @Override
    public double withdrawableBalance() {
        return getBalance();
    }
}
