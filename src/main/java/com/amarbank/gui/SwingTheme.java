package com.amarbank.gui;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

/**
 * The themes offered in the Theme menu. Each one maps to a Swing
 * Look&Feel; the dark and CYBERPUNK variants additionally override
 * UIManager colour keys (and, for Cyberpunk, the button UI) to restyle
 * the whole interface.
 */
public enum SwingTheme {

    METAL("Metal", new Palette(
            new Color(0xEEEEEE), new Color(0xD6D9DF), new Color(0x141414),
            new Color(0x666699), Color.WHITE, new Color(0xC0392B))),
    NIMBUS("Nimbus", new Palette(
            new Color(0xF0F0F2), new Color(0xD6D9DF), new Color(0x282828),
            new Color(0x39698A), Color.WHITE, new Color(0xC0392B))),
    NIMBUS_DARK("Nimbus Dark", new Palette(
            new Color(0x2B2B2B), new Color(0x3C3F41), new Color(0xE6E6E6),
            new Color(0x73A4D1), new Color(0x0F0F0F), new Color(0xE74C3C))),
    CYBERPUNK("Cyberpunk", new Palette(
            new Color(0x0A0A18), new Color(0x1C0C30), new Color(0xE0F8FF),
            new Color(0x00F0FF), new Color(0x08081A), new Color(0xFF2A6D)));

    /**
     * The colours used to paint the application chrome (custom title bar,
     * status bar, panels) so the whole window matches the active theme.
     *
     * @param background main window background
     * @param surface    raised surfaces (title bar, status bar, side panel)
     * @param foreground primary text colour
     * @param accent     highlight colour (title text, borders, hovers)
     * @param accentText text drawn on top of the accent colour
     * @param danger     destructive accent (e.g. the close button hover)
     */
    public record Palette(Color background, Color surface, Color foreground,
                          Color accent, Color accentText, Color danger) {
    }

    /** Nimbus colour keys touched by the dark/cyberpunk variants. */
    private static final String[] NIMBUS_OVERRIDE_KEYS = {
            "control", "info", "nimbusBase", "nimbusLightBackground",
            "nimbusFocus", "nimbusSelectionBackground", "nimbusSelection",
            "text", "background", "nimbusBlueGrey", "menu", "menuText",
            "textForeground", "Table.background", "Table.foreground",
            "Table.alternateRowColor", "TableHeader.background",
            "TableHeader.foreground", "Panel.background", "Label.foreground",
            "MenuBar.background", "Menu.foreground", "MenuItem.background",
            "MenuItem.foreground", "ScrollPane.background", "Viewport.background",
            "TitledBorder.titleColor"
    };

    /** Cyberpunk neon palette. */
    private static final Color CP_BG = new Color(10, 10, 24);
    private static final Color CP_PANEL = new Color(16, 16, 34);
    private static final Color CP_CYAN = new Color(0, 240, 255);
    private static final Color CP_PINK = new Color(255, 42, 109);
    private static final Color CP_TEXT = new Color(224, 248, 255);

    private final String label;
    private final Palette palette;

    SwingTheme(String label, Palette palette) {
        this.label = label;
        this.palette = palette;
    }

    public String getLabel() {
        return label;
    }

    public Palette palette() {
        return palette;
    }

    /**
     * The font for large headings / the app title in the custom chrome.
     * Cyberpunk uses the bundled Orbitron display face; other themes keep
     * the standard sans-serif.
     */
    public Font headingFont(int style, float size) {
        return this == CYBERPUNK
                ? CyberpunkFonts.display(style, size)
                : new Font("SansSerif", style, (int) size);
    }

    /**
     * The font for smaller chrome text (title bar, table header). Cyberpunk
     * uses the bundled Share Tech Mono HUD face; others keep sans-serif.
     */
    public Font chromeFont(int style, float size) {
        return this == CYBERPUNK
                ? CyberpunkFonts.body(style, size)
                : new Font("SansSerif", style, (int) size);
    }

    public static SwingTheme fromLabel(String label) {
        for (SwingTheme theme : values()) {
            if (theme.label.equals(label)) {
                return theme;
            }
        }
        return NIMBUS;
    }

    /** The theme currently installed application-wide. */
    private static SwingTheme active = NIMBUS;

    public static SwingTheme active() {
        return active;
    }

    /** Installs this theme's Look&Feel and decorations into the UIManager. */
    public void apply() throws Exception {
        clearOverrides();
        switch (this) {
            case METAL -> UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            case NIMBUS -> UIManager.setLookAndFeel(nimbusClassName());
            case NIMBUS_DARK -> {
                installNimbusDarkColors();
                UIManager.setLookAndFeel(nimbusClassName());
            }
            case CYBERPUNK -> {
                installCyberpunkColors();
                UIManager.setLookAndFeel(nimbusClassName());
            }
        }
        active = this;
    }

    private static String nimbusClassName() throws Exception {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        throw new Exception("Nimbus Look&Feel is not available on this JVM.");
    }

    private static void installNimbusDarkColors() {
        UIManager.put("control", new Color(60, 63, 65));
        UIManager.put("info", new Color(60, 63, 65));
        UIManager.put("nimbusBase", new Color(18, 30, 49));
        UIManager.put("nimbusLightBackground", new Color(43, 43, 43));
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusSelectionBackground", new Color(82, 109, 165));
        UIManager.put("text", new Color(230, 230, 230));
    }

    private static void installCyberpunkColors() {
        // Base Nimbus palette: deep midnight backgrounds, neon highlights.
        UIManager.put("control", CP_PANEL);
        UIManager.put("info", CP_PANEL);
        UIManager.put("nimbusBase", new Color(28, 12, 48));
        UIManager.put("nimbusBlueGrey", new Color(24, 24, 48));
        UIManager.put("nimbusLightBackground", CP_BG);
        UIManager.put("background", CP_BG);
        UIManager.put("nimbusFocus", CP_CYAN);
        UIManager.put("nimbusSelection", CP_PINK);
        UIManager.put("nimbusSelectionBackground", new Color(80, 16, 60));
        UIManager.put("text", CP_TEXT);
        UIManager.put("menu", CP_PANEL);
        UIManager.put("menuText", CP_CYAN);
        UIManager.put("textForeground", CP_TEXT);

        // Component-level keys so the table and panels match.
        UIManager.put("Panel.background", new ColorUIResource(CP_BG));
        UIManager.put("Label.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("Table.background", new ColorUIResource(CP_BG));
        UIManager.put("Table.foreground", new ColorUIResource(CP_TEXT));
        UIManager.put("Table.alternateRowColor", new ColorUIResource(CP_PANEL));
        UIManager.put("Table.selectionBackground", new ColorUIResource(new Color(80, 16, 60)));
        UIManager.put("Table.selectionForeground", new ColorUIResource(CP_CYAN));
        UIManager.put("TableHeader.background", new ColorUIResource(new Color(28, 12, 48)));
        UIManager.put("TableHeader.foreground", new ColorUIResource(CP_PINK));

        // Menu bar, scroll pane and titled borders.
        UIManager.put("MenuBar.background", new ColorUIResource(new Color(28, 12, 48)));
        UIManager.put("Menu.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("MenuItem.background", new ColorUIResource(CP_PANEL));
        UIManager.put("MenuItem.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("ScrollPane.background", new ColorUIResource(CP_BG));
        UIManager.put("Viewport.background", new ColorUIResource(CP_BG));
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(CP_PINK));

        // The custom angular neon button UI.
        UIManager.put("ButtonUI", CyberpunkButtonUI.class.getName());

        // Cyberpunk display fonts applied to all standard Swing widgets.
        installCyberpunkFonts();
    }

    /** Applies the bundled cyberpunk fonts to the common Swing UI font keys. */
    private static void installCyberpunkFonts() {
        FontUIResource body = new FontUIResource(CyberpunkFonts.body(Font.PLAIN, 13f));
        FontUIResource bodyBold = new FontUIResource(CyberpunkFonts.body(Font.BOLD, 13f));
        FontUIResource heading = new FontUIResource(CyberpunkFonts.display(Font.BOLD, 12f));

        for (String key : new String[]{
                "Label.font", "Button.font", "ToggleButton.font", "RadioButton.font",
                "CheckBox.font", "TextField.font", "TextArea.font", "ComboBox.font",
                "List.font", "Table.font", "Menu.font", "MenuItem.font",
                "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "PopupMenu.font",
                "ToolTip.font", "OptionPane.font", "Panel.font", "Viewport.font",
                "ScrollPane.font"}) {
            UIManager.put(key, body);
        }
        // Headers/menu-bar read as the more decorative display face.
        UIManager.put("TableHeader.font", heading);
        UIManager.put("MenuBar.font", heading);
        UIManager.put("TitledBorder.font", bodyBold);
    }

    private static final String[] FONT_KEYS = {
            "Label.font", "Button.font", "ToggleButton.font", "RadioButton.font",
            "CheckBox.font", "TextField.font", "TextArea.font", "ComboBox.font",
            "List.font", "Table.font", "TableHeader.font", "Menu.font", "MenuItem.font",
            "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "PopupMenu.font",
            "MenuBar.font", "ToolTip.font", "OptionPane.font", "Panel.font",
            "Viewport.font", "ScrollPane.font", "TitledBorder.font"
    };

    private static void clearOverrides() {
        for (String key : NIMBUS_OVERRIDE_KEYS) {
            UIManager.put(key, null);
        }
        // Reset the component keys touched by Cyberpunk.
        for (String key : new String[]{
                "Table.selectionBackground", "Table.selectionForeground",
                "TableHeader.background", "TableHeader.foreground"}) {
            UIManager.put(key, null);
        }
        // Restore the default button UI (removes the custom cyberpunk buttons).
        UIManager.put("ButtonUI", null);
        // Restore default fonts (lets the next L&F install its own).
        for (String key : FONT_KEYS) {
            UIManager.put(key, null);
        }
    }

    /** True for themes whose custom decorations are pure-Java (always safe). */
    public boolean isCustom() {
        return this == CYBERPUNK;
    }
}
