package com.amarbank.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.amarbank.Account;
import com.amarbank.LoanAccount;
import com.amarbank.exception.DataStoreException;

/**
 * Optional improvement: a SQLite-backed permanent store.
 *
 * <p>This is interchangeable with {@link CsvAccountStore} through the
 * {@link AccountStore} interface. It requires the {@code org.xerial:sqlite-jdbc}
 * driver on the classpath; if the driver is missing the application simply
 * falls back to the CSV store (see {@link com.amarbank.Main}).</p>
 */
public class SqliteAccountStore implements AccountStore {

    private static final String TABLE = "accounts";

    private final String url;
    private final Path databaseFile;

    public SqliteAccountStore(Path databaseFile) throws DataStoreException {
        this.databaseFile = databaseFile;
        this.url = "jdbc:sqlite:" + databaseFile;
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not create data directory for " + databaseFile, e);
        }
        createTable();
    }

    /** True if the SQLite JDBC driver is available on the classpath. */
    public static boolean isAvailable() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void createTable() throws DataStoreException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "account_number TEXT PRIMARY KEY,"
                + "type TEXT NOT NULL,"
                + "account_holder_name TEXT NOT NULL,"
                + "branch TEXT NOT NULL,"
                + "phone TEXT NOT NULL,"
                + "balance REAL NOT NULL,"
                + "loan_limit REAL,"
                + "amount_due REAL)";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DataStoreException("Could not initialise the SQLite database", e);
        }
    }

    @Override
    public List<Account> loadAll() throws DataStoreException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT account_number, type, account_holder_name, branch, phone, "
                + "balance, loan_limit, amount_due FROM " + TABLE + " ORDER BY account_number";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                accounts.add(AccountCodec.fromRow(new String[]{
                        rs.getString("account_number"),
                        rs.getString("type"),
                        rs.getString("account_holder_name"),
                        rs.getString("branch"),
                        rs.getString("phone"),
                        Double.toString(rs.getDouble("balance")),
                        nullableString(rs, "loan_limit"),
                        nullableString(rs, "amount_due")
                }));
            }
        } catch (SQLException e) {
            throw new DataStoreException("Could not load accounts from the SQLite database", e);
        }
        return accounts;
    }

    @Override
    public void saveAll(List<Account> accounts) throws DataStoreException {
        // Mirror the in-memory roster exactly: clear then re-insert in one
        // transaction so a failure rolls back rather than partially writing.
        String delete = "DELETE FROM " + TABLE;
        String insert = "INSERT INTO " + TABLE + "(account_number, type, account_holder_name, "
                + "branch, phone, balance, loan_limit, amount_due) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            try (Statement clear = conn.createStatement()) {
                clear.executeUpdate(delete);
            }
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                for (Account account : accounts) {
                    ps.setString(1, account.getAccountNumber());
                    ps.setString(2, account.getType());
                    ps.setString(3, account.getAccountHolderName());
                    ps.setString(4, account.getBranch());
                    ps.setString(5, account.getPhone());
                    ps.setDouble(6, account.getBalance());
                    if (account instanceof LoanAccount loan) {
                        ps.setDouble(7, loan.getLoanLimit());
                        ps.setDouble(8, loan.getAmountDue());
                    } else {
                        ps.setNull(7, java.sql.Types.REAL);
                        ps.setNull(8, java.sql.Types.REAL);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new DataStoreException("Could not save accounts to the SQLite database", e);
        }
    }

    @Override
    public String describe() {
        return "SQLite database: " + databaseFile.toAbsolutePath();
    }

    private static String nullableString(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? "" : Double.toString(value);
    }
}
