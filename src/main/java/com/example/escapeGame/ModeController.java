package com.example.escapeGame;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.io.IOException;

import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Button;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModeController {

    // Match fx:id values in mode.fxml to avoid unresolved fx:id warnings
    @FXML private Button multiplayerModeButton;
    @FXML private Button modeGoBack;

    @FXML
    private void goBack(ActionEvent event) throws IOException {
        LogIn.changeScene("com/example/escapeGame/level.fxml", "Choose Level");
    }

    @FXML
    private void modeGoBack(ActionEvent event) throws IOException {
        Parent levelRoot = FXMLLoader.load(getClass().getResource("/com/example/escapeGame/level.fxml"));
        Scene levelScene = new Scene(levelRoot);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(levelScene);
    }

    @FXML
    private void goToMultiplayerMode(ActionEvent event) throws IOException {
        Parent multiplayerRoot = FXMLLoader.load(getClass().getResource("/com/example/escapeGame/multiplayerRoom.fxml"));
        Scene multiplayerScene = new Scene(multiplayerRoot);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(multiplayerScene);
    }

    @FXML
    private void goToSoloMode(ActionEvent event) throws IOException {
        // Ensure we are NOT in network mode
        Session.getInstance().setNetClient(null);

        User user = Session.getCurrentUser();
        if (user == null) {
            LogIn.changeScene("com/example/escapeGame/login.fxml", "Login");
            return;
        }

        String username = user.getUsername();
        String room = Session.getInstance().getSelectedRoom();
        String difficulty = Session.getInstance().getSelectedDifficulty();
        if (room == null || difficulty == null) {
            // Go back to level selection if context is missing
            LogIn.changeScene("/com/example/escapeGame/level.fxml", "Choose Level");
            return;
        }

        // Create a local room and join as the only player
        String code = RoomManager.getInstance().createRoom(username);
        RoomManager.getInstance().joinRoom(code, username);

        // Load and select questions similar to server logic
        List<Puzzle> all = PuzzleDataUtil.loadPuzzles();
        List<Puzzle> filtered = new ArrayList<>();
        for (Puzzle p : all) {
            if (room.equals(p.getRoom()) && difficulty.equals(p.getDifficulty())) {
                filtered.add(p);
            }
        }
        if (filtered.isEmpty()) {
            for (Puzzle p : all) {
                if (difficulty.equals(p.getDifficulty())) filtered.add(p);
            }
        }
        if (filtered.isEmpty()) {
            filtered.addAll(all);
        }
        Collections.shuffle(filtered);
        if (filtered.size() > 10) filtered = new ArrayList<>(filtered.subList(0, 10));

        ClientRoom localRoom = RoomManager.getInstance().getRoom(code);
        if (localRoom != null) {
            localRoom.setSelectedQuestions(filtered);
        }

        // Save room code in session and start the local game
        Session.getInstance().setRoomCode(code);
        RoomManager.getInstance().startGame(code, username);

        // Navigate straight to competitive game scene (single player)
        LogIn.changeScene("com/example/escapeGame/competitiveGame.fxml", "Competitive Game");
    }

}
