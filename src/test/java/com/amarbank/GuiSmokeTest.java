package com.amarbank;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.amarbank.gui.BankAppGUI;
import com.amarbank.gui.SwingTheme;
import com.amarbank.storage.CsvAccountStore;

/**
 * GUI smoke test: builds the full {@link BankAppGUI} (all three tabs, the
 * themed chrome and the account table) and cycles every theme, without ever
 * calling {@code setVisible(true)} so no window is shown. It proves the Swing
 * layout and theme code construct and render-prepare without throwing.
 *
 * <p>Requires a graphics environment (a display) for AWT font metrics, so it is
 * separate from the headless {@link SelfTest}.</p>
 */
public class GuiSmokeTest {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("amarbank-gui");
        BankManagement bank = new BankManagement(new CsvAccountStore(dir.resolve("accounts.csv")));
        bank.openAccount("SAVINGS", "ALICE RAHMAN", "Dhaka Main", "01712345678", 500);
        bank.openAccount("LOAN", "BOB KHAN", "Chittagong", "01812345678", 10000);

        final Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                BankAppGUI gui = new BankAppGUI(bank);
                gui.pack();               // forces a full layout pass
                // Exercise every theme's apply/chrome path.
                Method applyTheme = BankAppGUI.class.getDeclaredMethod("applyTheme", SwingTheme.class);
                applyTheme.setAccessible(true);
                for (SwingTheme theme : SwingTheme.values()) {
                    applyTheme.invoke(gui, theme);
                }
                gui.dispose();            // never shown
                System.out.println("  [PASS] GUI constructed, laid out and themed for all "
                        + SwingTheme.values().length + " themes.");
            } catch (Throwable t) {
                failure[0] = t;
            }
        });

        if (failure[0] != null) {
            System.out.println("  [FAIL] GUI smoke test threw:");
            failure[0].printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("\n==== GUI smoke test passed ====");
        // Ensure the AWT thread does not keep the JVM alive.
        for (java.awt.Window w : JFrame.getWindows()) {
            w.dispose();
        }
        System.exit(0);
    }
}
