package com.example.escapeGame;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AfterLoginController implements Initializable {
    @FXML
    private Button logout;
    @FXML
    private Label usernameLabel;

    public void userLogOut(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/login.fxml","Log IN");
    }

    @FXML
    private void goToProfile(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/profile.fxml", "Profile"); // Or use your application's scene change method
    }

    @FXML
    private void startGame(javafx.event.ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/room.fxml", "Choose Your Room");
    }

    @FXML
    private void exitGame(javafx.event.ActionEvent event) {
        // This will close the application safely
        javafx.application.Platform.exit();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Get the logged-in user's username and display first 3 characters
        String currentUser = LogIn.getLoggedInUser();
        if (currentUser != null && currentUser.length() >= 3) {
            usernameLabel.setText(currentUser.substring(0, 3).toUpperCase());
        } else if (currentUser != null && !currentUser.isEmpty()) {
            // If username is shorter than 3 characters, just use what's available
            usernameLabel.setText(currentUser.toUpperCase());
        }
    }
}
