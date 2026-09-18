package com.application.editor.controller;

import com.application.editor.adapter.AceEditorAdapter;
import com.application.editor.adapter.XtermTerminalAdapter;
import com.application.editor.model.EditorDocument;
import com.application.editor.model.EditorMode;
import com.application.editor.model.TerminalSession;
import com.application.editor.model.Theme;
import com.application.editor.service.FileService;
import com.application.editor.service.ThemeService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class EditorController {

    @FXML private BorderPane rootBorderPane;
    @FXML private TreeView<Path> fileTreeView;
    @FXML private TabPane editorTabPane;
    @FXML private Label statusBarText;
    @FXML private Label cursorPositionStatusLabel;
    @FXML private VBox consolePanel;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane consoleSplitPane;
    @FXML private WebView terminalWebView;

    private double lastDividerPosition = 0.75;
    private boolean consoleColapsed = true;

    // Services
    private final ThemeService themeService = new ThemeService();
    private final FileService fileService = new FileService();
    private XtermTerminalAdapter xtermTerminalAdapter;
    private static final Logger logger = LoggerFactory.getLogger(EditorController.class);

    public record TabContext(EditorDocument document, AceEditorAdapter editorAdapter) {}


    @FXML
    public void handleUndo(ActionEvent event) {
        AceEditorAdapter adapter = getActiveAdapter();
        if (adapter != null) adapter.undo();
    }

    @FXML
    public void handleRedo(ActionEvent event) {
        AceEditorAdapter adapter = getActiveAdapter();
        if (adapter != null) adapter.redo();
    }

    @FXML
    public void handleOpenFiles(ActionEvent event) {
        Stage stage = (Stage) rootBorderPane.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        openFileToTab(selectedFile);
    }

    private void openFileToTab(File file) {
        if (file != null) {
            String content = fileService.readFile(file);
            if (content != null) {
                EditorDocument doc = new EditorDocument(file.getName(), content, false);
                doc.setFilePath(file.toPath());

                Tab tab = createTab(doc);
                editorTabPane.getTabs().add(tab);
                editorTabPane.getSelectionModel().select(tab);

                statusBarText.setText("Opened " + file.getName());
            } else {
                statusBarText.setText("Error reading file: " + file.getName());
            }
        }
    }

    @FXML
    public void newFileHandler(ActionEvent event) {
        int tabCount = editorTabPane.getTabs().size() + 1;
        String title = "Untitled-" + tabCount + ".txt";

        EditorDocument doc = new EditorDocument(title, "", true);
        Tab tab = createTab(doc);

        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void handleOpenProject(ActionEvent event) {
        Stage stage = (Stage) rootBorderPane.getScene().getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Project Directory");

        File selectedFile = directoryChooser.showDialog(stage);

        try {
            if (selectedFile != null) {
                Path selectedDirectory = selectedFile.toPath();
                TreeItem<Path> rootItem = fileService.createTreeNode(selectedDirectory);
                rootItem.setExpanded(true);
                fileTreeView.setRoot(rootItem);
                fileTreeView.setShowRoot(true);

                logger.info("Opened project directory: {}", selectedDirectory);
                statusBarText.setText("Opened folder: " + selectedDirectory.getFileName().toString());
            }
        } catch (Exception e){
            logger.error("Error Opening Folder", e);
        }
    }

    @FXML
    private void onTreeItemContextMenuRequested(){
        TreeItem<Path> item = fileTreeView.getSelectionModel().getSelectedItem();
        logger.info("Selected Item {}", item.getValue());
    }

    @FXML
    private void handleClearConsole() {
        if (xtermTerminalAdapter != null)
            xtermTerminalAdapter.clearConsole();
    }

    @FXML
    private void handleToggleCollapseConsole(ActionEvent event) {
        // Grabs the nested SplitPane containing the editor and console
        if(consoleSplitPane.getDividerPositions().length > 0) {
            Button toggleButton = (Button) event.getSource();
            double currentDividerPosition = consoleSplitPane.getDividerPositions()[0];
            logger.info("cdp {}, ldp {}", currentDividerPosition, lastDividerPosition);
            if(currentDividerPosition < 0.9) {
                consoleSplitPane.setDividerPositions(1.0); // Collapse console downward
                lastDividerPosition = currentDividerPosition;
                toggleButton.setText("▴");
            }
            else {
                consoleSplitPane.setDividerPositions(lastDividerPosition);
                toggleButton.setText("▾");
            }
        }
    }

    @FXML
    private void handleCloseConsole() {
        if (consolePanel == null) return;
        consoleSplitPane.getItems().remove(consolePanel);
    }

    @FXML
    private void handleFileSave() {
        Tab activeTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (activeTab == null) return;

        TabContext context = (TabContext) activeTab.getUserData();
        EditorDocument doc = context.document();
        AceEditorAdapter adapter = context.editorAdapter();

        String currentCode = adapter.getText();
        File targetFile = doc.getFilePath() != null ? doc.getFilePath().toFile() : null;

        if (targetFile == null) {
            Stage stage = (Stage) rootBorderPane.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(doc.getTitle().replace("*", "").trim());
            targetFile = fileChooser.showSaveDialog(stage);

            if (targetFile == null) return;
        }

        boolean success = fileService.saveFile(targetFile.toPath(), currentCode);

        if (success) {
            doc.setFilePath(targetFile.toPath());
            doc.setTitle(targetFile.getName());
            doc.setContent(currentCode);
            doc.setDirty(false);

            activeTab.setText(targetFile.getName());
            statusBarText.setText("Saved " + targetFile.getAbsolutePath());
        } else {
            statusBarText.setText("Error saving " + targetFile.getName());
        }
    }

    @FXML
    private void handleThemeChange(ActionEvent event) {
        if (event.getSource() instanceof MenuItem menuItem) { //Selected from menu
            String text = menuItem.getText();
            Theme selectedTheme = Theme.fromName(text);

            // ThemeService handles both CSS stylesheet clearing/adding and AceJS sync
            //themeService.applyTheme(selectedTheme, rootBorderPane, getActiveAdapter());
            applyTheme(selectedTheme);
        }
    }

    protected void applyTheme(Theme theme){
        //Set Global Theme State + runnable
        themeService.setCurrentTheme(theme, ()->{
            editorTabPane.getTabs().stream().map(t->{
                logger.info("TAB: {}", t.getText());
                TabContext context = (TabContext) t.getUserData();
                // Sync Ace Editor theme if adapter is present
                if(context != null){
                    if (context.editorAdapter() != null) {
                        logger.info("ADAPTER: {}", context.editorAdapter());
                        context.editorAdapter().setAceTheme(theme.getAceTheme());
                    }
                }
                return t;
            }).collect(Collectors.toUnmodifiableList());

            // Clear existing stylesheets to avoid CSS stacking
            rootBorderPane.getStylesheets().clear();

            if (theme != Theme.NATIVE && !theme.getStylesheetPath().isEmpty()) {
                var resource = getClass().getResource(theme.getStylesheetPath());
                if (resource != null) {
                    rootBorderPane.getStylesheets().add(resource.toExternalForm());
                } else {
                    logger.warn("Stylesheet path not found: {}", theme.getStylesheetPath());
                }
            }
        });
    }

    private Tab createTab(EditorDocument doc) {
        String tabTitle = (doc.isDirty())? doc.getTitle()+"*" : doc.getTitle();
        Tab tab = new Tab(tabTitle);
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        AceEditorAdapter adapter = new AceEditorAdapter(webEngine);

        VBox container = new VBox(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        tab.setContent(container);
        tab.setOnCloseRequest(event -> {
            logger.info("Tab Close Request.");
            if (doc.isDirty()) {
                TabContext context = (TabContext) tab.getUserData();
                AceEditorAdapter eventAdapter = context.editorAdapter();

                // Fetch current live text from the WebEngine JavaScript runtime
                String currentCode = eventAdapter != null ? eventAdapter.getText() : "";

                if (!currentCode.trim().isEmpty()) {
                    if (!confirmProcess("Discard Changes?")) {
                        event.consume(); // Cancels tab close if user clicks No/Cancel
                    }
                }
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                EditorMode mode = EditorMode.fromFileName(doc.getTitle());
                adapter.setAceMode(mode);
                adapter.setText(doc.getContent());

                // Enforce active theme from ThemeService on load
                adapter.setAceTheme(themeService.getCurrentTheme().getAceTheme());

                // Set Tab icon
                FontIcon tabIcon = new FontIcon();
                tabIcon.setIconLiteral(mode.getIconLiteral());
                tabIcon.setIconColor(Paint.valueOf(mode.getIconColor()));
                tabIcon.setIconSize(16);
                tab.setGraphic(tabIcon);

                // Register JS -> Java callback bridge
                adapter.setupJavaBridge();
                adapter.setOnContentChangeListener(() -> markTabDirty(tab, doc));
                adapter.setOnCursorPositionChangeListener(() -> {
                    int[] positions = adapter.getCursorPositions();
                    cursorPositionStatusLabel.setText("Ln "+positions[0]+", Col "+positions[1]);
                });
            }
        });

        webEngine.load(getClass().getResource("/ace-editor/editor.html").toExternalForm());
        tab.setUserData(new TabContext(doc, adapter));
        tab.setOnClosed(e -> webEngine.load(null));
        return tab;
    }

    private void markTabDirty(Tab tab, EditorDocument doc) {
        if (!doc.isDirty()) {
            doc.setDirty(true);
            tab.setText(doc.getTitle() + "*");
        }
    }

    private AceEditorAdapter getActiveAdapter() {
        Tab activeTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (activeTab == null) return null;
        TabContext context = (TabContext) activeTab.getUserData();
        return context != null ? context.editorAdapter() : null;
    }

    private boolean confirmProcess(String message){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText(message);

        // Styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().clear();
        dialogPane.getScene().setFill(null); //Clear default dialog backgrounds

        URL stylesheet = getClass().getResource("/com/css/alerts-dark.css");
        if(stylesheet == null)
            logger.error("Dialog Stylesheet Resource URL Invalid!");
        else
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());

        Stage stage = (Stage) dialogPane.getScene().getWindow(); // Remove OS Window Title Bar / Border
        stage.initStyle(StageStyle.UNDECORATED); // Use TRANSPARENT for rounded corners, or UNDECORATED for flat borders

        // showAndWait() pauses execution right here until the user clicks
        Optional<ButtonType> result = alert.showAndWait();
        // Returns true if user clicked OK, false otherwise
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    @FXML
    private void initialize() {
        // Automatically sync background tabs to active theme when selected
        editorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            AceEditorAdapter adapter = getActiveAdapter();
            if (adapter != null) {
                applyTheme(themeService.getCurrentTheme());
            }
        });
        applyTheme(themeService.getCurrentTheme());

        consoleColapsed = (consoleSplitPane.getDividerPositions()[0] < 0.9)? false : true;
        rootBorderPane.heightProperty()
                .addListener((obs, oldHeight, newHeight) ->{
                    logger.info("collapsed? {}, dp {}", consoleColapsed, consoleSplitPane.getDividerPositions()[0]);
                    if(consoleColapsed) consoleSplitPane.setDividerPositions(1.0);
                });

        //New Terminal Session
        try {
            WebEngine webEngine = terminalWebView.getEngine();

            URL htmlFileUrl = getClass().getResource("/terminal/xterm.html");
            if (htmlFileUrl == null) {
                throw new IllegalStateException("xterm.html file not found in resources path /terminal/xterm.html");
            }

            webEngine.load(htmlFileUrl.toExternalForm());
            TerminalSession session = new TerminalSession();

            xtermTerminalAdapter = new XtermTerminalAdapter(webEngine, session);
            xtermTerminalAdapter.start();
        } catch (Exception e) {
            logger.error("An Error occurred during session initialization!", e);
        }

        // Listen for file selections in the Project Explorer
        fileTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double click check
                TreeItem<Path> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.isLeaf()) { // Check if it's a file, not a folder
                    Path filePath = selectedItem.getValue();
                    // Call file opening method here!
                    openFileToTab(filePath.toFile());
                    logger.info("Double clicked file: {}", filePath.getFileName());
                }
            }
        });

        fileTreeView.setCellFactory(tv -> new TreeCell<Path>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);

                // If the cell is empty or the path node doesn't exist, draw nothing
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // CRITICAL: Extract just the plain file/folder name string for the UI layout
                    setText(item.getFileName().toString());

                    // This preserves the graphic icons you set up in createTreeNode!
                    if (getTreeItem() != null) {
                        setGraphic(getTreeItem().getGraphic());
                    }
                }
            }
        });

    }
}