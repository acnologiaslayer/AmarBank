package com.amarbank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Adds edge/corner resizing to an undecorated {@link JFrame}. Because the
 * frame paints its own chrome ({@link TitleBar}) it loses the native resize
 * grips, so this restores them: hovering near an edge shows the matching
 * resize cursor, and dragging resizes the window (respecting its minimum
 * size).
 */
public class WindowResizer extends MouseAdapter {

    private static final int BORDER = 6; // px hot-zone around the edges

    private final JFrame frame;
    private int zone = Cursor.DEFAULT_CURSOR;
    private Rectangle startBounds;
    private Point startScreen;

    public WindowResizer(JFrame frame) {
        this.frame = frame;
        Component glass = frame.getRootPane().getContentPane();
        glass.addMouseListener(this);
        glass.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                resize(e);
            }
        });
    }

    private void updateCursor(MouseEvent e) {
        zone = zoneFor(e.getPoint(), e.getComponent().getSize());
        e.getComponent().setCursor(Cursor.getPredefinedCursor(zone));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        zone = zoneFor(e.getPoint(), e.getComponent().getSize());
        startBounds = frame.getBounds();
        startScreen = e.getLocationOnScreen();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        zone = Cursor.DEFAULT_CURSOR;
        e.getComponent().setCursor(Cursor.getDefaultCursor());
    }

    private void resize(MouseEvent e) {
        if (zone == Cursor.DEFAULT_CURSOR || startBounds == null) {
            return;
        }
        Point now = e.getLocationOnScreen();
        int dx = now.x - startScreen.x;
        int dy = now.y - startScreen.y;

        Rectangle b = new Rectangle(startBounds);
        Dimension min = frame.getMinimumSize();

        switch (zone) {
            case Cursor.E_RESIZE_CURSOR -> b.width += dx;
            case Cursor.S_RESIZE_CURSOR -> b.height += dy;
            case Cursor.SE_RESIZE_CURSOR -> {
                b.width += dx;
                b.height += dy;
            }
            case Cursor.W_RESIZE_CURSOR -> {
                b.x += dx;
                b.width -= dx;
            }
            case Cursor.N_RESIZE_CURSOR -> {
                b.y += dy;
                b.height -= dy;
            }
            default -> {
                return;
            }
        }

        // Respect the minimum size without letting the top-left jump.
        if (b.width < min.width) {
            if (zone == Cursor.W_RESIZE_CURSOR) {
                b.x = startBounds.x + startBounds.width - min.width;
            }
            b.width = min.width;
        }
        if (b.height < min.height) {
            if (zone == Cursor.N_RESIZE_CURSOR) {
                b.y = startBounds.y + startBounds.height - min.height;
            }
            b.height = min.height;
        }
        frame.setBounds(b);
        frame.validate();
    }

    private int zoneFor(Point p, Dimension size) {
        boolean east = p.x >= size.width - BORDER;
        boolean west = p.x <= BORDER;
        boolean south = p.y >= size.height - BORDER;
        boolean north = p.y <= BORDER;

        if (south && east) return Cursor.SE_RESIZE_CURSOR;
        if (east) return Cursor.E_RESIZE_CURSOR;
        if (west) return Cursor.W_RESIZE_CURSOR;
        if (south) return Cursor.S_RESIZE_CURSOR;
        if (north) return Cursor.N_RESIZE_CURSOR;
        return Cursor.DEFAULT_CURSOR;
    }
}
