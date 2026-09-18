package com.application.editor.service;

import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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

    // Folders that should never be shown in the explorer
    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", ".idea", ".vscode", "node_modules", "target",
            "build", "out", "bin", ".gradle", ".mvn", "dist", "__pycache__"
    );

    /**
     * Metadata (icon literal + color) for each known file type.
     * Colors are picked to match each language's official branding where sensible.
     */
    public enum FileType {
        JAVA        ("mdi2l-language-java",        "#e76f00"),
        KOTLIN      ("mdi2l-language-kotlin",      "#7f52ff"),
        PYTHON      ("mdi2l-language-python",      "#3776ab"),
        JAVASCRIPT  ("mdi2l-language-javascript",  "#f7df1e"),
        TYPESCRIPT  ("mdi2l-language-typescript",  "#3178c6"),
        HTML        ("mdi2l-language-html5",       "#e34f26"),
        CSS         ("mdi2l-language-css3",        "#2965f1"),
        XML         ("mdi2f-file-xml-box",         "#e37933"),
        JSON        ("mdi2c-code-json",            "#cbcb41"),
        MARKDOWN    ("mdi2l-language-markdown",    "#519aba"),
        TEXT        ("mdi2f-file-document-outline","#a9b1bd"),
        PROPERTIES  ("mdi2f-file-cog-outline",     "#8a8a8a"),
        YAML        ("mdi2f-file-cog-outline",     "#cb171e"),
        GRADLE      ("mdi2e-elephant",             "#02303a"),
        SQL         ("mdi2d-database",             "#e38c00"),
        SHELL       ("mdi2c-console-line",         "#4eaa25"),
        IMAGE       ("mdi2f-file-image-outline",   "#26a69a"),
        SVG         ("mdi2f-svg",                  "#ffb13b"),
        DEFAULT     ("mdi2f-file-outline",         "#9aa0a6");

        private final String iconLiteral;
        private final String colorHex;

        FileType(String iconLiteral, String colorHex) {
            this.iconLiteral = iconLiteral;
            this.colorHex = colorHex;
        }

        public String iconLiteral() { return iconLiteral; }
        public Color color()        { return Color.web(colorHex); }
    }

    // Extension -> FileType mapping
    private static final Map<String, FileType> EXTENSION_MAP = Map.ofEntries(
            Map.entry("java",       FileType.JAVA),
            Map.entry("kt",         FileType.KOTLIN),
            Map.entry("kts",        FileType.KOTLIN),
            Map.entry("py",         FileType.PYTHON),
            Map.entry("js",         FileType.JAVASCRIPT),
            Map.entry("mjs",        FileType.JAVASCRIPT),
            Map.entry("cjs",        FileType.JAVASCRIPT),
            Map.entry("jsx",        FileType.JAVASCRIPT),
            Map.entry("ts",         FileType.TYPESCRIPT),
            Map.entry("tsx",        FileType.TYPESCRIPT),
            Map.entry("html",       FileType.HTML),
            Map.entry("htm",        FileType.HTML),
            Map.entry("css",        FileType.CSS),
            Map.entry("scss",       FileType.CSS),
            Map.entry("xml",        FileType.XML),
            Map.entry("json",       FileType.JSON),
            Map.entry("md",         FileType.MARKDOWN),
            Map.entry("markdown",   FileType.MARKDOWN),
            Map.entry("txt",        FileType.TEXT),
            Map.entry("log",        FileType.TEXT),
            Map.entry("properties", FileType.PROPERTIES),
            Map.entry("yml",        FileType.YAML),
            Map.entry("yaml",       FileType.YAML),
            Map.entry("gradle",     FileType.GRADLE),
            Map.entry("sql",        FileType.SQL),
            Map.entry("sh",         FileType.SHELL),
            Map.entry("bash",       FileType.SHELL),
            Map.entry("zsh",        FileType.SHELL),
            Map.entry("png",        FileType.IMAGE),
            Map.entry("jpg",        FileType.IMAGE),
            Map.entry("jpeg",       FileType.IMAGE),
            Map.entry("gif",        FileType.IMAGE),
            Map.entry("bmp",        FileType.IMAGE),
            Map.entry("webp",       FileType.IMAGE),
            Map.entry("svg",        FileType.SVG)
    );

    private static final String FOLDER_ICON_CLOSED = "mdi2f-folder";
    private static final String FOLDER_ICON_OPEN   = "mdi2f-folder-open";
    private static final String FOLDER_COLOR_HEX   = "#dcb67a"; // VS Code-ish folder yellow

    /**
     * Recursively creates a TreeItem for the given file/folder.
     * Children of sub-folders are added lazily on first expansion
     * to keep large projects fast.
     */
    public TreeItem<Path> createTreeNode(Path filePath) {
        // 1. A TreeItem<Path> MUST hold a Path object as its value, not a String name!
        TreeItem<Path> item = new TreeItem<>(filePath);

        // 2. Use Files.isDirectory(Path) from java.nio.file instead of path.isDirectory()
        if (Files.isDirectory(filePath)) {
            item.setGraphic(buildFolderIcon());

            // 3. The placeholder MUST match the TreeItem type (TreeItem<Path>)
            // We use the current filePath as a proxy placeholder node
            TreeItem<Path> placeholder = new TreeItem<>(filePath);
            item.getChildren().add(placeholder);

            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded) {
                    // Swap the folder icon to "open"
                    if (item.getGraphic() instanceof FontIcon fi) {
                        fi.setIconLiteral(FOLDER_ICON_OPEN);
                    }
                    // 4. Check if the placeholder is still there by inspecting the collection structure
                    if (item.getChildren().size() == 1 && item.getChildren().get(0) == placeholder) {
                        item.getChildren().clear();
                        loadChildren(filePath, item); // Pass the modern Path object
                    }
                } else if (item.getGraphic() instanceof FontIcon fi) {
                    fi.setIconLiteral(FOLDER_ICON_CLOSED);
                }
            });
        } else {
            // 5. Use filePath.getFileName().toString() to extract the plain name string for your icon filter
            String fileName = filePath.getFileName().toString();
            item.setGraphic(buildFileIcon(fileName));
        }

        return item;
    }
    /** Loads the sorted contents of a directory into the given parent TreeItem. */
    private void loadChildren(Path directory, TreeItem<Path> parent) {
        List<Path> visible = new ArrayList<>();
        // Use modern Files.list to read directory children safely
        try (Stream<Path> stream = Files.list(directory)) {
            stream.forEach(child -> {
                String name = child.getFileName().toString();

                // Skip ignored directories
                if (Files.isDirectory(child) && IGNORED_DIRS.contains(name)) {
                    return;
                }

                // Skip hidden file assets (e.g. system dotfiles like .git)
                try {
                    if (Files.isHidden(child)) {
                        return;
                    }
                } catch (IOException e) {
                    // Ignore parsing errors on individual locked files and keep moving
                }

                visible.add(child);
            });
        } catch (IOException e) {
            logger.error("Failed to read directory entries for: " + directory, e);
            return;
        }

        // Folders first, then files — each sorted alphabetically (case-insensitive)
        visible.sort(Comparator
                .comparing((Path p) -> !Files.isDirectory(p)) // False (directories) first, True (files) second
                .thenComparing(p -> p.getFileName().toString().toLowerCase()));

        // Instantiates modern nodes and mounts them into the lazy layout
        for (Path child : visible) {
            parent.getChildren().add(createTreeNode(child));
        }
    }

    public FontIcon buildFolderIcon() {
        FontIcon icon = new FontIcon(FOLDER_ICON_CLOSED);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(FOLDER_COLOR_HEX));
        return icon;
    }

    /** Resolves the FileType for a given file name based on its extension. */
    public FileType resolveFileType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot + 1).toLowerCase();
            return EXTENSION_MAP.getOrDefault(ext, FileType.DEFAULT);
        }
        return FileType.DEFAULT;
    }

    /** Builds an icon (literal + color) for the given file name. */
    public FontIcon buildFileIcon(String fileName) {
        FileType type = resolveFileType(fileName);
        FontIcon icon = new FontIcon(type.iconLiteral());
        icon.setIconSize(16);
        icon.setIconColor(type.color());
        return icon;
    }

    public void setOnFileClicked(Runnable runnable){
        runnable.run();

    }
}