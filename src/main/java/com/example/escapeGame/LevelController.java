package com.example.escapeGame;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class LevelController {
    
    @FXML
    private Button easyButton;
    
    @FXML
    private Button mediumButton;
    
    @FXML
    private Button hardButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label roomTitleLabel;
    
    @FXML
    private void initialize() {
        // Display the selected room in the title
        String selectedRoom = Session.getInstance().getSelectedRoom();
        if (selectedRoom != null) {
            roomTitleLabel.setText("Choose Level - " + selectedRoom);
            
            // Enable/disable level buttons based on completion status
            updateLevelButtons(selectedRoom);
        }
    }
    
    private void updateLevelButtons(String room) {
        // Easy level is always enabled
        
        // Check and update Medium button
        boolean mediumUnlocked = Session.isLevelUnlocked(room, "Medium");
        mediumButton.setDisable(!mediumUnlocked);
        if (!mediumUnlocked) {
            mediumButton.setTooltip(new javafx.scene.control.Tooltip("Complete Easy level first"));
        }
        
        // Check and update Hard button
        boolean hardUnlocked = Session.isLevelUnlocked(room, "Hard");
        hardButton.setDisable(!hardUnlocked);
        if (!hardUnlocked) {
            hardButton.setTooltip(new javafx.scene.control.Tooltip("Complete Medium level first"));
        }
    }
    
    @FXML
    private void selectEasy(ActionEvent event) throws IOException {
        String room = Session.getInstance().getSelectedRoom();
        System.out.println("Easy level selected for room: " + room);
        Session.getInstance().setSelectedDifficulty("Easy");
        navigateToGame("Easy");
    }
    
    @FXML
    private void selectMedium(ActionEvent event) throws IOException {
        String room = Session.getInstance().getSelectedRoom();
        if (Session.isLevelUnlocked(room, "Medium")) {
            System.out.println("Medium level selected for room: " + room);
            Session.getInstance().setSelectedDifficulty("Medium");
            navigateToGame("Medium");
        } else {
            // This should not happen as the button should be disabled
            System.out.println("Access denied: Medium level not unlocked yet");
        }
    }
    
    @FXML
    private void selectHard(ActionEvent event) throws IOException {
        String room = Session.getInstance().getSelectedRoom();
        if (Session.isLevelUnlocked(room, "Hard")) {
            System.out.println("Hard level selected for room: " + room);
            Session.getInstance().setSelectedDifficulty("Hard");
            navigateToGame("Hard");
        } else {
            // This should not happen as the button should be disabled
            System.out.println("Access denied: Hard level not unlocked yet");
        }
    }
    
    @FXML
    private void goBack(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/room.fxml", "Choose Your Room");
    }
    
    private void navigateToGame(String difficulty) throws IOException {
        String selectedRoom = Session.getInstance().getSelectedRoom();
        String gameTitle = difficulty + " Level - " + selectedRoom;
        LogIn.changeScene("/com/example/escapeGame/mode.fxml", gameTitle);
    }
}

