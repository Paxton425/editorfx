package com.application.editor.model;

import javafx.scene.input.KeyCode;

/**
 * Shell / terminal control commands.
 * Each entry maps a logical command to:
 *   - the JavaFX {@link KeyCode} that triggers it
 *   - the raw byte(s) that get sent to the PTY/process
 *   - a human-readable ANSI/control notation
 */
public enum ShellCommand {

    // ── Single-character editing ─────────────────────────────────────────
    BACKSPACE   (KeyCode.BACK_SPACE, new byte[]{0x08},          "^H",  "Backspace — move cursor back (BS)"),
    SPACE       (KeyCode.SPACE,      new byte[]{0x20},          "SP",  "Space character"),
    DELETE      (KeyCode.DELETE,     new byte[]{0x7F},          "^?",  "Delete forward (DEL)"),
    KILL_WORD   (KeyCode.W,          new byte[]{0x17},          "^W",  "Erase previous word (ETB / werase)"),
    KILL_LINE   (KeyCode.U,          new byte[]{0x15},          "^U",  "Erase whole input line (NAK / kill)"),

    // ── Input / session control ──────────────────────────────────────────
    INTERRUPT   (KeyCode.C,          new byte[]{0x03},          "^C",  "SIGINT (ETX)"),
    END_OF_FILE (KeyCode.D,          new byte[]{0x04},          "^D",  "EOF (EOT)"),
    SUSPEND     (KeyCode.Z,          new byte[]{0x1A},          "^Z",  "SIGTSTP (SUB)"),
    QUIT        (KeyCode.BACK_SLASH, new byte[]{0x1C},          "^\\", "SIGQUIT (FS)"),
    FLUSH       (KeyCode.Q,          new byte[]{0x11},          "^Q",  "XON — resume output"),
    STOP        (KeyCode.S,          new byte[]{0x13},          "^S",  "XOFF — pause output"),

    // ── Line / screen ANSI escape sequences ──────────────────────────────
    ERASE_TO_END   (null, "\033[0K", "ESC[0K", "Erase cursor → end of line"),
    ERASE_TO_START (null, "\033[1K", "ESC[1K", "Erase start of line → cursor"),
    ERASE_LINE     (null, "\033[2K", "ESC[2K", "Erase entire line"),
    ERASE_SCREEN   (null, "\033[2J", "ESC[2J", "Erase entire screen"),
    CURSOR_HOME    (null, "\033[H",  "ESC[H",  "Move cursor to home (1,1)"),
    CARRIAGE_RETURN(null, "\r",      "CR",     "Move cursor to column 0"),

    // ── Cursor movement (ANSI) ───────────────────────────────────────────
    CURSOR_UP    (KeyCode.UP,    "\033[A", "ESC[A", "Cursor up"),
    CURSOR_DOWN  (KeyCode.DOWN,  "\033[B", "ESC[B", "Cursor down"),
    CURSOR_RIGHT (KeyCode.RIGHT, "\033[C", "ESC[C", "Cursor right"),
    CURSOR_LEFT  (KeyCode.LEFT,  "\033[D", "ESC[D", "Cursor left"),

    // ── Screen clear convenience ─────────────────────────────────────────
    CLEAR_SCREEN (KeyCode.L, "\033[2J\033[H", "ESC[2J ESC[H", "Clear screen + home");

    // ─────────────────────────────────────────────────────────────────────
    private final KeyCode keyCode;   // may be null for pure escape sequences
    private final byte[]  bytes;     // raw bytes to write to the process
    private final String  notation;  // "^U", "ESC[2K", ...
    private final String  description;

    ShellCommand(KeyCode keyCode, byte[] bytes, String notation, String description) {
        this.keyCode = keyCode;
        this.bytes = bytes;
        this.notation = notation;
        this.description = description;
    }

    ShellCommand(KeyCode keyCode, String ansi, String notation, String description) {
        this(keyCode, ansi.getBytes(java.nio.charset.StandardCharsets.UTF_8), notation, description);
    }

    public KeyCode getKeyCode()    { return keyCode; }
    public byte[]  getBytes()      { return bytes.clone(); }
    public String  getNotation()   { return notation; }
    public String  getDescription(){ return description; }

    /** True if this command carries a single ASCII control byte. */
    public boolean isControlChar() { return bytes.length == 1 && bytes[0] < 0x20 || bytes[0] == 0x7F; }

    /** True if this command is an ANSI escape sequence. */
    public boolean isAnsi()        { return bytes.length > 1 && bytes[0] == 0x1B; }

    /** Look up a command by its JavaFX KeyCode (returns first match, or null). */
    public static ShellCommand fromKeyCode(KeyCode code) {
        for (ShellCommand c : values()) {
            if (c.keyCode == code) return c;
        }
        return null;
    }

    /** Look up a command by its notation, e.g. "^U" or "ESC[2K". */
    public static ShellCommand fromNotation(String notation) {
        for (ShellCommand c : values()) {
            if (c.notation.equals(notation)) return c;
        }
        return null;
    }
}
