package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class InstructionController {

    @FXML private Label gameTitleLabel;
    @FXML private TextArea gameDescriptionArea;

    @FXML
    private void initialize() {
        try {
            if (gameTitleLabel != null) {
                gameTitleLabel.setText("Welcome to the Escape Room!");
            }
            if (gameDescriptionArea != null) {
                gameDescriptionArea.setText(buildInstructions());
            }
        } catch (Exception ignored) {}
    }

    private String buildInstructions() {
        StringBuilder sb = new StringBuilder();
        sb.append("GAME RULES & INSTRUCTIONS\n\n");

        sb.append("Progression\n");
        sb.append(" • Play Easy first, then Medium, then Hard (per room). Levels unlock in that order.\n\n");

        sb.append("Solo Mode\n");
        sb.append(" • Time per question: Easy 90s · Medium 120s · Hard 150s.\n");
        sb.append(" • Scoring: +10 for correct, -5 for wrong.\n");
        sb.append(" • Escape target: reach 50 points to escape.\n\n");

        sb.append("Multiplayer Mode\n");
        sb.append(" • Time per question: Easy 90s · Medium 120s · Hard 150s.\n");
        sb.append(" • Scoring by answer order: 1st +10, 2nd +8, 3rd +6, 4th+ +4; -5 for wrong.\n");
        sb.append(" • Team bonuses: +2 each if everyone answers correctly; +5 each if a correct answer arrives within 10s.\n");
        sb.append(" • Host sets players needed (2–4). Game auto-starts when room reaches that capacity.\n\n");

        sb.append("Crowns\n");
        sb.append(" • \uD83D\uDC51 One crown for each room you complete on Hard.\n");
        sb.append(" • \uD83D\uDC51\uD83D\uDC51\uD83D\uDC51 Triple-crown when you complete Hard in all rooms available.\n\n");

        sb.append("Tips\n");
        sb.append(" • Read carefully, think logically, and avoid penalties!\n");
        return sb.toString();
    }

    @FXML
    private void goBack() throws IOException {
        LogIn.changeScene("com/example/escapeGame/room.fxml", "Choose Your Room");
    }
}
