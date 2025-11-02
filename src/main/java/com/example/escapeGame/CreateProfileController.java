package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CreateProfileController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button createButton;
    @FXML private Button backButton;

    @FXML
    private void handleCreateProfile() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("All fields required.");
            return;
        }

        if (UserDataUtil.isUsernameTaken(username)) {
            statusLabel.setText("Username already taken!");
            return;
        }

        User newUser = new User(username, password, email);
        UserDataUtil.addUser(newUser);

        statusLabel.setText("Profile created!");
        // Optionally: LogIn.changeScene("com/example/escapeGame/login.fxml", "Log IN");
    }


    @FXML
    private void handleBack() {
        try {
            LogIn.changeScene("com/example/escapeGame/login.fxml", "Log IN");
        } catch (java.io.IOException e) {
            statusLabel.setText("Error switching to login scene!");
            e.printStackTrace();
        }
    }

}
