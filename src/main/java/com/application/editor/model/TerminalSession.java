package com.application.editor.model;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TerminalSession implements AutoCloseable {

    private final PtyProcess process;
    private final OutputStream input;
    private final InputStream output;

    public TerminalSession() throws IOException {

        String[] command = {
                "powershell.exe",
                "-NoLogo"
        };
        Path workingDirectory = Paths.get(System.getProperty("user.home"));

        Map<String, String> environment =
                new HashMap<>(System.getenv());

        environment.put("TERM", "xterm");

        this.process = new PtyProcessBuilder()
                .setCommand(command)
                .setDirectory(workingDirectory.toString())
                .setEnvironment(environment)
                .setInitialColumns(120)
                .setInitialRows(30)
                .start();

        this.input = process.getOutputStream();
        this.output = process.getInputStream();
    }

    public void write(String input) throws IOException {
        this.input.write(input.getBytes(StandardCharsets.UTF_8));
        this.input.flush();
    }

    public void write(byte[] input) throws IOException {
        this.input.write(input);
        this.input.flush();
    }

    public InputStream getOutput() {
        return output;
    }

    public boolean isRunning() {
        return process.isAlive();
    }

    public void resize(int columns, int rows) {
        process.setWinSize(new WinSize(columns, rows));
    }

    @Override
    public void close() {
        if (process.isAlive()) {
            process.destroy();
        }
    }
}