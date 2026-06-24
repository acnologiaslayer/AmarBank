package com.amarbank.gui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and exposes the bundled "cyberpunk" display fonts used by the
 * Cyberpunk theme.
 *
 * Two SIL OFL licensed fonts are shipped on the classpath next to this
 * class (see fonts/OFL.txt):
 *  - Orbitron      - a geometric, futuristic display face for headings.
 *  - Share Tech Mono - a monospaced HUD/terminal face for body text.
 *
 * The fonts are loaded once and registered with the local graphics
 * environment so they can be referenced by name too. If loading fails for
 * any reason the methods fall back to a generic monospaced font, so the
 * theme still works without the bundled assets.
 */
public final class CyberpunkFonts {

    private static Font display; // Orbitron
    private static Font body;    // Share Tech Mono
    private static boolean loaded;

    private CyberpunkFonts() {
    }

    /** The futuristic display font (headings, title bar), at the given size/style. */
    public static Font display(int style, float size) {
        ensureLoaded();
        return display.deriveFont(style, size);
    }

    /** The monospaced HUD body font (labels, table, buttons), at the given size/style. */
    public static Font body(int style, float size) {
        ensureLoaded();
        return body.deriveFont(style, size);
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        display = load("fonts/Orbitron.ttf", "Monospaced");
        body = load("fonts/ShareTechMono.ttf", "Monospaced");
        loaded = true;
    }

    private static Font load(String resource, String fallbackFamily) {
        try (InputStream in = CyberpunkFonts.class.getResourceAsStream(resource)) {
            if (in == null) {
                return new Font(fallbackFamily, Font.PLAIN, 12);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            return new Font(fallbackFamily, Font.PLAIN, 12);
        }
    }
}
