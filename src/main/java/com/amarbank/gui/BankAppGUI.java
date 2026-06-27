package com.amarbank.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.amarbank.Account;
import com.amarbank.BankManagement;
import com.amarbank.LoanAccount;
import com.amarbank.SavingsAccount;
import com.amarbank.Validators;
import com.amarbank.exception.BankException;

/**
 * Java Swing front-end for Amar Bank.
 *
 * <p>The interface is a {@link JTabbedPane} with the three panels required by
 * Section 3 of the assignment:</p>
 * <ol>
 *   <li><b>Account Management</b> - inputs for name, phone, branch and initial
 *       balance / loan limit, a {@link JComboBox} to pick Savings vs Loan, and
 *       buttons to Open Account, Update Phone Number and Shift Branch.</li>
 *   <li><b>Transactions</b> - source / destination account and amount fields
 *       with Deposit, Withdraw and Transfer buttons.</li>
 *   <li><b>Accounts (Display)</b> - a searchable {@link JTable} roster plus a
 *       per-account summary view.</li>
 * </ol>
 *
 * <p>Every field is validated with the {@link Validators} regex rules before a
 * submission is processed; a failure raises a {@link JOptionPane} alert, exactly
 * as the specification requires. The window keeps the project's themed chrome
 * (custom title bar, theme selector and fault-tolerant Look&amp;Feel switching).</p>
 */
public class BankAppGUI extends JFrame {

    private static final String[] ACCOUNT_TYPES = {SavingsAccount.TYPE, LoanAccount.TYPE};
    private static final SwingTheme DEFAULT_THEME = SwingTheme.CYBERPUNK;

    private final BankManagement bank;

    // ----- themed chrome -----
    private JPanel rootPanel;
    private JPanel contentPanel;
    private TitleBar titleBar;
    private JLabel titleLabel;
    private ThemeSelectorButton themeSelector;
    private JTabbedPane tabs;

    // ----- account management tab -----
    private JComboBox<String> cboType;
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextField txtBranch;
    private JTextField txtAmount;
    private JLabel lblAmount;
    private JTextField txtManageAccountNo;

    // ----- transaction tab -----
    private JTextField txtSource;
    private JTextField txtDestination;
    private JTextField txtTxnAmount;

    // ----- display tab -----
    private JTextField txtSearch;
    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JScrollPane tableScroll;
    private JTextArea txtSummary;

    private final List<JLabel> formLabels = new ArrayList<>();

    // ----- theme state (for fault-tolerant switching) -----
    private SwingTheme currentTheme = DEFAULT_THEME;
    private SwingTheme lastGoodTheme = DEFAULT_THEME;
    private boolean recoveringTheme = false;
    private Timer promoteTimer;

    public BankAppGUI(BankManagement bank) {
        this.bank = bank;

        installDefaultTheme();

        setTitle("Amar Bank - Banking Application");
        setSize(900, 620);
        setMinimumSize(new Dimension(760, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true); // we paint our own themed chrome

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createLineBorder(currentTheme.palette().accent(), 1));
        setContentPane(rootPanel);

        titleBar = new TitleBar(this, "AMAR BANK");
        rootPanel.add(titleBar, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        contentPanel.add(buildHeader(), BorderLayout.NORTH);
        contentPanel.add(buildTabs(), BorderLayout.CENTER);
        rootPanel.add(contentPanel, BorderLayout.CENTER);

        new WindowResizer(this);

        refreshTable(bank.listAccounts());
        applyChrome(currentTheme);

        SafeEventQueue.install(this::handleEventThreadError);
    }

    // ========================================================================
    // Layout
    // ========================================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        titleLabel = new JLabel("AMAR BANK", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        header.add(titleLabel, BorderLayout.CENTER);

        themeSelector = new ThemeSelectorButton(currentTheme, this::applyTheme);
        JPanel selectorWrap = new JPanel();
        selectorWrap.setOpaque(false);
        selectorWrap.add(themeSelector);
        header.add(selectorWrap, BorderLayout.EAST);

        Box leftSpacer = Box.createHorizontalBox();
        leftSpacer.setPreferredSize(new Dimension(46, 1));
        header.add(leftSpacer, BorderLayout.WEST);
        return header;
    }

    private JComponent buildTabs() {
        tabs = new JTabbedPane();
        tabs.addTab("Account Management", buildManagementTab());
        tabs.addTab("Transactions", buildTransactionTab());
        tabs.addTab("Accounts", buildDisplayTab());
        return tabs;
    }

    // ---- Tab 1: Account Management ----------------------------------------

    private JComponent buildManagementTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        GridBagConstraints g = baseConstraints();

        cboType = new JComboBox<>(ACCOUNT_TYPES);
        addField(panel, g, 0, "Account type:", cboType);

        txtName = new JTextField(20);
        addField(panel, g, 1, "Holder name (UPPER CASE):", txtName);

        txtPhone = new JTextField(20);
        addField(panel, g, 2, "Phone (e.g. 01712345678):", txtPhone);

        txtBranch = new JTextField(20);
        addField(panel, g, 3, "Branch:", txtBranch);

        txtAmount = new JTextField(20);
        lblAmount = addField(panel, g, 4, "Initial balance:", txtAmount);

        // The amount label tracks the selected account type.
        cboType.addActionListener(e -> lblAmount.setText(
                LoanAccount.TYPE.equals(cboType.getSelectedItem()) ? "Loan limit:" : "Initial balance:"));

        // Open Account spans its own row of actions.
        JButton openButton = new JButton("Open Account");
        openButton.addActionListener(e -> openAccount());
        addButtonRow(panel, g, 5, openButton);

        addSeparatorLabel(panel, g, 6,
                "Modify an existing account (uses the phone / branch fields above):");

        txtManageAccountNo = new JTextField(20);
        addField(panel, g, 7, "Account number (AMB-XXXXX):", txtManageAccountNo);

        JButton updatePhoneButton = new JButton("Update Phone Number");
        updatePhoneButton.addActionListener(e -> updatePhone());
        JButton shiftBranchButton = new JButton("Shift Branch");
        shiftBranchButton.addActionListener(e -> shiftBranch());
        addButtonRow(panel, g, 8, updatePhoneButton, shiftBranchButton);

        // push everything to the top
        g.gridx = 0;
        g.gridy = 9;
        g.weighty = 1;
        g.gridwidth = 2;
        panel.add(Box.createGlue(), g);
        return wrapScroll(panel);
    }

    // ---- Tab 2: Transactions ----------------------------------------------

    private JComponent buildTransactionTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        GridBagConstraints g = baseConstraints();

        txtSource = new JTextField(20);
        addField(panel, g, 0, "Source account (AMB-XXXXX):", txtSource);

        txtDestination = new JTextField(20);
        addField(panel, g, 1, "Destination account (transfer only):", txtDestination);

        txtTxnAmount = new JTextField(20);
        addField(panel, g, 2, "Amount:", txtTxnAmount);

        JButton depositButton = new JButton("Deposit");
        depositButton.addActionListener(e -> deposit());
        JButton withdrawButton = new JButton("Withdraw");
        withdrawButton.addActionListener(e -> withdraw());
        JButton transferButton = new JButton("Transfer");
        transferButton.addActionListener(e -> transfer());
        addButtonRow(panel, g, 3, depositButton, withdrawButton, transferButton);

        addSeparatorLabel(panel, g, 4,
                "Deposit/Withdraw use the source account. Transfer moves funds "
                        + "from source to destination.");

        g.gridx = 0;
        g.gridy = 5;
        g.weighty = 1;
        g.gridwidth = 2;
        panel.add(Box.createGlue(), g);
        return wrapScroll(panel);
    }

    // ---- Tab 3: Display ---------------------------------------------------

    private JComponent buildDisplayTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setOpaque(false);
        JLabel searchLabel = new JLabel("Search: ");
        formLabels.add(searchLabel);
        searchBar.add(searchLabel, BorderLayout.WEST);
        txtSearch = new JTextField();
        txtSearch.addActionListener(e -> doSearch());
        searchBar.add(txtSearch, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel();
        searchButtons.setOpaque(false);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> doSearch());
        JButton clearButton = new JButton("Show All");
        clearButton.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable(bank.listAccounts());
        });
        JButton summaryButton = new JButton("View Summary");
        summaryButton.addActionListener(e -> showSelectedSummary());
        searchButtons.add(searchButton);
        searchButtons.add(clearButton);
        searchButtons.add(summaryButton);
        searchBar.add(searchButtons, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Roster table
        String[] columns = {"Account No", "Type", "Holder Name", "Branch", "Phone", "Balance", "Loan Limit", "Amount Due"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accountTable = new JTable(tableModel);
        accountTable.setRowHeight(28);
        accountTable.setFillsViewportHeight(true);
        accountTable.setAutoCreateRowSorter(true);
        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSummaryForSelection();
            }
        });
        tableScroll = new JScrollPane(accountTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Account Roster"));
        panel.add(tableScroll, BorderLayout.CENTER);

        // Summary area
        txtSummary = new JTextArea(6, 20);
        txtSummary.setEditable(false);
        txtSummary.setLineWrap(true);
        txtSummary.setWrapStyleWord(true);
        txtSummary.setText("Select an account to see its summary, or use the search box above.");
        JScrollPane summaryScroll = new JScrollPane(txtSummary);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Account Summary"));
        summaryScroll.setPreferredSize(new Dimension(10, 130));
        panel.add(summaryScroll, BorderLayout.SOUTH);

        return panel;
    }

    // ========================================================================
    // Form helpers
    // ========================================================================

    private GridBagConstraints baseConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        return g;
    }

    private JLabel addField(JPanel panel, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        JLabel jLabel = new JLabel(label);
        formLabels.add(jLabel);
        panel.add(jLabel, g);

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, g);
        return jLabel;
    }

    private void addButtonRow(JPanel panel, GridBagConstraints g, int row, JButton... buttons) {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            bar.add(buttons[i]);
            if (i < buttons.length - 1) {
                bar.add(Box.createHorizontalStrut(8));
            }
        }
        g.gridx = 1;
        g.gridy = row;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(bar, g);
    }

    private void addSeparatorLabel(JPanel panel, GridBagConstraints g, int row, String text) {
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        JLabel note = new JLabel("<html><i>" + text + "</i></html>");
        note.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
        formLabels.add(note);
        panel.add(note, g);
        g.gridwidth = 1;
    }

    private JScrollPane wrapScroll(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ========================================================================
    // Actions - Account Management
    // ========================================================================

    private void openAccount() {
        String type = (String) cboType.getSelectedItem();
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String branch = txtBranch.getText().trim();

        if (!validHolderName(name) || !validPhone(phone) || !validBranch(branch)) {
            return;
        }
        Double amount = parseAmount(txtAmount.getText(),
                LoanAccount.TYPE.equals(type) ? "loan limit" : "initial balance");
        if (amount == null) {
            return;
        }
        try {
            Account account = bank.openAccount(type, name, branch, phone, amount);
            refreshTable(bank.listAccounts());
            clearManagementFields();
            info("Account Created",
                    "Account opened successfully.\nAccount number: " + account.getAccountNumber());
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private void updatePhone() {
        String accountNo = txtManageAccountNo.getText().trim();
        String phone = txtPhone.getText().trim();
        if (!validAccountNumber(accountNo) || !validPhone(phone)) {
            return;
        }
        try {
            bank.updatePhone(accountNo, phone);
            refreshTable(currentRoster());
            info("Phone Updated", "Phone number updated for " + accountNo + ".");
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private void shiftBranch() {
        String accountNo = txtManageAccountNo.getText().trim();
        String branch = txtBranch.getText().trim();
        if (!validAccountNumber(accountNo) || !validBranch(branch)) {
            return;
        }
        try {
            bank.shiftBranch(accountNo, branch);
            refreshTable(currentRoster());
            info("Branch Updated", "Branch updated for " + accountNo + ".");
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    // ========================================================================
    // Actions - Transactions
    // ========================================================================

    private void deposit() {
        String source = txtSource.getText().trim();
        if (!validAccountNumber(source)) {
            return;
        }
        Double amount = parseAmount(txtTxnAmount.getText(), "amount");
        if (amount == null) {
            return;
        }
        try {
            bank.deposit(source, amount);
            afterTransaction(source, "Deposit complete.");
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private void withdraw() {
        String source = txtSource.getText().trim();
        if (!validAccountNumber(source)) {
            return;
        }
        Double amount = parseAmount(txtTxnAmount.getText(), "amount");
        if (amount == null) {
            return;
        }
        try {
            bank.withdraw(source, amount);
            afterTransaction(source, "Withdrawal complete.");
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private void transfer() {
        String source = txtSource.getText().trim();
        String destination = txtDestination.getText().trim();
        if (!validAccountNumber(source)) {
            return;
        }
        if (!validAccountNumber(destination)) {
            return;
        }
        Double amount = parseAmount(txtTxnAmount.getText(), "amount");
        if (amount == null) {
            return;
        }
        try {
            bank.transfer(source, destination, amount);
            refreshTable(currentRoster());
            info("Transfer Complete", String.format(
                    "Transferred %.2f from %s to %s.", amount, source, destination));
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private void afterTransaction(String accountNo, String message) throws BankException {
        refreshTable(currentRoster());
        Account account = bank.requireAccount(accountNo);
        info("Transaction Complete", message
                + String.format("%nAccount: %s%nNew balance: %.2f",
                accountNo, account.getBalance()));
    }

    // ========================================================================
    // Actions - Display / search
    // ========================================================================

    private void doSearch() {
        List<Account> results = bank.search(txtSearch.getText());
        refreshTable(results);
        if (results.isEmpty()) {
            txtSummary.setText("No accounts match \"" + txtSearch.getText().trim() + "\".");
        }
    }

    private List<Account> currentRoster() {
        // Preserve any active search filter after a mutation.
        return bank.search(txtSearch == null ? "" : txtSearch.getText());
    }

    private void updateSummaryForSelection() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = accountTable.convertRowIndexToModel(row);
        String accountNo = String.valueOf(tableModel.getValueAt(modelRow, 0));
        try {
            txtSummary.setText(summaryOf(bank.requireAccount(accountNo)));
            txtSummary.setCaretPosition(0);
        } catch (BankException ignored) {
            // selection out of sync; ignore
        }
    }

    private void showSelectedSummary() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            error("Select an account in the table first.");
            return;
        }
        int modelRow = accountTable.convertRowIndexToModel(row);
        String accountNo = String.valueOf(tableModel.getValueAt(modelRow, 0));
        try {
            info("Account Summary", summaryOf(bank.requireAccount(accountNo)));
        } catch (BankException e) {
            error(e.getMessage());
        }
    }

    private String summaryOf(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append("Account number : ").append(account.getAccountNumber()).append('\n');
        sb.append("Type           : ").append(account.getType()).append('\n');
        sb.append("Holder name    : ").append(account.getAccountHolderName()).append('\n');
        sb.append("Branch         : ").append(account.getBranch()).append('\n');
        sb.append("Phone          : ").append(account.getPhone()).append('\n');
        sb.append(String.format("Balance        : %.2f%n", account.getBalance()));
        if (account instanceof LoanAccount loan) {
            sb.append(String.format("Loan limit     : %.2f%n", loan.getLoanLimit()));
            sb.append(String.format("Amount due     : %.2f%n", loan.getAmountDue()));
            sb.append(String.format("Borrowable now : %.2f", loan.withdrawableBalance()));
        } else {
            sb.append(String.format("Withdrawable   : %.2f", account.withdrawableBalance()));
        }
        return sb.toString();
    }

    // ========================================================================
    // Validation (regex rules + JOptionPane alerts)
    // ========================================================================

    private boolean validHolderName(String value) {
        if (Validators.isValidHolderName(value)) {
            return true;
        }
        alert(Validators.HOLDER_NAME_RULE);
        return false;
    }

    private boolean validPhone(String value) {
        if (Validators.isValidPhone(value)) {
            return true;
        }
        alert(Validators.PHONE_RULE);
        return false;
    }

    private boolean validBranch(String value) {
        if (Validators.isValidBranchName(value)) {
            return true;
        }
        alert(Validators.BRANCH_NAME_RULE);
        return false;
    }

    private boolean validAccountNumber(String value) {
        if (Validators.isValidAccountNumber(value)) {
            return true;
        }
        alert(Validators.ACCOUNT_NUMBER_RULE);
        return false;
    }

    /** Parses a positive monetary amount, alerting via JOptionPane on failure. */
    private Double parseAmount(String text, String label) {
        if (text == null || text.trim().isEmpty()) {
            alert("Please enter " + label + ".");
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            alert("'" + text.trim() + "' is not a valid " + label + ". Enter a number.");
            return null;
        }
        if (!Double.isFinite(value) || value < 0) {
            alert("The " + label + " must be a non-negative number.");
            return null;
        }
        return value;
    }

    // ========================================================================
    // Table + small helpers
    // ========================================================================

    private void refreshTable(List<Account> accounts) {
        tableModel.setRowCount(0);
        for (Account account : accounts) {
            String loanLimit = "-";
            String amountDue = "-";
            if (account instanceof LoanAccount loan) {
                loanLimit = String.format("%.2f", loan.getLoanLimit());
                amountDue = String.format("%.2f", loan.getAmountDue());
            }
            tableModel.addRow(new Object[]{
                    account.getAccountNumber(),
                    account.getType(),
                    account.getAccountHolderName(),
                    account.getBranch(),
                    account.getPhone(),
                    String.format("%.2f", account.getBalance()),
                    loanLimit,
                    amountDue
            });
        }
    }

    private void clearManagementFields() {
        txtName.setText("");
        txtPhone.setText("");
        txtBranch.setText("");
        txtAmount.setText("");
    }

    private void alert(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ========================================================================
    // Theme handling (kept from the project's themed chrome)
    // ========================================================================

    private void installDefaultTheme() {
        try {
            DEFAULT_THEME.apply();
            currentTheme = DEFAULT_THEME;
            lastGoodTheme = DEFAULT_THEME;
        } catch (Exception e) {
            try {
                SwingTheme.METAL.apply();
                currentTheme = SwingTheme.METAL;
                lastGoodTheme = SwingTheme.METAL;
            } catch (Exception ignored) {
                // keep whatever L&F the JVM started with
            }
        }
    }

    private void applyTheme(SwingTheme theme) {
        if (theme == currentTheme) {
            return;
        }
        SwingTheme previousGood = lastGoodTheme;
        try {
            theme.apply();
            currentTheme = theme;
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            applyChrome(theme);
            scheduleThemePromotion(theme);
        } catch (Throwable e) {
            revertTheme(previousGood, theme, describeThrowable(e));
        }
    }

    private void applyChrome(SwingTheme theme) {
        SwingTheme.Palette p = theme.palette();

        rootPanel.setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        rootPanel.setBackground(p.background());
        contentPanel.setBackground(p.background());
        titleBar.applyPalette(p);
        titleBar.applyFont(theme.chromeFont(Font.BOLD, 14f));

        titleLabel.setForeground(p.accent());
        titleLabel.setFont(theme.headingFont(Font.BOLD, 26f));
        themeSelector.applyPalette(p);
        themeSelector.setSelected(theme);

        for (JLabel label : formLabels) {
            label.setForeground(p.foreground());
        }

        applyTableChrome(theme);
        repaint();
    }

    private void applyTableChrome(SwingTheme theme) {
        SwingTheme.Palette p = theme.palette();
        boolean cyber = theme == SwingTheme.CYBERPUNK;
        Color rowA = cyber ? new Color(0x0A0A18) : p.background();
        Color rowB = cyber ? new Color(0x111128) : p.surface();
        Color grid = cyber ? new Color(0x321A55) : p.surface();
        Color selected = cyber ? new Color(0x4A103A) : p.accent();

        accountTable.setBackground(rowA);
        accountTable.setForeground(p.foreground());
        accountTable.setGridColor(grid);
        accountTable.setSelectionBackground(selected);
        accountTable.setSelectionForeground(cyber ? new Color(0x00F0FF) : p.accentText());
        accountTable.setFont(theme.chromeFont(Font.PLAIN, 13f));

        JTableHeader header = accountTable.getTableHeader();
        header.setBackground(cyber ? new Color(0x1C0C30) : p.surface());
        header.setForeground(cyber ? new Color(0xFF2A6D) : p.accent());
        header.setFont(theme.headingFont(Font.BOLD, 12f));

        accountTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(column == 2 || column == 3 ? SwingConstants.LEFT : SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? rowA : rowB);
                    c.setForeground(p.foreground());
                }
                return c;
            }
        });

        if (txtSummary != null) {
            txtSummary.setBackground(rowA);
            txtSummary.setForeground(p.foreground());
            txtSummary.setFont(theme.chromeFont(Font.PLAIN, 13f));
        }
        tableScroll.getViewport().setBackground(rowA);
    }

    private void scheduleThemePromotion(SwingTheme theme) {
        if (promoteTimer != null) {
            promoteTimer.stop();
        }
        promoteTimer = new Timer(400, e -> markThemeAsGood(theme));
        promoteTimer.setRepeats(false);
        promoteTimer.start();
    }

    private void markThemeAsGood(SwingTheme theme) {
        if (currentTheme == theme && !recoveringTheme) {
            lastGoodTheme = theme;
        }
    }

    private void handleEventThreadError(Throwable error) {
        if (recoveringTheme) {
            return;
        }
        if (isLookAndFeelError(error) && currentTheme != lastGoodTheme) {
            recoveringTheme = true;
            if (promoteTimer != null) {
                promoteTimer.stop();
            }
            SwingTheme broken = currentTheme;
            try {
                revertTheme(lastGoodTheme, broken, describeThrowable(error));
            } finally {
                recoveringTheme = false;
            }
        } else if (!isLookAndFeelError(error)) {
            error("An unexpected error occurred:\n" + describeThrowable(error));
        }
    }

    private void revertTheme(SwingTheme target, SwingTheme broken, String reason) {
        SwingTheme restored = target;
        try {
            target.apply();
            currentTheme = target;
        } catch (Throwable e) {
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
            // ignore
        }
        final String restoredLabel = restored.getLabel();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "The '" + broken.getLabel() + "' theme is not supported on this system.\n"
                        + "Reverted to '" + restoredLabel + "'.\n\nDetails: " + reason,
                "Theme Not Available", JOptionPane.WARNING_MESSAGE));
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

    private String describeThrowable(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            message = t.getClass().getSimpleName();
        }
        return message;
    }

    // ========================================================================
    // Stand-alone launcher (delegates to Main in normal use)
    // ========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                com.amarbank.Main.main(args);
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                JOptionPane.showMessageDialog(null, "The application failed to start:\n" + t,
                        "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
