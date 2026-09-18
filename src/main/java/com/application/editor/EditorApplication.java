package com.application.editor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class EditorApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(EditorApplication.class.getResource("editor-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1020, 600);
        stage.setTitle("EditorFx");
        stage.setScene(scene);
        stage.show();
    }
}