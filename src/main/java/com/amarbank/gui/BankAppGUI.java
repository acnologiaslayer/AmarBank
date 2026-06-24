package com.amarbank.gui;

import com.amarbank.exception.BankException;
import com.amarbank.exception.DataStoreException;
import com.amarbank.Account;
import com.amarbank.BankManagement;
import com.amarbank.BankOperations;
import com.amarbank.LoanAccount;
import com.amarbank.SavingsAccount;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * Java Swing front-end for the banking application.
 *
 * Built in the same style as the Playground DivisionEngineGUI lesson:
 *  - extends JFrame,
 *  - builds the layout in the constructor,
 *  - wires buttons with anonymous ActionListeners,
 *  - keeps all business logic in the service layer (BankOperations).
 *
 * Each action opens its own modal pop-up form (FormDialog). A Theme menu
 * lets the user switch the Swing Look&Feel at runtime.
 *
 * Theme switching is fault-tolerant: some Look&Feels (notably the native
 * GTK "System" L&F) can throw asynchronously during painting on certain
 * JVMs. A {@link SafeEventQueue} catches those uncaught event-thread
 * errors so the application reverts to the previous working theme and
 * shows a clear message instead of crashing.
 */
public class BankAppGUI extends JFrame {

    private static final String[] ACCOUNT_TYPES = {"SAVINGS", "LOAN"};

    /** Pure-Java theme used at startup and as the recovery fallback. */
    private static final SwingTheme DEFAULT_THEME = SwingTheme.NIMBUS;

    private final BankOperations bank;

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel titleLabel;

    // custom themed chrome
    private TitleBar titleBar;
    private JPanel rootPanel;
    private JPanel contentPanel;
    private JPanel toolbarPanel;
    private JPanel statusPanel;
    private JScrollPane tableScroll;
    private ThemeSelectorButton themeSelector;
    private final java.util.List<JButton> actionButtons = new java.util.ArrayList<>();

    // theme state used for error recovery
    private SwingTheme currentTheme = DEFAULT_THEME;
    private SwingTheme lastGoodTheme = DEFAULT_THEME;
    private boolean recoveringTheme = false;
    private Timer promoteTimer;

    public BankAppGUI(BankOperations bank) {
        this.bank = bank;

        installDefaultTheme();

        setTitle("Amar Bank - Banking Application");
        setSize(820, 560);
        setMinimumSize(new Dimension(640, 440));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Undecorated so we can paint the whole window (title bar + border)
        // in the active theme instead of the native OS chrome.
        setUndecorated(true);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createLineBorder(currentTheme.palette().accent(), 1));
        setContentPane(rootPanel);

        titleBar = new TitleBar(this, "AMAR BANK");
        rootPanel.add(titleBar, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.add(buildHeader(), BorderLayout.NORTH);
        contentPanel.add(buildToolbar(), BorderLayout.WEST);
        contentPanel.add(buildTablePanel(), BorderLayout.CENTER);
        contentPanel.add(buildStatusBar(), BorderLayout.SOUTH);
        rootPanel.add(contentPanel, BorderLayout.CENTER);

        new WindowResizer(this); // edge/corner resize on the undecorated frame

        refreshTable();
        applyChrome(currentTheme); // colour all custom components for the theme

        // Catch any uncaught event-thread error (e.g. a Look&Feel that fails
        // to paint) and route it through our recovery/reporting handler.
        SafeEventQueue.install(this::handleEventThreadError);
    }

    // ---------- layout builders ----------

    private JComponent buildHeader() {
        titleLabel = new JLabel("AMAR BANK", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        return titleLabel;
    }

    private JComponent buildToolbar() {
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        toolbarPanel.add(actionButton("Open Account...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openAccount();
            }
        }));
        toolbarPanel.add(actionButton("Deposit...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deposit();
            }
        }));
        toolbarPanel.add(actionButton("Withdraw...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                withdraw();
            }
        }));
        toolbarPanel.add(actionButton("Transfer...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transfer();
            }
        }));
        toolbarPanel.add(actionButton("Check Balance...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkBalance();
            }
        }));
        toolbarPanel.add(actionButton("Update Contact...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateContact();
            }
        }));
        toolbarPanel.add(actionButton("Close Account...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeAccount();
            }
        }));
        toolbarPanel.add(Box.createVerticalStrut(10));
        toolbarPanel.add(actionButton("Refresh List", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTable();
                setStatus("Account list refreshed.");
            }
        }));

        return toolbarPanel;
    }

    private JComponent buildTablePanel() {
        String[] columns = {"Account No", "Type", "Holder Name", "Balance", "Special Attribute"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only display
            }
        };
        accountTable = new JTable(tableModel);
        accountTable.setRowHeight(24);
        accountTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        tableScroll = new JScrollPane(accountTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Accounts"));
        return tableScroll;
    }

    private JComponent buildStatusBar() {
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 8));

        statusLabel = new JLabel("Ready.");
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // The theme selector lives in the bottom-right, as a palette icon.
        themeSelector = new ThemeSelectorButton(currentTheme, this::applyTheme);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(themeSelector);
        statusPanel.add(right, BorderLayout.EAST);

        return statusPanel;
    }

    private JButton actionButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.addActionListener(listener);
        actionButtons.add(button);
        return button;
    }

    // ---------- theme handling ----------

    /** Applies the default theme at startup, with a hard fallback chain. */
    private void installDefaultTheme() {
        try {
            DEFAULT_THEME.apply();
            currentTheme = DEFAULT_THEME;
            lastGoodTheme = DEFAULT_THEME;
        } catch (Exception e) {
            // Fall back to the cross-platform L&F, which is always available.
            try {
                SwingTheme.METAL.apply();
                currentTheme = SwingTheme.METAL;
                lastGoodTheme = SwingTheme.METAL;
            } catch (Exception ignored) {
                // Keep whatever L&F the JVM started with.
            }
        }
    }

    /**
     * Switches to the requested theme. If applying it fails immediately we
     * revert here; if it fails later during painting, {@link #handleEventThreadError}
     * performs the recovery. Either way the user sees a clear message.
     */
    private void applyTheme(SwingTheme theme) {
        if (theme == currentTheme) {
            return;
        }
        SwingTheme previousGood = lastGoodTheme;
        try {
            theme.apply();
            currentTheme = theme;
            // System-wide: refresh every open window (this frame plus any open
            // dialogs/pop-ups) so the whole application adopts the new theme.
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            applyChrome(theme);
            // Only trust the new theme once it has survived a short settling
            // period of real repaints. A broken Look&Feel throws during those
            // repaints first, so lastGoodTheme is not advanced prematurely.
            scheduleThemePromotion(theme);
            setStatus("Theme changed to " + theme.getLabel() + ".");
        } catch (Throwable e) {
            revertTheme(previousGood, theme, describeThrowable(e));
        }
    }

    /**
     * Recolours every custom-painted component (title bar, window border,
     * side panel, status bar, table, header, theme icon) to match the theme
     * palette, so the whole window - not just the Swing widgets - is themed.
     */
    private void applyChrome(SwingTheme theme) {
        SwingTheme.Palette p = theme.palette();

        rootPanel.setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        rootPanel.setBackground(p.background());
        contentPanel.setBackground(p.background());
        titleBar.applyPalette(p);
        titleBar.applyFont(theme.chromeFont(Font.BOLD, 14f));

        toolbarPanel.setBackground(p.background());
        statusPanel.setBackground(p.surface());
        statusLabel.setForeground(p.foreground());
        statusLabel.setFont(theme.chromeFont(Font.PLAIN, 12f));

        titleLabel.setForeground(p.accent());
        titleLabel.setFont(theme.headingFont(Font.BOLD, 26f));

        accountTable.setBackground(p.background());
        accountTable.setForeground(p.foreground());
        accountTable.setGridColor(p.surface());
        accountTable.getTableHeader().setBackground(p.surface());
        accountTable.getTableHeader().setForeground(p.accent());
        accountTable.getTableHeader().setFont(theme.chromeFont(Font.BOLD, 12f));
        accountTable.setSelectionBackground(p.accent());
        accountTable.setSelectionForeground(p.accentText());
        tableScroll.getViewport().setBackground(p.background());
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(p.accent()), "Accounts", 0, 0,
                theme.chromeFont(Font.BOLD, 12f), p.accent()));

        themeSelector.applyPalette(p);
        themeSelector.setSelected(theme);

        repaint();
    }

    /** After a brief delay with no paint failures, accept the theme as safe. */
    private void scheduleThemePromotion(SwingTheme theme) {
        if (promoteTimer != null) {
            promoteTimer.stop();
        }
        promoteTimer = new Timer(400, e -> markThemeAsGood(theme));
        promoteTimer.setRepeats(false);
        promoteTimer.start();
    }

    /** Called after a successful repaint to remember a theme as safe. */
    private void markThemeAsGood(SwingTheme theme) {
        if (currentTheme == theme && !recoveringTheme) {
            lastGoodTheme = theme;
        }
    }

    /**
     * Handles any exception that escaped to the event thread. A failure that
     * comes from a Look&Feel (painting) triggers a revert to the last working
     * theme; anything else is reported as an unexpected error.
     */
    private void handleEventThreadError(Throwable error) {
        if (recoveringTheme) {
            return; // already recovering; swallow follow-up paint errors
        }
        if (isLookAndFeelError(error) && currentTheme != lastGoodTheme) {
            recoveringTheme = true;
            if (promoteTimer != null) {
                promoteTimer.stop(); // never promote the broken theme
            }
            SwingTheme broken = currentTheme;
            try {
                revertTheme(lastGoodTheme, broken, describeThrowable(error));
            } finally {
                recoveringTheme = false;
            }
        } else if (!isLookAndFeelError(error)) {
            showError("An unexpected error occurred:\n" + describeThrowable(error));
        }
        // A look&feel error while we are already on the last-good theme is
        // a transient repaint glitch from the failed switch; safe to ignore.
    }

    /** Restores a known-good theme and tells the user what went wrong. */
    private void revertTheme(SwingTheme target, SwingTheme broken, String reason) {
        SwingTheme restored = target;
        try {
            target.apply();
            currentTheme = target;
        } catch (Throwable e) {
            // Last resort: the cross-platform L&F never fails to load.
            try {
                SwingTheme.METAL.apply();
                restored = SwingTheme.METAL;
                currentTheme = SwingTheme.METAL;
            } catch (Throwable ignored) {
                restored = currentTheme;
            }
        }
        lastGoodTheme = currentTheme;
        try {
            SwingUtilities.updateComponentTreeUI(this);
            applyChrome(currentTheme);
        } catch (Throwable ignored) {
            // updating the tree should be safe now; ignore if not
        }
        statusLabel.setForeground(currentTheme.palette().danger());
        statusLabel.setText("Theme '" + broken.getLabel() + "' is not supported here; reverted to "
                + restored.getLabel() + ".");
        // Show the dialog after we have fully unwound from the failing paint
        // dispatch, so we never start a nested modal loop inside the handler.
        final String restoredLabel = restored.getLabel();
        SwingUtilities.invokeLater(() -> MessageDialog.show(this, "Theme Not Available",
                "The '" + broken.getLabel() + "' theme is not supported on this system "
                        + "and could not be displayed.\n\nReverted to the '" + restoredLabel
                        + "' theme.\n\nDetails: " + reason,
                MessageDialog.Kind.WARNING));
    }

    private boolean isLookAndFeelError(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                String cls = frame.getClassName();
                if (cls.contains("javax.swing.plaf") || cls.contains("com.sun.java.swing.plaf")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- actions (each opens a pop-up form) ----------

    private void openAccount() {
        FormDialog form = new FormDialog(this, "Open Account");
        JComboBox<String> type = form.addComboBox("Account type:", ACCOUNT_TYPES);
        JTextField name = form.addTextField("Customer name:");
        JTextField branch = form.addTextField("Branch:");
        JTextField phone = form.addTextField("Phone:");
        JTextField opening = form.addTextField("Opening balance:");
        if (!form.showDialog()) {
            return;
        }
        try {
            String customer = name.getText().trim();
            if (customer.isEmpty()) {
                throw new BankException("Customer name cannot be empty.");
            }
            String branchName = branch.getText().trim();
            if (branchName.isEmpty()) {
                throw new BankException("Branch cannot be empty.");
            }
            String phoneNum = phone.getText().trim();
            if (phoneNum.isEmpty()) {
                throw new BankException("Phone number cannot be empty.");
            }
            double balance = parseAmount(opening.getText());
            Account account = bank.openAccount((String) type.getSelectedItem(), customer, branchName, phoneNum, balance);
            refreshTable();
            setStatus("Account created: " + account.getAccountNumber());
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void deposit() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Deposit");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        JTextField amount = form.addTextField("Amount to deposit:");
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
            double value = parseAmount(amount.getText());
            bank.deposit(number, value);
            refreshTable();
            setStatus(String.format("Deposited %.2f. New balance: %.2f",
                    value, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void withdraw() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Withdraw");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        JTextField amount = form.addTextField("Amount to withdraw:");
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
            double value = parseAmount(amount.getText());
            bank.withdraw(number, value);
            refreshTable();
            setStatus(String.format("Withdrew %.2f. New balance: %.2f",
                    value, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void transfer() {
        if (bank.listAccounts().size() < 2) {
            showError("You need at least two accounts to make a transfer.");
            return;
        }
        FormDialog form = new FormDialog(this, "Transfer");
        JComboBox<String> from = form.addComboBox("From account:", accountChoices());
        JComboBox<String> to = form.addComboBox("To account:", accountChoices());
        JTextField amount = form.addTextField("Amount to transfer:");
        preselectAccount(from);
        if (!form.showDialog()) {
            return;
        }
        try {
            String fromNumber = accountNumberOf(from);
            String toNumber = accountNumberOf(to);
            double value = parseAmount(amount.getText());
            bank.transfer(fromNumber, toNumber, value);
            refreshTable();
            setStatus(String.format("Transferred %.2f from %s to %s.", value, fromNumber, toNumber));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void checkBalance() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Check Balance");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
            Account acc = bank.requireAccount(number);
            String message = String.format(
                    "Account: %s%nType: %s%nCustomer: %s%nBalance: %.2f%nWithdrawable now: %.2f",
                    acc.getAccountNumber(), acc.getType(), acc.getAccountHolderName(),
                    acc.getBalance(), acc.withdrawableBalance());
            MessageDialog.show(this, "Balance", message, MessageDialog.Kind.INFO);
            setStatus(String.format("%s balance: %.2f", acc.getAccountNumber(), acc.getBalance()));
        } catch (BankException e) {
            showError(e.getMessage());
        }
    }

    private void updateContact() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Update Contact / Branch");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        JTextField branch = form.addTextField("New branch:");
        JTextField phone = form.addTextField("New phone:");
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
            bank.updateAccountDetails(number, branch.getText(), phone.getText());
            refreshTable();
            setStatus("Updated details for " + number + ".");
        } catch (BankException e) {
            showError(e.getMessage());
        }
    }

    private void closeAccount() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Close Account");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        String number = accountNumberOf(account);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Close account " + number + "? This is only allowed for fully settled zero-balance accounts.",
                "Confirm Close Account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            bank.deleteAccount(number);
            refreshTable();
            setStatus("Closed account " + number + ".");
        } catch (BankException e) {
            showError(e.getMessage());
        }
    }

    // ---------- shared helpers ----------

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Account> accounts = bank.listAccounts();
        for (Account account : accounts) {
            tableModel.addRow(new Object[]{
                    account.getAccountNumber(),
                    account.getType(),
                    account.getAccountHolderName(),
                    String.format("%.2f", account.getBalance()),
                    describeSpecialAttribute(account)
            });
        }
    }

    private String describeSpecialAttribute(Account account) {
        if (account instanceof SavingsAccount) {
            return "Interest 1.00%";
        } else if (account instanceof LoanAccount loan) {
            return String.format("Due: %.2f / Limit: %.2f", loan.getAmountDue(), loan.getLoanLimit());
        }
        return "-";
    }

    /** Builds the labelled choices ("AC00001 - Alice (SAVINGS)") for an account dropdown. */
    private String[] accountChoices() {
        List<Account> accounts = bank.listAccounts();
        String[] choices = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            choices[i] = String.format("%s - %s (%s)",
                    a.getAccountNumber(), a.getAccountHolderName(), a.getType());
        }
        return choices;
    }

    /** Extracts the account number from a "AC00001 - Name (TYPE)" dropdown item. */
    private String accountNumberOf(JComboBox<String> combo) {
        Object selected = combo.getSelectedItem();
        String text = selected == null ? "" : selected.toString();
        int dash = text.indexOf(" - ");
        return (dash >= 0 ? text.substring(0, dash) : text).trim();
    }

    /** Selects the table-highlighted account in the dropdown, if one is highlighted. */
    private void preselectAccount(JComboBox<String> combo) {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String number = String.valueOf(tableModel.getValueAt(row, 0));
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(number + " ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Warns and returns true when there are no accounts to act on yet. */
    private boolean ensureAccountsExist() {
        if (bank.listAccounts().isEmpty()) {
            showError("There are no accounts yet. Please open an account first.");
            return true;
        }
        return false;
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Please enter a valid amount.");
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("'" + text.trim() + "' is not a valid number.");
        }
    }

    private String describeThrowable(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            message = t.getClass().getSimpleName();
        }
        return message;
    }

    private void setStatus(String message) {
        statusLabel.setForeground(currentTheme.palette().foreground());
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(currentTheme.palette().danger());
        statusLabel.setText("Error: " + message.replace('\n', ' '));
        MessageDialog.show(this, "Error", message, MessageDialog.Kind.ERROR);
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                BankManagement bankManagement = new BankManagement(Path.of("data"));
                BankOperations bank = new BankOperations(bankManagement);
                new BankAppGUI(bank).setVisible(true);
            } catch (DataStoreException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not start the bank: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                JOptionPane.showMessageDialog(null,
                        "The application failed to start:\n" + t,
                        "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
