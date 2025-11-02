package com.example.escapeGame;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;

public class RoomController {
    
    @FXML
    private Button riddleChamberButton;
    
    @FXML
    private Button programmingLabButton;
    
    @FXML
    private Button mathQuizButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private void selectRiddleChamber(ActionEvent event) throws IOException {
        System.out.println("Riddle Chamber selected");
        // Navigate to level selection with room context
        navigateToLevel("Riddle Chamber");
    }
    
    @FXML
    private void selectProgrammingLab(ActionEvent event) throws IOException {
        System.out.println("Programming Lab selected");
        // Navigate to level selection with room context
        navigateToLevel("Programming Lab");
    }
    
    @FXML
    private void selectMathQuiz(ActionEvent event) throws IOException {
        System.out.println("Math Quiz selected");
        // Navigate to level selection with room context
        navigateToLevel("Math Quiz");
    }
    
    @FXML
    private void goBack(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/afterLogin.fxml", "After Login");
    }
    
    @FXML
    private void openLeaderboard(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/leaderboard.fxml", "Leaderboard");
    }
    
    private void navigateToLevel(String selectedRoom) throws IOException {
        // Store the selected room in Session for later use
        Session.getInstance().setSelectedRoom(selectedRoom);
        LogIn.changeScene("com/example/escapeGame/level.fxml", "Choose Level - " + selectedRoom);
    }
}
