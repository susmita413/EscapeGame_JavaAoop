package com.example.escapeGame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
 import javafx.stage.Stage;

import java.io.IOException;

public class LogIn extends Application {
    private static Stage stg;
    // Store the currently logged-in username for access across controllers
    private static String loggedInUser;
    @Override
    public void start(Stage primaryStage) throws Exception {
        stg= primaryStage;
        primaryStage.setResizable(false); //user cannot change window size
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        primaryStage.setTitle("Log IN");
        primaryStage.setScene(new Scene(root,680,400));
        primaryStage.show();
    }

    public static void changeScene(String fxml, String title) throws IOException {
        System.out.println("Attempting to load FXML: " + fxml);
        
        // Ensure FXML path is correct
        String fxmlPath = fxml;
        if (!fxml.startsWith("/")) {
            fxmlPath = "/" + fxml;
        }
        
        // Get the resource URL first
        java.net.URL resourceUrl = LogIn.class.getResource(fxmlPath);
        if (resourceUrl == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }
        
        // Use the simple and reliable approach
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(resourceUrl);
        Parent root = loader.load();
        
        // Use the stored stage reference instead of searching for windows
        if (stg != null) {
            stg.setTitle(title);
            stg.setScene(new Scene(root));
            stg.show();
        } else {
            // Fallback to the window search method
            Stage stage = (Stage) Stage.getWindows().filtered(window -> window.isShowing()).get(0);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        }
    }

    // Getter for the logged-in username
    public static String getLoggedInUser() {
        return loggedInUser;
    }

    // Setter for the logged-in username
    public static void setLoggedInUser(String username) {
        loggedInUser = username;
    }



    public static void main(String[] args){
        launch(args);
    }
}
