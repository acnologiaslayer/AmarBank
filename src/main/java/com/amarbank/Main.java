package com.amarbank;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.amarbank.exception.DataStoreException;
import com.amarbank.gui.BankAppGUI;
import com.amarbank.storage.AccountStore;
import com.amarbank.storage.CsvAccountStore;
import com.amarbank.storage.SqliteAccountStore;

/**
 * Application entry point.
 *
 * <p>Wires up the permanent store and launches the Swing GUI on the event
 * dispatch thread. The assignment requires a CSV file, which is the default.
 * As an optional improvement, passing {@code --sqlite} on the command line (or
 * setting {@code -Damarbank.store=sqlite}) switches to the SQLite backend when
 * its driver is available.</p>
 */
public class Main {

    public static void main(String[] args) {
        Path dataDir = Path.of("data");
        boolean wantSqlite = wantsSqlite(args);

        try {
            AccountStore store = createStore(dataDir, wantSqlite);
            BankManagement bank = new BankManagement(store);
            SwingUtilities.invokeLater(() -> new BankAppGUI(bank).setVisible(true));
        } catch (DataStoreException e) {
            reportStartupFailure("Could not start Amar Bank: " + e.getMessage());
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            reportStartupFailure("The application failed to start:\n" + t);
        }
    }

    /** Builds the chosen store, falling back to CSV if SQLite is unavailable. */
    private static AccountStore createStore(Path dataDir, boolean wantSqlite) throws DataStoreException {
        if (wantSqlite && SqliteAccountStore.isAvailable()) {
            return new SqliteAccountStore(dataDir.resolve("bank.db"));
        }
        // Default, spec-required storage: a CSV file.
        return new CsvAccountStore(dataDir.resolve("accounts.csv"));
    }

    private static boolean wantsSqlite(String[] args) {
        if ("sqlite".equalsIgnoreCase(System.getProperty("amarbank.store", ""))) {
            return true;
        }
        for (String arg : args) {
            if ("--sqlite".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void reportStartupFailure(String message) {
        System.err.println(message);
        try {
            JOptionPane.showMessageDialog(null, message, "Amar Bank - Startup Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Throwable headless) {
            // No display available; the stderr message above is enough.
        }
    }
}
