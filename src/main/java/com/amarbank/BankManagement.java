package com.amarbank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.amarbank.exception.DataStoreException;

public class BankManagement {
    // controller class to manage accounts and operations. contains arraylist<account> to store accounts and methods to perform operations on accounts and handles logic for reading and writing to db (sqlite)
   private static final String TABLE_NAME = "accounts";

    private final String url;

    public BankManagement(Path dataDirectory) throws DataStoreException {
        Path databaseFile = dataDirectory.resolve("bank.db");
        this.url = "jdbc:sqlite:" + databaseFile;
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new DataStoreException("Could not create data directory " + dataDirectory, e);
        }
        createTable();
    }   

     private void createTable() throws DataStoreException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + " account_number TEXT PRIMARY KEY,\n"
                + " type           TEXT NOT NULL,\n"
                + " account_holder_name  TEXT NOT NULL,\n"
                + " branch        TEXT NOT NULL,\n"
                + " phone        TEXT NOT NULL,\n"
                + " balance        REAL NOT NULL,\n"
                + "loan_limit        REAL,\n"
                + "amount_due        REAL\n"
                + ");";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DataStoreException("Could not initialise the database", e);
        }
    }

    public void addAccount(Account account) throws DataStoreException {
        String sql = "INSERT INTO " + TABLE_NAME + "(account_number, type, account_holder_name, branch, phone, balance, loan_limit, amount_due) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getType());
            pstmt.setString(3, account.getAccountHolderName());
            pstmt.setString(4, account.getBranch());
            pstmt.setString(5, account.getPhone());
            pstmt.setDouble(6, account.getBalance());
            if (account instanceof LoanAccount loanAccount) {
                pstmt.setDouble(7, loanAccount.getLoanLimit());
                pstmt.setDouble(8, loanAccount.getAmountDue());
            } else {
                pstmt.setNull(7, java.sql.Types.DOUBLE);
                pstmt.setNull(8, java.sql.Types.DOUBLE);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Could not add account to the database", e);
        }
    }

    public Account getAccount(String accountNumber) throws DataStoreException {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE account_number = ?";
        try (Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return toAccount(
                            rs.getString("account_number"),
                            rs.getString("type"),
                            rs.getString("account_holder_name"),
                            rs.getString("branch"),
                            rs.getString("phone"),
                            rs.getDouble("balance"),
                            nullableDouble(rs, "loan_limit"),
                            nullableDouble(rs, "amount_due")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataStoreException("Could not retrieve account from the database", e);
        }
        return null; // or throw an exception if account not found
    }

    public void updateAccount(Account account) throws DataStoreException {
        String sql = "UPDATE " + TABLE_NAME + " SET type = ?, account_holder_name = ?, branch = ?, phone = ?, balance = ?, loan_limit = ?, amount_due = ? WHERE account_number = ?";
        try (Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getType());
            pstmt.setString(2, account.getAccountHolderName());
            pstmt.setString(3, account.getBranch());
            pstmt.setString(4, account.getPhone());
            pstmt.setDouble(5, account.getBalance());
            if (account instanceof LoanAccount loanAccount) {
                pstmt.setDouble(6, loanAccount.getLoanLimit());
                pstmt.setDouble(7, loanAccount.getAmountDue());
            } else {
                pstmt.setNull(6, java.sql.Types.DOUBLE);
                pstmt.setNull(7, java.sql.Types.DOUBLE);
            }
            pstmt.setString(8, account.getAccountNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Could not update account in the database", e);
        }
    }

    public ArrayList<Account> loadAccounts() throws DataStoreException {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY account_number";
        ArrayList<Account> accounts = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                accounts.add(toAccount(
                        rs.getString("account_number"),
                        rs.getString("type"),
                        rs.getString("account_holder_name"),
                        rs.getString("branch"),
                        rs.getString("phone"),
                        rs.getDouble("balance"),
                        nullableDouble(rs, "loan_limit"),
                        nullableDouble(rs, "amount_due")
                ));
            }
        } catch (SQLException e) {
            throw new DataStoreException("Could not load accounts from the database", e);
        }
        return accounts;
    }

    public void deleteAccount(String accountNumber) throws DataStoreException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE account_number = ?";
        try (Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Could not delete account from the database", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private Account toAccount(String accountNumber, String type, String name, String branch, String phone, double balance, Double loanLimit, Double amountDue)
            throws DataStoreException {
        if ("SAVINGS".equalsIgnoreCase(type)) {
            return new SavingsAccount(accountNumber, type, name, branch, phone, balance, 0.01);
        } else if ("LOAN".equalsIgnoreCase(type)) {
            double restoredAmountDue = amountDue == null ? Math.max(0.0, balance) : amountDue;
            double restoredLimit = loanLimit == null ? Math.max(10000.0, restoredAmountDue) : loanLimit;
            return new LoanAccount(accountNumber, type, name, branch, phone, balance, restoredLimit, restoredAmountDue);
        } else {
            throw new DataStoreException("Unknown account type: " + type, null);
        }
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
