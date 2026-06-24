package com.amarbank;

import com.amarbank.exception.BankException;

public class LoanAccount extends Account {
    private double loanLimit;
    private double amountDue;

    public LoanAccount(String accountNumber, String type, String accountHolderName, String branch, String phone, double balance, double loanLimit, double amountDue) {
        super(accountNumber, type, accountHolderName, branch, phone, balance);
        this.loanLimit = loanLimit;
        this.amountDue = amountDue;
    }

    public double getLoanLimit() {
        return loanLimit;
    }

    public double getAmountDue() {
        return amountDue;
    }

    @Override
    public double withdrawableBalance() {
        return loanLimit - amountDue;
    }

    @Override
    public void deposit(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amountDue <= 0) {
            throw new BankException("This loan is already fully repaid.");
        }
        if (amount > amountDue) {
            throw new BankException("Payment exceeds remaining loan due. Remaining due: "
                    + String.format("%.2f", amountDue));
        }
        amountDue -= amount;
    }

    @Override
    public void withdraw(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amount + amountDue > loanLimit) {
            throw new BankException("Withdrawal exceeds loan limit. Withdrawable amount: "
                    + String.format("%.2f", withdrawableBalance()));
        }
        amountDue += amount;
        setBalance(getBalance() + amount);
    }
}
