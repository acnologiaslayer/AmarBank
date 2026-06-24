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
        double remainingPayment = amount;
        if (amountDue > 0) {
            double paidToLoan = Math.min(amountDue, remainingPayment);
            amountDue -= paidToLoan;
            remainingPayment -= paidToLoan;
        }
        setBalance(getBalance() + remainingPayment);
    }

    @Override
    public void withdraw(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amount + amountDue > loanLimit) {
            throw new BankException("Withdrawal exceeds loan limit. Withdrawable amount: "
                    + String.format("%.2f", withdrawableBalance()));
        }
        amountDue += amount;
        setBalance(getBalance() - amount);
    }
}
