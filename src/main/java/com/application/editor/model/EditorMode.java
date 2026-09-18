package com.application.editor.model;

// Popular Ace Editor Modes
public enum EditorMode {
    HTML("html", "fab-html5", "#E34F26", "html", "htm"),
    JAVASCRIPT("javascript", "fab-js", "#F7DF1E", "js"),
    TYPESCRIPT("typescript", "fab-js", "#3178C6", "ts"),
    CSS("css", "fab-css3-alt", "#1572B6", "css"),
    JAVA("java", "fab-java", "#ED8B00", "java"),
    PYTHON("python", "fab-python", "#3776AB", "py"),
    JSON("json", "fas-code", "#292929", "json"),
    XML("xml", "fas-code", "#E66E22", "xml"),
    SQL("sql", "fas-database", "#00758F", "sql"),
    PLAIN_TEXT("text", "far-file-alt", "#808080", "txt");

    private final String aceMode;
    private final String iconLiteral;
    private final String iconColor;
    private final String[] extensions;

    EditorMode(String aceMode, String iconLiteral, String iconColor, String... extensions) {
        this.aceMode = aceMode;
        this.iconLiteral = iconLiteral;
        this.iconColor = iconColor;
        this.extensions = extensions;
    }

    public String getAceMode() {
        return aceMode;
    }

    public String getIconLiteral() {
        return iconLiteral;
    }

    public String getIconColor() {
        return iconColor;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public static EditorMode fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return PLAIN_TEXT;
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        for (EditorMode mode : values()) {
            for (String e : mode.extensions) {
                if (e.equalsIgnoreCase(ext)) {
                    return mode;
                }
            }
        }
        return PLAIN_TEXT;
    }
}
