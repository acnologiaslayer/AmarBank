package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;

/**
 * A themed replacement for {@link JOptionPane#showMessageDialog}. It is an
 * undecorated modal dialog with a custom {@link TitleBar}, so the message
 * pop-up - including its title - follows the active {@link SwingTheme}
 * instead of the native OS window chrome.
 */
public final class MessageDialog {

    public enum Kind {INFO, WARNING, ERROR}

    private MessageDialog() {
    }

    public static void show(Component parent, String title, String message, Kind kind) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        SwingTheme theme = SwingTheme.active();
        SwingTheme.Palette p = theme.palette();
        Color accent = switch (kind) {
            case ERROR -> p.danger();
            case WARNING -> p.danger();
            case INFO -> p.accent();
        };

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createLineBorder(accent, 1));
        root.setBackground(p.background());
        dialog.setContentPane(root);

        TitleBar titleBar = new TitleBar(dialog, title);
        root.add(titleBar, BorderLayout.NORTH);

        // Message body, honouring %n / \n line breaks.
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));
        JLabel icon = new JLabel(glyphFor(kind));
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 22f));
        icon.setForeground(accent);
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
        body.add(icon, BorderLayout.WEST);

        JLabel text = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>");
        text.setForeground(p.foreground());
        text.setFont(theme.chromeFont(Font.PLAIN, 13f));
        body.add(text, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.setOpaque(false);
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dialog.dispose());
        bar.add(ok);
        root.add(bar, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);

        SwingUtilities.updateComponentTreeUI(dialog);
        titleBar.applyPalette(p); // re-apply after UI refresh
        titleBar.applyFont(theme.chromeFont(Font.BOLD, 14f));
        text.setForeground(p.foreground());
        icon.setForeground(accent);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(320, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String glyphFor(Kind kind) {
        return switch (kind) {
            case ERROR -> "\u2716";   // heavy multiplication x
            case WARNING -> "\u26A0"; // warning sign
            case INFO -> "\u2139";    // information source
        };
    }
}
