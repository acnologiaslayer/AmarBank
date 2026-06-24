package com.amarbank;

import com.amarbank.exception.BankException;
import com.amarbank.exception.InvalidAmountException;

public abstract class Account {
    private String accountNumber;
    private String type;
    private String accountHolderName;
    private String branch;
    private String phone;
    private double balance;

    public Account(String accountNumber, String type, String accountHolderName, String branch, String phone, double balance) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.accountHolderName = accountHolderName;
        this.branch = branch;
        this.phone = phone;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getBranch() {
        return branch;
    }

    public String getPhone() {
        return phone;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getType() {
        return type;
    }

    public abstract void deposit(double amount) throws BankException;

    public abstract void withdraw(double amount) throws BankException;

    public void transferFunds(Account targetAccount, double amount) throws BankException {
        validatePositiveAmount(amount);
        withdraw(amount);
        targetAccount.deposit(amount);
    }

    public void updatePhone(String newPhone) {
        this.phone = newPhone;
    }

    public void shiftBranch(String newBranch) {
        this.branch = newBranch;
    }

    protected void validatePositiveAmount(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    public abstract double withdrawableBalance();
}
