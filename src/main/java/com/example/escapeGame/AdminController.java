package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.util.List;

public class AdminController {

    @FXML
    private Button viewUsersButton;
    @FXML
    private Button manageGameButton;
    @FXML
    private Button viewStatsButton;
    @FXML
    private Button logoutButton;
    @FXML
    private ListView<String> userListView;
    @FXML
    private TextArea statsTextArea;
    @FXML
    private Label welcomeLabel;

    @FXML
    private void viewUsers() {
        // Hide other components and show user list
        userListView.setVisible(true);
        statsTextArea.setVisible(false);
        welcomeLabel.setVisible(false);
        
        // Load users from the data file
        List<User> users = UserDataUtil.loadUsers();
        ObservableList<String> userNames = FXCollections.observableArrayList();
        
        for (User user : users) {
            String userInfo = String.format("Username: %s | Email: %s", 
                user.getUsername(), user.getEmail());
            userNames.add(userInfo);
        }
        
        userListView.setItems(userNames);
    }

    @FXML
    private void manageGame() {
        try {
            LogIn.changeScene("com/example/escapeGame/puzzle_manager.fxml", "Puzzle Manager");
        } catch (IOException e) {
            System.err.println("Error opening Puzzle Manager: " + e.getMessage());
        }
    }

    @FXML
    private void openLeaderboard() {
        try {
            LogIn.changeScene("com/example/escapeGame/leaderboard.fxml", "Leaderboard");
        } catch (IOException e) {
            System.err.println("Error opening Leaderboard: " + e.getMessage());
        }
    }

    @FXML
    private void viewStats() {
        // Hide other components and show stats
        userListView.setVisible(false);
        welcomeLabel.setVisible(false);
        statsTextArea.setVisible(true);
        
        // Generate basic stats
        List<User> users = UserDataUtil.loadUsers();
        StringBuilder stats = new StringBuilder();
        stats.append("=== GAME STATISTICS ===\n\n");
        stats.append("Total Users: ").append(users.size()).append("\n\n");
        
        // Count admin vs regular users
        int adminCount = 0;
        for (User user : users) {
            if ("admin".equals(user.getRole())) {
                adminCount++;
            }
        }
        
        stats.append("Admin Users: ").append(adminCount).append("\n");
        stats.append("Regular Users: ").append(users.size() - adminCount).append("\n\n");
        stats.append("=== RECENT ACTIVITY ===\n");
        stats.append("No activity data available yet.\n\n");
        stats.append("=== SYSTEM INFO ===\n");
        stats.append("Application Version: 1.0\n");
        stats.append("Database: JSON File System\n");
        
        statsTextArea.setText(stats.toString());
    }

    @FXML
    private void logout() {
        try {
            LogIn.changeScene("com/example/escapeGame/login.fxml", "Login");
        } catch (IOException e) {
            System.err.println("Error switching to login scene: " + e.getMessage());
        }
    }
}
