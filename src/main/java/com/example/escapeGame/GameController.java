package com.example.escapeGame;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.util.List;

public class GameController {

    @FXML
    private Button startGameButton;
    @FXML
    private Button instructionsButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Label gameTitleLabel;
    @FXML
    private TextArea gameDescriptionArea;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label statusLabel;

    private int score = 0;
    private String selectedRoom;
    private String selectedDifficulty;

    @FXML
    private void initialize() {
        // Get room and difficulty from session
        selectedRoom = Session.getInstance().getSelectedRoom();
        selectedDifficulty = Session.getInstance().getSelectedDifficulty();
        
        // Initialize game state based on selected room
        initializeRoomSpecificContent();
        updateScore();
        statusLabel.setText("Ready to play " + selectedRoom + "!");
    }

    private void initializeRoomSpecificContent() {
        if (selectedRoom == null) {
            selectedRoom = "Unknown Room";
        }
        
        switch (selectedRoom) {
            case "Riddle Chamber":
                gameTitleLabel.setText("Riddle Chamber - " + selectedDifficulty + " Level");
                gameDescriptionArea.setText(
                    "Welcome to the Riddle Chamber! This ancient room is filled with mysterious riddles and word puzzles.\n\n" +
                    "Your mission: Solve the riddles to unlock the chamber's secrets!\n\n" +
                    "Available actions:\n" +
                    "• Read ancient scrolls\n" +
                    "• Examine mysterious symbols\n" +
                    "• Solve word puzzles\n" +
                    "• Decipher cryptic messages\n\n" +
                    "Difficulty: " + selectedDifficulty + "\n" +
                    "Time limit: " + getTimeLimit() + " minutes"
                );
                break;
                
            case "Programming Lab":
                gameTitleLabel.setText("Programming Lab - " + selectedDifficulty + " Level");
                gameDescriptionArea.setText(
                    "Welcome to the Programming Lab! This high-tech room contains coding challenges and algorithmic puzzles.\n\n" +
                    "Your mission: Write code and solve programming problems to escape!\n\n" +
                    "Available actions:\n" +
                    "• Write code solutions\n" +
                    "• Debug programs\n" +
                    "• Solve algorithmic puzzles\n" +
                    "• Optimize performance\n\n" +
                    "Difficulty: " + selectedDifficulty + "\n" +
                    "Time limit: " + getTimeLimit() + " minutes"
                );
                break;
                
            case "Math Quiz":
                gameTitleLabel.setText("Math Quiz Room - " + selectedDifficulty + " Level");
                gameDescriptionArea.setText(
                    "Welcome to the Math Quiz Room! This room is filled with mathematical challenges and numerical puzzles.\n\n" +
                    "Your mission: Solve mathematical problems to unlock the exit!\n\n" +
                    "Available actions:\n" +
                    "• Solve equations\n" +
                    "• Calculate complex problems\n" +
                    "• Work with geometric shapes\n" +
                    "• Apply mathematical formulas\n\n" +
                    "Difficulty: " + selectedDifficulty + "\n" +
                    "Time limit: " + getTimeLimit() + " minutes"
                );
                break;
                
            default:
                gameTitleLabel.setText("Unknown Room - " + selectedDifficulty + " Level");
                gameDescriptionArea.setText("You find yourself in an unknown room. Explore and solve puzzles to escape!");
        }
    }
    
    private int getTimeLimit() {
        switch (selectedDifficulty) {
            case "Easy": return 15;
            case "Medium": return 10;
            case "Hard": return 5;
            default: return 10;
        }
    }

    @FXML
    private void startGame() {
        statusLabel.setText("Game starting... Good luck!");
        
        // Load room-specific puzzles
        loadRoomPuzzles();
        
        // Disable start button and enable game controls
        startGameButton.setDisable(true);
        startGameButton.setText("Game In Progress...");
        
        // TODO: Implement actual game logic here
        // This would typically involve:
        // - Loading different room scenes
        // - Managing inventory
        // - Handling puzzle interactions
        // - Tracking time and score
    }
    
    private void loadRoomPuzzles() {
        try {
            List<Puzzle> allPuzzles = PuzzleDataUtil.loadPuzzles();
            List<Puzzle> roomPuzzles = allPuzzles.stream()
                .filter(puzzle -> puzzle.getRoom().equals(selectedRoom))
                .filter(puzzle -> puzzle.getDifficulty().equals(selectedDifficulty))
                .toList();
            
            System.out.println("Loaded " + roomPuzzles.size() + " puzzles for " + selectedRoom + " (" + selectedDifficulty + ")");
            
            // TODO: Display first puzzle or puzzle selection interface
            if (!roomPuzzles.isEmpty()) {
                Puzzle firstPuzzle = roomPuzzles.get(0);
                gameDescriptionArea.setText(
                    gameDescriptionArea.getText() + "\n\n" +
                    "=== FIRST PUZZLE ===\n" +
                    "Question: " + firstPuzzle.getQuestion() + "\n" +
                    "Answer: [Hidden - solve to reveal]"
                );
            }
            
        } catch (Exception e) {
            System.err.println("Error loading puzzles: " + e.getMessage());
            statusLabel.setText("Error loading puzzles!");
        }
    }

    @FXML
    private void showInstructions() {
        statusLabel.setText("Showing instructions...");
        gameTitleLabel.setText("Game Instructions");
        gameDescriptionArea.setText(
            "=== ESCAPE ROOM GAME INSTRUCTIONS ===\n\n" +
            "OBJECTIVE:\n" +
            "Solve puzzles and escape from the room before time runs out!\n\n" +
            "HOW TO PLAY:\n" +
            "1. Look around the room carefully\n" +
            "2. Examine objects for clues\n" +
            "3. Solve puzzles to get keys and items\n" +
            "4. Use items strategically\n" +
            "5. Find the exit before time expires\n\n" +
            "TIPS:\n" +
            "• Pay attention to details\n" +
            "• Think logically\n" +
            "• Don't waste time on dead ends\n" +
            "• Some puzzles require multiple steps\n\n" +
            "CONTROLS:\n" +
            "• Click buttons to interact with objects\n" +
            "• Use your mouse to navigate\n" +
            "• Read descriptions carefully\n\n" +
            "Good luck, and may you escape successfully!"
        );
    }

    @FXML
    private void logout() {
        try {
            LogIn.changeScene("com/example/escapeGame/login.fxml", "Login");
        } catch (IOException e) {
            System.err.println("Error switching to login scene: " + e.getMessage());
            statusLabel.setText("Error logging out!");
        }
    }

    private void updateScore() {
        scoreLabel.setText("Score: " + score);
    }

    // Method to be called when user completes puzzles (for future implementation)
    public void addScore(int points) {
        score += points;
        updateScore();
    }

    // Method to reset game state
    public void resetGame() {
        score = 0;
        updateScore();
        startGameButton.setDisable(false);
        startGameButton.setText("Start Adventure");
        statusLabel.setText("Ready to play!");
        gameTitleLabel.setText("Welcome to the Escape Room!");
        gameDescriptionArea.setText(
            "You find yourself trapped in a mysterious room. Your mission is to solve puzzles and escape before time runs out. Look around carefully, examine objects, and use your wits to find the way out!"
        );
    }
}
