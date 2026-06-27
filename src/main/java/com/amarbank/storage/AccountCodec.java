package com.amarbank.storage;

import com.amarbank.Account;
import com.amarbank.LoanAccount;
import com.amarbank.SavingsAccount;
import com.amarbank.exception.DataStoreException;

/**
 * Shared (de)serialisation logic that turns an {@link Account} into a flat row
 * of values and back again. Both the CSV and SQLite stores reuse this so the
 * two backends always agree on the persisted shape of an account.
 *
 * <p>Column order: {@code accountNumber, type, accountHolderName, branch,
 * phone, balance, loanLimit, amountDue}. The two loan-only columns are blank
 * for savings accounts.</p>
 */
public final class AccountCodec {

    public static final String[] COLUMNS = {
            "accountNumber", "type", "accountHolderName", "branch",
            "phone", "balance", "loanLimit", "amountDue"
    };

    private AccountCodec() {
    }

    /** Returns the account as an 8-element string row (loan fields blank for savings). */
    public static String[] toRow(Account account) {
        String loanLimit = "";
        String amountDue = "";
        if (account instanceof LoanAccount loan) {
            loanLimit = Double.toString(loan.getLoanLimit());
            amountDue = Double.toString(loan.getAmountDue());
        }
        return new String[]{
                account.getAccountNumber(),
                account.getType(),
                account.getAccountHolderName(),
                account.getBranch(),
                account.getPhone(),
                Double.toString(account.getBalance()),
                loanLimit,
                amountDue
        };
    }

    /** Rebuilds the correct {@link Account} subclass from a row of values. */
    public static Account fromRow(String[] row) throws DataStoreException {
        if (row.length < 6) {
            throw new DataStoreException(
                    "Malformed account record (expected at least 6 fields): "
                            + String.join(",", row), null);
        }
        String accountNumber = row[0];
        String type = row[1];
        String name = row[2];
        String branch = row[3];
        String phone = row[4];
        double balance = parseDouble(row[5], "balance", accountNumber);

        if (SavingsAccount.TYPE.equalsIgnoreCase(type)) {
            return new SavingsAccount(accountNumber, name, branch, phone, balance);
        } else if (LoanAccount.TYPE.equalsIgnoreCase(type)) {
            double loanLimit = row.length > 6 ? parseDouble(row[6], "loanLimit", accountNumber) : 0.0;
            double amountDue = row.length > 7 ? parseDouble(row[7], "amountDue", accountNumber) : 0.0;
            return new LoanAccount(accountNumber, name, branch, phone, balance, loanLimit, amountDue);
        } else {
            throw new DataStoreException(
                    "Unknown account type '" + type + "' for account " + accountNumber, null);
        }
    }

    private static double parseDouble(String value, String field, String accountNumber)
            throws DataStoreException {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new DataStoreException(
                    "Invalid " + field + " '" + value + "' for account " + accountNumber, e);
        }
    }
}
