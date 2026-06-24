package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * A custom, fully theme-able window title bar used by the undecorated
 * {@link BankAppGUI} frame and the undecorated {@link FormDialog}/themed
 * pop-ups. Because those windows are undecorated, this bar (and the window
 * border) is painted by us, so the whole window - including its title -
 * matches the active {@link SwingTheme} instead of the native OS chrome.
 *
 * For top-level frames it offers minimise / maximise / close and
 * drag-to-move; for dialogs it shows only a close button.
 */
public class TitleBar extends JPanel {

    private final Window window;
    private final JLabel titleLabel;
    private final ChromeButton minimiseButton; // null for dialogs
    private final ChromeButton maximiseButton; // null for dialogs
    private final ChromeButton closeButton;

    private Point dragOffset;
    private Rectangle normalBounds; // remembered bounds before maximising

    public TitleBar(Window window, String title) {
        this.window = window;
        boolean isFrame = window instanceof Frame;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));
        setPreferredSize(new Dimension(10, 38));

        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 5));
        buttons.setOpaque(false);

        if (isFrame) {
            minimiseButton = new ChromeButton(ChromeButton.Glyph.MINIMISE);
            maximiseButton = new ChromeButton(ChromeButton.Glyph.MAXIMISE);
            minimiseButton.addActionListener(e -> ((Frame) window).setExtendedState(Frame.ICONIFIED));
            maximiseButton.addActionListener(e -> toggleMaximise());
            buttons.add(minimiseButton);
            buttons.add(maximiseButton);
        } else {
            minimiseButton = null;
            maximiseButton = null;
        }

        closeButton = new ChromeButton(ChromeButton.Glyph.CLOSE);
        closeButton.addActionListener(e -> window.dispatchEvent(
                new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        buttons.add(closeButton);
        add(buttons, BorderLayout.EAST);

        installDragSupport(isFrame);
    }

    /** Recolours the bar and its buttons for the given palette. */
    public void applyPalette(SwingTheme.Palette palette) {
        setBackground(palette.surface());
        titleLabel.setForeground(palette.accent());
        if (minimiseButton != null) {
            minimiseButton.applyPalette(palette, false);
            maximiseButton.applyPalette(palette, false);
        }
        closeButton.applyPalette(palette, true);
        repaint();
    }

    /** Sets the title font (so Cyberpunk can use its display face). */
    public void applyFont(Font font) {
        titleLabel.setFont(font);
        repaint();
    }

    private void toggleMaximise() {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle screen = gc.getBounds();
        Insets si = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usable = new Rectangle(screen.x + si.left, screen.y + si.top,
                screen.width - si.left - si.right, screen.height - si.top - si.bottom);

        if (normalBounds == null) {
            normalBounds = window.getBounds();
            window.setBounds(usable);
        } else {
            window.setBounds(normalBounds);
            normalBounds = null;
        }
        window.revalidate();
    }

    private void installDragSupport(boolean allowMaximise) {
        MouseAdapter press = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (allowMaximise && e.getClickCount() == 2) {
                    toggleMaximise();
                }
            }
        };
        MouseMotionAdapter drag = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null || normalBounds != null) {
                    return; // not dragging, or currently maximised
                }
                Point onScreen = e.getLocationOnScreen();
                window.setLocation(onScreen.x - dragOffset.x, onScreen.y - dragOffset.y);
            }
        };
        addMouseListener(press);
        addMouseMotionListener(drag);
        titleLabel.addMouseListener(press);
        titleLabel.addMouseMotionListener(drag);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // A thin accent underline gives the bar a HUD-like edge.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(titleLabel.getForeground());
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
    }
}
