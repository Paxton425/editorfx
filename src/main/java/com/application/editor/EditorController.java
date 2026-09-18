package com.application.editor;

import com.application.editor.adapter.AceEditorAdapter;
import com.application.editor.model.EditorDocument;
import com.application.editor.service.FileService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class EditorController {

    @FXML private TabPane editorTabPane;
    @FXML private Label statusBarText;

    private final FileService fileService = new FileService(); // Reusable service instance
    private static final Logger logger = LoggerFactory.getLogger(EditorController.class);

    public record TabContext(EditorDocument document, AceEditorAdapter editorAdapter) {}

    @FXML
    public void handleUndo(ActionEvent event) {
        AceEditorAdapter adapter = getActiveAdapter();
        if (adapter != null) {
            adapter.undo();
        }
    }

    @FXML
    public void handleRedo(ActionEvent event) {
        AceEditorAdapter adapter = getActiveAdapter();
        if (adapter != null) {
            adapter.redo();
        }
    }

    @FXML
    public void handleOpenFiles(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            String content = fileService.readFile(selectedFile);
            if (content != null) {
                EditorDocument doc = new EditorDocument(selectedFile.getName(), content);
                doc.setFilePath(selectedFile.toPath());

                Tab tab = createTab(doc);
                editorTabPane.getTabs().add(tab);
                editorTabPane.getSelectionModel().select(tab);

                statusBarText.setText("Opened " + selectedFile.getName());
            } else {
                statusBarText.setText("Error reading file: " + selectedFile.getName());
            }
        }
    }

    @FXML
    private void handleOpenProject(ActionEvent event) {

    }

    @FXML
    public void newFileHandler(ActionEvent event) {
        int tabCount = editorTabPane.getTabs().size() + 1;
        String title = "Untitled-" + tabCount + ".txt";

        EditorDocument doc = new EditorDocument(title, "");
        Tab tab = createTab(doc);

        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
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

        // Open Save Dialog if file has no existing path on disk
        if (targetFile == null) {
            Stage stage = (Stage) editorTabPane.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(doc.getTitle().replace("*", "").trim());
            targetFile = fileChooser.showSaveDialog(stage);

            if (targetFile == null) return; // User cancelled
        }

        // Delegate pure I/O to FileService
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

    private Tab createTab(EditorDocument doc) {
        Tab tab = new Tab(doc.getTitle());
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        AceEditorAdapter adapter = new AceEditorAdapter(webEngine);

        VBox container = new VBox(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        tab.setContent(container);

        // Check if the editor is loaded
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                AceEditorAdapter.EditorMode mode = AceEditorAdapter.EditorMode.fromFileName(doc.getTitle());
                adapter.setMode(mode);
                adapter.setText(doc.getContent());

                //Set Tab icon
                FontIcon tabIcon = new FontIcon();
                tabIcon.setIconLiteral("fab-java");
                tabIcon.setIconColor(Paint.valueOf(mode.getIconColor()));
                tabIcon.setIconSize(16);
                tab.setGraphic(tabIcon);
            }
        });

        webEngine.load(getClass().getResource("/editor.html").toExternalForm());
        tab.setUserData(new TabContext(doc, adapter));
        tab.setOnClosed(e -> webEngine.load(null));
        return tab;
    }

    private AceEditorAdapter getActiveAdapter() {
        Tab activeTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (activeTab == null) return null;
        TabContext context = (TabContext) activeTab.getUserData();
        return context != null ? context.editorAdapter() : null;
    }

    @FXML
    private void initialize() {
        // Starts the app with a fresh "Untitled-1.txt" tab
        newFileHandler(null);
    }
}