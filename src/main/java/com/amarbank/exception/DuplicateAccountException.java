package com.amarbank.exception;

/**
 * Thrown when trying to create an account with a number
 * that is already registered in the bank.
 */
public class DuplicateAccountException extends BankException {

    public DuplicateAccountException(String accountNumber) {
        super("Account already exists: " + accountNumber);
    }
}
