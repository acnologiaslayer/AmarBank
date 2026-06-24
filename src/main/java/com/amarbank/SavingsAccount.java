package com.amarbank;

import com.amarbank.exception.BankException;

public class SavingsAccount extends Account {

    public SavingsAccount(String accountNumber,String type, String accountHolderName, String branch, String phone, double balance, double interestRate) {
        super(accountNumber, type, accountHolderName, branch, phone, balance);
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
            throw new BankException("Insufficient balance. Available balance: " + String.format("%.2f", getBalance()));
        }
        setBalance(getBalance() - amount);
    }

    @Override
    public double withdrawableBalance() {
        return getBalance();
    }
}
