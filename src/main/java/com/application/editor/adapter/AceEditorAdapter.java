package com.application.editor.adapter;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;

public class AceEditorAdapter {

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

    private final WebEngine webEngine;
    private boolean isLoaded = false;

    public AceEditorAdapter(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                this.isLoaded = true;
            }
        });
    }

    public void undo() {
        if (isLoaded) {
            webEngine.executeScript("editor.execCommand('undo');");
        }
    }

    public void redo() {
        if (isLoaded) {
            webEngine.executeScript("editor.execCommand('redo');");
        }
    }

    public void selectAll() {
        if (isLoaded) {
            webEngine.executeScript("editor.selectAll();");
        }
    }

    public void find() {
        if (isLoaded) {
            webEngine.executeScript("editor.execCommand('find');");
        }
    }

    public String getText() {
        if (!isLoaded) return "";
        Object result = webEngine.executeScript("editor.getValue();");
        return (result != null) ? result.toString() : "";
    }

    public void setText(String content) {
        if (isLoaded) {
            webEngine.executeScript("editor.setValue(" + quote(content) + ", -1);");
        }
    }

    // Type-safe setMode method using Enum
    public void setMode(EditorMode mode) {
        if (isLoaded && mode != null) {
            webEngine.executeScript("editor.session.setMode(" + quote("ace/mode/" + mode.getAceMode()) + ");");
        }
    }

    private String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}