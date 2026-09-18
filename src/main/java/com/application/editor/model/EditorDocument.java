package com.application.editor.model;

import java.nio.file.Path;

public class EditorDocument {
    private String title;
    private String content;
    private Path filePath;
    private boolean dirty;

    public EditorDocument(String title, String content) {
        this.title = title;
        this.content = content;
        this.dirty = false;
    }

    // Standard Getters / Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
}