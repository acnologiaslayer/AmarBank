package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A small custom-painted window-control button (minimise / maximise /
 * close) for the {@link TitleBar}. It paints its own glyph and a hover
 * highlight using the active theme palette, so the window controls match
 * the rest of the themed chrome.
 */
public class ChromeButton extends JButton {

    public enum Glyph {MINIMISE, MAXIMISE, CLOSE}

    private final Glyph glyph;
    private Color iconColor = Color.DARK_GRAY;
    private Color hoverColor = new Color(0, 0, 0, 40);
    private boolean hovering;

    public ChromeButton(Glyph glyph) {
        this.glyph = glyph;
        setPreferredSize(new Dimension(34, 28));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    /** isClose selects the danger colour for the hover state. */
    public void applyPalette(SwingTheme.Palette palette, boolean isClose) {
        this.iconColor = palette.accent();
        this.hoverColor = isClose ? palette.danger() : palette.accent();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (hovering) {
            g2.setColor(new Color(hoverColor.getRed(), hoverColor.getGreen(),
                    hoverColor.getBlue(), 60));
            g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
        }

        g2.setColor(hovering ? hoverColor : iconColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = w / 2;
        int cy = h / 2;
        int s = 5; // glyph half-size

        switch (glyph) {
            case MINIMISE -> g2.drawLine(cx - s, cy + 3, cx + s, cy + 3);
            case MAXIMISE -> g2.drawRect(cx - s, cy - s, s * 2, s * 2);
            case CLOSE -> {
                g2.drawLine(cx - s, cy - s, cx + s, cy + s);
                g2.drawLine(cx + s, cy - s, cx - s, cy + s);
            }
        }
        g2.dispose();
    }
}
