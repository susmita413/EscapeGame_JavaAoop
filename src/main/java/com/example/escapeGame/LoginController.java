package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.List;

public class LoginController {

    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;
    @FXML
    private Label wrongLogin;
    @FXML
    private Button createAccountButton;

    @FXML
    public void userLogin(ActionEvent event) throws IOException {
        String userText = username.getText().trim();
        String passText = password.getText().trim();

        if(userText.isEmpty() || passText.isEmpty()) {
            wrongLogin.setText("Please fill both fields");
            return;
        }

        // Use UserDataUtil to load users consistently
        List<User> users = UserDataUtil.loadUsers();
        
        boolean matched = false;
        boolean isAdmin = false;

        for(User user : users) {
            if(user.getUsername().equals(userText) && user.getPassword().equals(passText)) {
                matched = true;
                if("admin".equals(user.getRole())) {
                    isAdmin = true;
                }
                break;
            }
        }

        if(matched) {
            wrongLogin.setText("Success");
            // Find the user object and set it in session
            User loggedInUser = null;
            for(User user : users) {
                if(user.getUsername().equals(userText) && user.getPassword().equals(passText)) {
                    loggedInUser = user;
                    break;
                }
            }
            
            if(loggedInUser != null) {
                Session.setCurrentUser(loggedInUser);
            }
            // Also store plain username for other controllers that rely on LogIn
            LogIn.setLoggedInUser(userText);
            
            // Set the session username for the profile controller
            ProfileController.sessionUsername = userText;
            try {
                if(isAdmin)
                    LogIn.changeScene("com/example/escapeGame/adminPanel.fxml","Admin Panel");
                else
                    LogIn.changeScene("com/example/escapeGame/afterLogin.fxml","After Login");
            } catch (IOException e) {
                wrongLogin.setText("Error loading next screen: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            wrongLogin.setText("Wrong username or password");
        }
    }

    @FXML
    public void goToCreateAccount(ActionEvent event) throws IOException {
        try {
            LogIn.changeScene("com/example/escapeGame/createProfile.fxml", "Create Account");
        } catch (IOException e) {
            wrongLogin.setText("Error loading create account screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
