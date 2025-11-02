package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;

public class ProfileController {
    @FXML private Label usernameLabel;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private Label statusLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Pane avatarPane;
    @FXML private Button changeAvatarButton;

    private User currentUser;

    // Session user - should be set when user logs in
    public static String sessionUsername = "susmita"; // Default to susmita for testing

    @FXML
    public void initialize() {
        currentUser = UserDataUtil.getUserByUsername(sessionUsername);
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());
            passwordField.setText(currentUser.getPassword()); // Show current password
            loadAvatar();
        } else {
            statusLabel.setText("User not found.");
        }
    }

    @FXML
    private void handleSave() {
        String newEmail = emailField.getText();
        String newPassword = passwordField.getText();
        if (newEmail.isEmpty()) {
            statusLabel.setText("Email cannot be empty.");
            return;
        }
        currentUser.setEmail(newEmail);
        if (!newPassword.isEmpty()) {
            currentUser.setPassword(newPassword);
        }
        UserDataUtil.updateUser(currentUser);
        statusLabel.setText("Profile updated successfully!");
        // Keep the password visible after saving
    }

    @FXML
    private void handleBack() {
        try {
            LogIn.changeScene("com/example/escapeGame/afterLogin.fxml", "After Login");
        } catch (IOException e) {
            statusLabel.setText("Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangeAvatar() {
        showAvatarSelectionDialog();
    }

    private void loadAvatar() {
        if (currentUser != null && currentUser.getAvatar() != null) {
            String avatarPath = "/img/" + currentUser.getAvatar();
            try {
                Image avatarImage = new Image(getClass().getResourceAsStream(avatarPath));
                avatarImageView.setImage(avatarImage);
            } catch (Exception e) {
                // If avatar loading fails, use default
                loadDefaultAvatar();
            }
        } else {
            loadDefaultAvatar();
        }
    }

    private void loadDefaultAvatar() {
        try {
            Image defaultAvatar = new Image(getClass().getResourceAsStream("/img/boy_1.png"));
            avatarImageView.setImage(defaultAvatar);
        } catch (Exception e) {
            statusLabel.setText("Could not load default avatar");
        }
    }

    private void showAvatarSelectionDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Select Avatar");
        alert.setHeaderText("Choose your avatar");

        String[] avatarOptions = {"girl_1.png", "girl_2.png", "boy_1.png", "boy_2.png", "cat.png"};
        String[] displayNames = {"Girl 1", "Girl 2", "Boy 1", "Boy 2", "Cat"};
        
        ButtonType[] buttons = new ButtonType[avatarOptions.length + 1];
        
        for (int i = 0; i < avatarOptions.length; i++) {
            buttons[i] = new ButtonType(displayNames[i]);
        }
        buttons[avatarOptions.length] = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(buttons);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            for (int i = 0; i < avatarOptions.length; i++) {
                if (result.get() == buttons[i]) {
                    currentUser.setAvatar(avatarOptions[i]);
                    loadAvatar();
                    UserDataUtil.updateUser(currentUser);
                    statusLabel.setText("Avatar updated successfully!");
                    break;
                }
            }
        }
    }
}

