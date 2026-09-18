package com.application.editor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public String readFile(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    public boolean saveFile(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.error("Failed to write file to path: {}", path, e);
            return false;
        }
    }
}