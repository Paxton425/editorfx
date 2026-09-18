package com.application.editor.adapter;

import com.application.editor.model.ShellCommand;
import com.application.editor.model.TerminalSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

// x-Term Terminal Adapter
public class XtermTerminalAdapter {

    private static final Logger logger = LoggerFactory.getLogger(XtermTerminalAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebEngine webEngine;
    private TerminalSession session;

    // CRITICAL: Hold a strong instance reference so Garbage Collection does NOT sweep JavaBridge!
    private final JavaBridge bridge;

    public XtermTerminalAdapter(WebEngine webEngine, TerminalSession session) {
        this.webEngine = webEngine;
        this.session = session;
        this.bridge = new JavaBridge(this); // Instantiate once here
    }

    public void start() throws NullPointerException {
        if (session == null) {
            logger.error("Session class is not defined!");
            throw new NullPointerException("Session is null");
        }
        if (webEngine == null) {
            logger.error("Web Engine class is not defined!");
            throw new NullPointerException("WebEngine is null");
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                logger.info("Xterm Terminal State Succeeded.");

                // 1. Expose our strongly-referenced bridge to window.javaTerminal
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaTerminal", bridge);

                // 2. Attach xterm.js input listener
                webEngine.executeScript("""
                    if (typeof terminal !== 'undefined') {
                        terminal.onData(function(data) {
                            if (window.javaTerminal) {
                                window.javaTerminal.onInput(data);
                            }
                        });
                    }
                """);

                // 3. Start background reader thread (PowerShell → xterm.js)
                Thread outputThread = getShellThread(session);
                outputThread.setDaemon(true);
                outputThread.start();
            }
        });
    }

    private Thread getShellThread(TerminalSession session) {
        return new Thread(() -> {
            try (Reader reader = new InputStreamReader(session.getOutput(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int charsRead;

                while ((charsRead = reader.read(buffer)) != -1) {
                    String text = new String(buffer, 0, charsRead);
                    String quotedText = quote(text);

                    // Forward output to WebEngine UI thread
                    Platform.runLater(() -> {
                        try {
                            webEngine.executeScript("terminal.write(" + quotedText + ");");
                        } catch (Exception e) {
                            logger.error("Error writing to xterm.js", e);
                        }
                    });
                }
            } catch (IOException e) {
                logger.debug("Shell thread output stream closed.");
            }
        });
    }

    public TerminalSession getShellSession() {
        return session;
    }

    public void write(String input) throws IOException {
        if (session == null) {
            throw new IllegalStateException("Terminal is not running.");
        }
        session.write(input);
    }

    // Must be public so WebEngine reflection can access onInput()
    public static class JavaBridge {
        private final XtermTerminalAdapter adapter;

        public JavaBridge(XtermTerminalAdapter adapter) {
            this.adapter = adapter;
        }

        public void onInput(String input) {
            try {
                if (adapter != null && adapter.session != null) {
                    adapter.session.write(input);
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(JavaBridge.class).error("Java Bridge IO Error", e);
            }
        }
    }

    public void clearConsole() {
        try {
            if (webEngine != null) {
                Platform.runLater(() -> {
                    try {
                        // Cancel/discard whatever is currently typed at the prompt WITH Ctrl+C
                        session.write(ShellCommand.CLEAR_SCREEN.getBytes());
                        session.write("clear \r".getBytes());
                    } catch (IOException e) {
                        logger.error("Error Clearing Console", e);
                    }
                });
            } else {
                logger.error("Webview is Undefined!");
            }
        } catch (Exception e) {
            logger.error("Failed to redraw terminal", e);
        }
    }

    public void close() {
        if (session != null) {
            session.close();
            session = null;
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