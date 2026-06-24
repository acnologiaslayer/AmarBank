package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small reusable modal pop-up form.
 *
 * Built in the Playground Swing style (a dialog with a GridBag layout and
 * anonymous ActionListeners), but undecorated with a custom {@link TitleBar}
 * so the pop-up - including its title - follows the active {@link SwingTheme}
 * instead of the native OS window chrome. Callers add labelled fields, show
 * the dialog, and read the typed values back after it closes.
 *
 * Example:
 * <pre>
 *   FormDialog form = new FormDialog(parent, "Deposit");
 *   JTextField account = form.addTextField("Account number:");
 *   JTextField amount  = form.addTextField("Amount:");
 *   if (form.showDialog()) {
 *       // use account.getText(), amount.getText()
 *   }
 * </pre>
 */
public class FormDialog extends JDialog {

    private final JPanel rootPanel;
    private final TitleBar titleBar;
    private final JPanel fieldsPanel;
    private final GridBagConstraints gbc;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final List<JLabel> rowLabels = new ArrayList<>();
    private final JButton okButton;
    private final JButton cancelButton;
    private final JPanel buttonBar;
    private boolean confirmed = false;
    private JComponent firstField;

    public FormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true); // we paint our own themed title bar

        rootPanel = new JPanel(new BorderLayout(10, 10));
        setContentPane(rootPanel);

        titleBar = new TitleBar(this, title);
        rootPanel.add(titleBar, BorderLayout.NORTH);

        fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        rootPanel.add(fieldsPanel, BorderLayout.CENTER);

        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        buttonBar = buildButtonBar();
        rootPanel.add(buttonBar, BorderLayout.SOUTH);
    }

    // ---------- builders ----------

    /** Adds a labelled single-line text field and returns it. */
    public JTextField addTextField(String label) {
        JTextField field = new JTextField(18);
        addRow(label, field);
        return field;
    }

    /** Adds a labelled drop-down and returns the combo box. */
    public JComboBox<String> addComboBox(String label, String[] options) {
        JComboBox<String> combo = new JComboBox<>(options);
        addRow(label, combo);
        return combo;
    }

    private void addRow(String label, JComponent field) {
        int row = fields.size();

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel jLabel = new JLabel(label);
        rowLabels.add(jLabel);
        fieldsPanel.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fieldsPanel.add(field, gbc);

        fields.put(label, field);
        if (firstField == null) {
            firstField = field;
        }
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        bar.add(okButton);
        bar.add(cancelButton);
        getRootPane().setDefaultButton(okButton); // Enter confirms
        return bar;
    }

    /** Recolours the dialog chrome to match the active theme. */
    private void applyTheme() {
        SwingTheme theme = SwingTheme.active();
        SwingTheme.Palette p = theme.palette();
        rootPanel.setBorder(BorderFactory.createLineBorder(p.accent(), 1));
        rootPanel.setBackground(p.background());
        fieldsPanel.setBackground(p.background());
        buttonBar.setBackground(p.background());
        titleBar.applyPalette(p);
        titleBar.applyFont(theme.chromeFont(Font.BOLD, 14f));
        for (JLabel label : rowLabels) {
            label.setForeground(p.foreground());
            label.setFont(theme.chromeFont(Font.PLAIN, 13f));
        }
    }

    // ---------- show / read ----------

    /** Displays the dialog modally; returns true if the user pressed OK. */
    public boolean showDialog() {
        SwingUtilities.updateComponentTreeUI(this);
        applyTheme();
        pack();
        setLocationRelativeTo(getOwner());
        if (firstField != null) {
            firstField.requestFocusInWindow();
        }
        setVisible(true);
        return confirmed;
    }
}
