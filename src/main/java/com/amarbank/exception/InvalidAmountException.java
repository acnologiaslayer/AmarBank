package com.amarbank.exception;

/**
 * Thrown when an amount supplied for a transaction is invalid
 * (zero, negative, or not a number).
 */
public class InvalidAmountException extends BankException {

    public InvalidAmountException(double amount) {
        super(String.format("Invalid amount: %.2f. Amount must be greater than zero.", amount));
    }

    public InvalidAmountException(String message) {
        super(message);
    }
}
