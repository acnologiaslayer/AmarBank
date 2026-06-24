package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * The theme selector shown in the bottom-right of the window. Instead of a
 * "Theme" menu it is a small custom-painted palette icon; clicking it pops
 * up the list of available themes. The icon and the popup recolour to match
 * the active theme palette.
 */
public class ThemeSelectorButton extends JButton {

    private final JPopupMenu popup = new JPopupMenu();
    private final JRadioButtonMenuItem[] items;
    private SwingTheme.Palette palette = SwingTheme.active().palette();
    private boolean hovering;

    public ThemeSelectorButton(SwingTheme current, Consumer<SwingTheme> onSelect) {
        setPreferredSize(new Dimension(34, 34));
        setToolTipText("Change theme");
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Keep the popup lightweight so it always renders inside the window's
        // own (themed) layer rather than a separate native heavyweight window.
        popup.setLightWeightPopupEnabled(true);

        SwingTheme[] themes = SwingTheme.values();
        items = new JRadioButtonMenuItem[themes.length];
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < themes.length; i++) {
            SwingTheme theme = themes[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getLabel());
            item.setSelected(theme == current);
            item.addActionListener(e -> onSelect.accept(theme));
            group.add(item);
            popup.add(item);
            items[i] = item;
        }

        addActionListener(e -> showPopup());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaint();
            }
        });
    }

    private void showPopup() {
        // Anchor the popup just above the icon, right-aligned, clamping so it
        // is always fully on screen (the previous fixed negative offset could
        // place it off-screen under some Look&Feels, e.g. Metal).
        Dimension pref = popup.getPreferredSize();
        int x = getWidth() - pref.width;
        int y = -pref.height - 4;
        popup.show(this, x, y);
    }

    /** Marks the given theme as selected in the popup. */
    public void setSelected(SwingTheme theme) {
        SwingTheme[] themes = SwingTheme.values();
        for (int i = 0; i < items.length; i++) {
            items[i].setSelected(themes[i] == theme);
        }
    }

    public void applyPalette(SwingTheme.Palette palette) {
        this.palette = palette;
        popup.setBackground(palette.surface());
        popup.setBorder(BorderFactory.createLineBorder(palette.accent(), 1));
        for (JRadioButtonMenuItem item : items) {
            item.setBackground(palette.surface());
            item.setForeground(palette.foreground());
            item.setOpaque(true);
        }
        SwingUtilities.updateComponentTreeUI(popup);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (hovering) {
            g2.setColor(new Color(palette.accent().getRed(), palette.accent().getGreen(),
                    palette.accent().getBlue(), 50));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 10, 10);
        }

        // A painter's-palette glyph: a rounded blob with paint dots, drawn
        // in the theme accent colour with multi-coloured dots.
        int cx = w / 2;
        int cy = h / 2;
        int r = Math.min(w, h) / 2 - 6;

        g2.setColor(palette.accent());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);

        Color[] dots = {palette.accent(), palette.danger(), palette.foreground()};
        int dotR = Math.max(2, r / 4);
        int[][] offsets = {{-r / 2, -r / 3}, {r / 2, -r / 3}, {0, r / 2}};
        for (int i = 0; i < dots.length; i++) {
            g2.setColor(dots[i]);
            g2.fillOval(cx + offsets[i][0] - dotR / 2, cy + offsets[i][1] - dotR / 2, dotR, dotR);
        }
        g2.dispose();
    }
}
