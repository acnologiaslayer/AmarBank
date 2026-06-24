package com.amarbank.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * A custom Swing button UI that draws angular, neon "cyberpunk" buttons:
 * a chamfered (cut-corner) hexagonal shape with a glowing outline, a dark
 * fill, and neon text that shifts colour on hover/press.
 *
 * It is installed via the UIManager only while the Cyberpunk theme is
 * active (see {@link SwingTheme}); switching to any other theme clears the
 * "ButtonUI" override and restores the normal Look&Feel buttons.
 */
public class CyberpunkButtonUI extends BasicButtonUI {

    private static final Color NEON_CYAN = new Color(0, 240, 255);
    private static final Color NEON_PINK = new Color(255, 42, 109);
    private static final Color FILL = new Color(13, 12, 28);
    private static final Color FILL_HOVER = new Color(34, 14, 46);
    private static final Color FILL_PRESS = new Color(60, 16, 44);
    private static final Color DISABLED = new Color(90, 96, 110);
    private static final int CUT = 10; // size of the angled corner cut

    // Stateless, so a single shared instance is enough.
    private static final CyberpunkButtonUI INSTANCE = new CyberpunkButtonUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        // UIResource values so the next Look&Feel can replace them cleanly.
        b.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(8, 18, 8, 18));
        b.setFont(new javax.swing.plaf.FontUIResource(b.getFont().deriveFont(Font.BOLD)));
        b.setForeground(new javax.swing.plaf.ColorUIResource(NEON_CYAN));
        b.setRolloverEnabled(true); // so the hover state updates
        b.setOpaque(false);
        b.setContentAreaFilled(false);
    }

    @Override
    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // Restore the values mutated in installDefaults so the next theme's
        // buttons paint normally (otherwise they stay transparent/borderless).
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBorder(null);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        boolean enabled = b.isEnabled();
        boolean hover = enabled && m.isRollover();
        boolean pressed = enabled && m.isArmed() && m.isPressed();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = c.getWidth();
        int h = c.getHeight();
        Shape shape = chamfer(w, h, CUT);

        // body fill
        g2.setColor(!enabled ? FILL : (pressed ? FILL_PRESS : (hover ? FILL_HOVER : FILL)));
        g2.fill(shape);

        // glowing edge: a soft wide halo first, then a crisp neon line on top
        Color edge = !enabled ? DISABLED : (hover || pressed ? NEON_PINK : NEON_CYAN);
        g2.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 70));
        g2.setStroke(new BasicStroke(4.5f));
        g2.draw(shape);
        g2.setColor(edge);
        g2.setStroke(new BasicStroke(hover || pressed ? 2.2f : 1.5f));
        g2.draw(shape);

        // a small accent notch in the top-left cut, a typical HUD detail
        if (enabled) {
            g2.setColor(edge);
            g2.fillRect(CUT + 3, 2, 14, 2);
        }

        g2.dispose();

        // icon + text (text colour handled in paintText below)
        super.paint(g, c);
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel m = b.getModel();
        boolean enabled = b.isEnabled();
        Color neon = !enabled ? DISABLED : (m.isRollover() || (m.isArmed() && m.isPressed()) ? NEON_PINK : NEON_CYAN);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int x = textRect.x;
        int y = textRect.y + fm.getAscent();

        // faint glow halo behind the glyphs
        if (enabled) {
            g2.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 90));
            g2.drawString(text, x + 1, y + 1);
        }
        g2.setColor(neon);
        g2.drawString(text, x, y);
        g2.dispose();
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                              Rectangle textRect, Rectangle iconRect) {
        // The neon outline already signals interactivity; skip the focus box.
    }

    /** A rectangle with the top-left and bottom-right corners cut off. */
    private Shape chamfer(int w, int h, int cut) {
        Path2D path = new Path2D.Float();
        path.moveTo(cut, 1);
        path.lineTo(w - 1, 1);
        path.lineTo(w - 1, h - cut);
        path.lineTo(w - cut - 1, h - 1);
        path.lineTo(1, h - 1);
        path.lineTo(1, cut);
        path.closePath();
        return path;
    }
}
