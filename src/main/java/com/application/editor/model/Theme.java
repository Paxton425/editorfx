package com.application.editor.model;

/**
 * Represents available application themes and their mapped resource definitions.
 */
public enum Theme {
    NATIVE("", "eclipse", "Native"), // No Style
    LIGHT("/com/css/editor-view-light.css", "eclipse", "Light"),
    DARK("/com/css/editor-view-dark.css", "tomorrow_night_bright", "Dark");

    private final String stylesheetPath;
    private final String aceTheme;
    private final String displayName;

    Theme(String stylesheetPath, String aceTheme, String displayName) {
        this.stylesheetPath = stylesheetPath;
        this.displayName = displayName;
        this.aceTheme = aceTheme;
    }

    public String getStylesheetPath() {
        return stylesheetPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAceTheme() {
        return aceTheme;
    }

    /** Returns the opposite theme — handy for a "toggle" menu item. */
    public Theme toggle() {
        return this == DARK ? LIGHT : DARK;
    }

    public static Theme fromName(String name) {
        if (name == null) return DARK;
        for (Theme t : values()) {
            if (t.name().equalsIgnoreCase(name) || t.displayName.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return DARK;
    }
}