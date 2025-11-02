package com.example.escapeGame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AfterLogin extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("afterLogin.fxml"));
        primaryStage.setTitle("Profile");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static Stage stg;
    public static void changeScene(String fxml) throws IOException {
        Parent pane = FXMLLoader.load(LogIn.class.getResource(fxml));
        stg.getScene().setRoot(pane);
    }
    
}
