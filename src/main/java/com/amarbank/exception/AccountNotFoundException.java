package com.amarbank.exception;

/**
 * Thrown when an operation references an account number
 * that does not exist in the bank.
 */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
