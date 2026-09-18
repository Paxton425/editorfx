package com.application.editor.adapter;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java bridge instance registered on window.javaBridge inside WebEngine.
 * Receives execution calls directly from Ace Editor's JavaScript session events.
 */
public class JavaBridge {

    private static final Logger logger = LoggerFactory.getLogger(JavaBridge.class);

    private final Runnable onChangeCallback;
    private volatile boolean suppressEvents = false;

    public JavaBridge(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;
    }

    /**
     * Temporarily mute bridge execution (e.g. while programmatically populating text).
     */
    public void setSuppressEvents(boolean suppressEvents) {
        this.suppressEvents = suppressEvents;
    }

    /**
     * Invoked reflectively from JS when Ace fires editor.session.on('change').
     * MUST be public for netscape.javascript.JSObject visibility.
     */
    public void onContentChanged() {
        if (suppressEvents) return;

        // Ensure JavaFX UI mutations (tab titles, status bars) happen safely on the FX Application Thread
        Platform.runLater(() -> {
            if (onChangeCallback != null) {
                try {
                    onChangeCallback.run();
                } catch (Exception e) {
                    logger.error("Error executing Ace change callback", e);
                }
            }
        });
    }
}
