module com.example.escapeGame {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.json;
    requires com.google.gson;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;


    opens com.example.escapeGame to javafx.fxml, com.google.gson;

    exports com.example.escapeGame;
}