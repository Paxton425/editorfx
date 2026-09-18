package com.application.editor.adapter;

import com.application.editor.model.EditorMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AceEditorAdapter {
    private final WebEngine webEngine;
    private boolean isLoaded = false;

    private Runnable onContentChangeListener;
    private Runnable onPositionChangeListener; //Cursor position change

    private static final ObjectMapper MAPPER = new ObjectMapper();
    Logger logger = LoggerFactory.getLogger(AceEditorAdapter.class);

    public AceEditorAdapter(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                this.isLoaded = true;
            }
        });
    }

    /**
     * Call this after editor.html finishes loading.
     */
    public void setOnContentChangeListener(Runnable onContentChangeListener) {
        this.onContentChangeListener = onContentChangeListener;
    }
    public void setOnCursorPositionChangeListener(Runnable onPositionChangeListener) {
        this.onPositionChangeListener = onPositionChangeListener;
    }

    /**
     * Call this after editor.html finishes loading.
     */
    public void setupJavaBridge() {
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaBridge", new JavaBridge());

        // Ace edit listener in JS
        webEngine.executeScript(
                "if (typeof editor !== 'undefined') { " +
                        "    editor.session.on('change', function() { " +
                        "        if (window.javaBridge) { window.javaBridge.onContentChanged(); } " +
                        "    }); " +
                        "}"
        );

        // Ace cursor position change event listener in JS -> Java Bridge
        webEngine.executeScript("""
                if(typeof editor !== 'undefined') {
                    editor.selection.on("changeCursor", function() {
                        if(window.javaBridge) { window.javaBridge.onPositionChanged(); } 
                    });
                }
                """);
    }

    // Must be public so JS can invoke it via reflection
    public class JavaBridge {
        public void onContentChanged() {
            Platform.runLater(() -> {
                if (onContentChangeListener != null) {
                    onContentChangeListener.run();
                }
            });
        }
        public void onPositionChanged() {
            Platform.runLater(() -> {
                if (onPositionChangeListener != null) {
                    onPositionChangeListener.run();
                }
            });
        }
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

    public int[] getCursorPositions(){
        // Correct Java syntax for array initialization
        int[] positions = new int[]{1, 1};
        if (isLoaded) {
            try {
                // 1. Execute JS to get the 0-indexed cursor position from Ace Editor
                // 2. Format it as a simple comma-separated string "row,column"
                String script = "var pos = editor.getCursorPosition(); pos.row + ',' + pos.column;";
                Object result = webEngine.executeScript(script);
                if (result instanceof String resultStr) {
                    String[] parts = resultStr.split(",");
                    if (parts.length == 2) {
                        // Ace is 0-indexed, so we add 1 to make it human-readable (Line 1, Col 1)
                        positions[0] = Integer.parseInt(parts[0]) + 1; // Line/Row
                        positions[1] = Integer.parseInt(parts[1]) + 1; // Column
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve cursor positions from Ace Editor", e);
            }
        }

        return positions;
    }

    // Type-safe setMode method using Enum
    public void setAceMode(EditorMode mode) {
        if (isLoaded && mode != null) {
            webEngine.executeScript("editor.session.setMode(" + quote("ace/mode/" + mode.getAceMode()) + ");");
        }
    }

    public void setAceTheme(String theme) {
        if (isLoaded && theme != null) {
            webEngine.executeScript("editor.setTheme(" + quote("ace/theme/" + theme) + ");");
        }
    }

    private String quote(String text) {
        try {
            return MAPPER.writeValueAsString(text);
        } catch (Exception e) {
            logger.error("Error serializing terminal text", e);
            return "\"\"";
        }
    }
}