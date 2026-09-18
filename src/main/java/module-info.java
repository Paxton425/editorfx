module com.application.editor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.slf4j;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.bootstrapfx.core;

    opens com.application.editor to javafx.fxml;
    exports com.application.editor;
    exports com.application.editor.model;
    opens com.application.editor.model to javafx.fxml;
    exports com.application.editor.adapter;
    opens com.application.editor.adapter to javafx.fxml;
}