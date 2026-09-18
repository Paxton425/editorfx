module com.application.editor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires org.slf4j;

    requires pty4j;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.bootstrapfx.core;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires reload4j;

    opens com.application.editor to javafx.fxml;
    exports com.application.editor;
    exports com.application.editor.model;
    opens com.application.editor.model to javafx.fxml;
    exports com.application.editor.adapter;
    opens com.application.editor.adapter to javafx.fxml;
    exports com.application.editor.controller;
    opens com.application.editor.controller to javafx.fxml;
}