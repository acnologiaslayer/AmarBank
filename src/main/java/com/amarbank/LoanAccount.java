package com.amarbank;

import com.amarbank.exception.BankException;

/**
 * A loan account.
 *
 * <p>Attributes (per spec):</p>
 * <ul>
 *   <li>{@code loanLimit} - the maximum money that may be borrowed.</li>
 *   <li>{@code amountDue} - tracks how much the user currently owes.</li>
 * </ul>
 *
 * <p>Operations (per spec):</p>
 * <ul>
 *   <li>{@code deposit} acts as a <b>loan repayment</b>: it reduces
 *       {@code amountDue} and updates the balance.</li>
 *   <li>{@code withdraw} acts as <b>borrowing / disbursing funds</b>: it
 *       increases {@code amountDue}. A withdrawal is only allowed if
 *       {@code amountDue + amount <= loanLimit}.</li>
 * </ul>
 */
public class LoanAccount extends Account {

    public static final String TYPE = "LOAN";

    private double loanLimit;
    private double amountDue;

    public LoanAccount(String accountNumber, String accountHolderName, String branch,
                       String phone, double balance, double loanLimit, double amountDue) {
        super(accountNumber, accountHolderName, branch, phone, balance);
        this.loanLimit = loanLimit;
        this.amountDue = amountDue;
    }

    public double getLoanLimit() {
        return loanLimit;
    }

    public void setLoanLimit(double loanLimit) {
        this.loanLimit = loanLimit;
    }

    public double getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(double amountDue) {
        this.amountDue = amountDue;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /** Remaining borrowing headroom under the loan limit. */
    @Override
    public double withdrawableBalance() {
        return loanLimit - amountDue;
    }

    /**
     * Loan repayment: reduces the amount owed and updates the balance.
     * A repayment cannot exceed the outstanding amount due.
     */
    @Override
    public void deposit(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amountDue <= 0) {
            throw new BankException("This loan is already fully repaid; nothing is due.");
        }
        if (amount > amountDue) {
            throw new BankException("Repayment exceeds the outstanding amount due. Amount due: "
                    + String.format("%.2f", amountDue));
        }
        amountDue -= amount;
        setBalance(getBalance() - amount);
    }

    /**
     * Borrowing / disbursing funds: increases the amount owed and disburses
     * the cash into the balance. Only allowed while the new total owed stays
     * within the loan limit.
     */
    @Override
    public void withdraw(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amountDue + amount > loanLimit) {
            throw new BankException("Withdrawal exceeds the loan limit. Borrowable amount remaining: "
                    + String.format("%.2f", withdrawableBalance()));
        }
        amountDue += amount;
        setBalance(getBalance() + amount);
    }
}
