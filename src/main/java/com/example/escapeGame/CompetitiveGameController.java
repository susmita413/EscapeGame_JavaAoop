package com.example.escapeGame;

import com.example.escapeGame.net.NetClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompetitiveGameController {
    
    @FXML private Label roomCodeLabel;
    @FXML private Label usernameLabel;
    @FXML private Label roomLabel;
    @FXML private Label levelLabel;
    @FXML private Label questionCounterLabel;
    @FXML private Label timerLabel;
    @FXML private Label questionLabel;
    @FXML private TextField answerField;
    @FXML private Button submitButton;
    @FXML private ListView<String> playersListView;
    @FXML private Label scoreLabel;
    @FXML private Button nextQuestionButton;
    @FXML private Button leaveButton;
    @FXML private ScrollPane questionScroll;
    
    private ClientRoom currentRoom;
    private String currentUsername;
    private Timeline timer;
    private int timeRemaining;
    private long questionStartTime;
    private boolean hasAnswered;
    private ObservableList<String> playersList;
    private NetClient net;
    private boolean networkMode;
    private int netCurrentIndex;
    private int netTotalQuestions;
    // Debounce flag for auto-fit scheduling to avoid flooding FX thread
    private boolean autoFitScheduled;
    
    
    public void initialize() {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to access competitive mode!");
            // Navigate back to login
            try {
                LogIn.changeScene("com/example/escapeGame/login.fxml", "Login");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        currentUsername = currentUser.getUsername();
        if (usernameLabel != null) {
            usernameLabel.setText("User: " + currentUsername);
        }
        // Show current room and level in the header
        String selRoom = Session.getInstance().getSelectedRoom();
        String selDiff = Session.getInstance().getSelectedDifficulty();
        if (roomLabel != null) {
            roomLabel.setText("Room: " + (selRoom == null ? "-" : selRoom));
        }
        if (levelLabel != null) {
            levelLabel.setText("Level: " + (selDiff == null ? "-" : selDiff));
        }
        playersList = FXCollections.observableArrayList();
        playersListView.setItems(playersList);
        // Auto-fit question font when layout or text changes
        if (questionScroll != null) {
            questionScroll.viewportBoundsProperty().addListener((obs, o, n) -> scheduleAutoFit());
        }
        if (questionLabel != null) {
            questionLabel.widthProperty().addListener((obs, o, n) -> scheduleAutoFit());
            questionLabel.textProperty().addListener((obs, o, n) -> scheduleAutoFit());
        }
        
        // Handle Enter key press in answer field
        answerField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                submitAnswer();
            }
        });
        
        // Initialize timer
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);

        net = Session.getInstance().getNetClient();
        networkMode = (net != null);
        
        // For solo mode, hide the right panel (players score), chat button, and room code, show solo score
        if (!networkMode) {
            if (rightPanel != null) {
                rightPanel.setVisible(false);
                rightPanel.setManaged(false);
            }
            if (openChatButton != null) {
                openChatButton.setVisible(false);
                openChatButton.setManaged(false);
            }
            if (roomCodeLabel != null) {
                roomCodeLabel.setVisible(false);
            }
            if (soloScoreLabel != null) {
                soloScoreLabel.setVisible(true);
            }
        } else {
            // For multiplayer mode, hide the solo score label and show room code
            if (soloScoreLabel != null) {
                soloScoreLabel.setVisible(false);
            }
            if (roomCodeLabel != null) {
                roomCodeLabel.setVisible(true);
            }
        }
        
        if (networkMode) {
            setupNetwork();
        } else {
            setupRoom();
        }
    }

    private void setupNetwork() {
        String roomCode = Session.getInstance().getRoomCode();
        System.out.println("Setting up network for user: " + currentUsername + ", room code: " + roomCode);
        if (roomCode == null || roomCode.equals("null")) {
            System.err.println("WARNING: Room code is null for user " + currentUsername);
            roomCodeLabel.setText("Room Code: null (ERROR)");
        } else {
            roomCodeLabel.setText("Room Code: " + roomCode);
        }
        nextQuestionButton.setVisible(false);

        if (net != null) {
            net.setOnMessage(this::onNetMessage);
        }
    }

    private void onNetMessage(JsonObject msg) {
        String type = msg.get("type").getAsString();
        switch (type) {
            case "joined": {
                // Handle successful room join
                JsonObject room = msg.get("room").getAsJsonObject();
                String code = room.get("code").getAsString();
                Session.getInstance().setRoomCode(code);
                System.out.println("User " + currentUsername + " successfully joined room: " + code);
                Platform.runLater(() -> {
                    roomCodeLabel.setText("Room Code: " + code);
                    updatePlayersListNetwork(room.getAsJsonArray("players"));
                    // Ensure chat dropdown is populated immediately after join for all players (not only host)
                    updateChatPlayerList();
                });
                break;
            }
            case "roomUpdate": {
                JsonObject room = msg.getAsJsonObject("room");
                if (room != null) {
                    // Update room code if provided
                    if (room.has("code")) {
                        String code = room.get("code").getAsString();
                        Session.getInstance().setRoomCode(code);
                        System.out.println("Updated room code to: " + code);
                        Platform.runLater(() -> roomCodeLabel.setText("Room Code: " + code));
                    }
                    // Update players list
                    if (room.has("players")) {
                        JsonArray arr = room.getAsJsonArray("players");
                        Platform.runLater(() -> {
                            updatePlayersListNetwork(arr);
                            // Also update chat player list if chat window is open
                            updateChatPlayerList();
                        });
                    }
                }
                break;
            }
            case "question": {
                String text = msg.has("text") ? msg.get("text").getAsString() : "";
                int index = msg.has("index") ? msg.get("index").getAsInt() : 1;
                int total = msg.has("total") ? msg.get("total").getAsInt() : 10;
                int timeSec = msg.has("timeSec") ? msg.get("timeSec").getAsInt() : 60;
                Platform.runLater(() -> startNetworkQuestion(text, index, total, timeSec));
                break;
            }
            case "nextQuestion": {
                String text = msg.has("text") ? msg.get("text").getAsString()
                        : (msg.has("question") ? msg.get("question").getAsString() : "");
                int index = msg.has("index") ? msg.get("index").getAsInt()
                        : (msg.has("questionIndex") ? (msg.get("questionIndex").getAsInt() + 1) : netCurrentIndex + 1);
                int total = msg.has("total") ? msg.get("total").getAsInt()
                        : (msg.has("totalQuestions") ? msg.get("totalQuestions").getAsInt() : netTotalQuestions);
                int timeSec = msg.has("timeSec") ? msg.get("timeSec").getAsInt() : 60;
                Platform.runLater(() -> startNetworkQuestion(text, index, total, timeSec));
                break;
            }
            case "answerResult": {
                String user = msg.has("username") ? msg.get("username").getAsString() : "";
                System.out.println("Received answerResult for user: " + user + ", current user: " + currentUsername);
                if (user.equals(currentUsername)) {
                    boolean correct = msg.has("correct") && msg.get("correct").getAsBoolean();
                    int delta = msg.has("scoreDelta") ? msg.get("scoreDelta").getAsInt() : 0;
                    String resultText = correct ? ("Correct! +" + delta + " points") : ("Wrong answer, " + delta + " points");
                    System.out.println("Processing answerResult: " + resultText);
                    Platform.runLater(() -> {
                        questionLabel.setText(resultText);
                        // Set color based on answer correctness
                        if (correct) {
                            questionLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #000000; -fx-border-color: #c3e6cb; -fx-border-width: 2px; -fx-padding: 10px;");
                        } else {
                            questionLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #000000; -fx-border-color: #f5c6cb; -fx-border-width: 2px; -fx-padding: 10px;");
                        }
                        // Optimistically update local score label so UI reflects -2 penalty too
                        try {
                            String txt = scoreLabel.getText(); // format: "Your Score: N"
                            int idx = txt.lastIndexOf(':');
                            if (idx >= 0) {
                                int current = Integer.parseInt(txt.substring(idx + 1).trim());
                                int updated = current + delta; // apply even when negative
                                scoreLabel.setText("Your Score: " + updated);
                            }
                        } catch (Exception ignored) {}
                    });
                }
                break;
            }
            case "scoreUpdate": {
                if (msg.has("scores") && msg.get("scores").isJsonArray()) {
                    JsonArray scores = msg.getAsJsonArray("scores");
                    System.out.println("Received score update: " + scores.toString());
                    Platform.runLater(() -> updateScores(scores));
                }
                break;
            }
            case "teamScoreUpdate": {
                if (msg.has("scores") && msg.get("scores").isJsonArray()) {
                    JsonArray scores = msg.getAsJsonArray("scores");
                    System.out.println("Received team score update: " + scores.toString());
                    Platform.runLater(() -> {
                        updateScores(scores);
                        boolean unanimous = msg.has("unanimous") && msg.get("unanimous").getAsBoolean();
                        if (unanimous) {
                            questionLabel.setText("Everyone corrects, You get extra 2 points");
                            questionLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #000000; -fx-border-color: #c3e6cb; -fx-border-width: 2px; -fx-padding: 10px;");
                        }
                    });
                }
                break;
            }
            case "gameStarted": {
                // Ensure room code is set when game starts
                if (msg.has("roomCode")) {
                    String code = msg.get("roomCode").getAsString();
                    Session.getInstance().setRoomCode(code);
                    System.out.println("Game started in room: " + code);
                    Platform.runLater(() -> roomCodeLabel.setText("Room Code: " + code));
                }
                break;
            }
            case "gameOver": {
                int threshold = msg.has("threshold") ? msg.get("threshold").getAsInt() : 50;
                JsonArray scores = msg.has("scores") && msg.get("scores").isJsonArray()
                        ? msg.getAsJsonArray("scores") : new JsonArray();
                // Close chat for all players, then show the final dialog
                Platform.runLater(() -> {
                    try { ChatWindow.closeChatWindow(); } catch (Exception ignored) {}
                    showFinalResultsAndExit(scores, threshold);
                });
                break;
            }
            case "chatMessage": {
                // Delegate chat messages to ChatWindow
                ChatWindow.handleChatMessage(msg);
                break;
            }
        }
    }

    private void startNetworkQuestion(String text, int index, int total, int timeSec) {
        netCurrentIndex = index;
        netTotalQuestions = total;
        questionLabel.setText(text);
        questionCounterLabel.setText("Question " + index + "/" + total);

        // Reset UI for new question
        answerField.clear();
        answerField.setDisable(false);
        submitButton.setDisable(false);
        hasAnswered = false;
        
        // Reset question label styling for new question
        questionLabel.setStyle("");

        // Start timer with server-provided time
        timeRemaining = timeSec;
        timerLabel.setText(String.valueOf(timeRemaining));
        questionStartTime = System.currentTimeMillis();
        System.out.println("Starting timer for " + timeSec + " seconds");
        timer.playFromStart();
        scheduleAutoFit();
    }

    private void updatePlayersListNetwork(JsonArray players) {
        // Do NOT clobber the scoreboard list with names-only data.
        // Room updates often contain just usernames (no scores). Clearing
        // playersList here freezes the side leaderboard. Instead, update the
        // chat recipients list only.
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            names.add(players.get(i).getAsString());
        }
        ChatWindow.updatePlayerList(names);
    }

    private void updateScores(JsonArray scores) {
        // Update side list and personal score label
        playersList.clear();
        int myScore = 0;
        for (int i = 0; i < scores.size(); i++) {
            JsonObject o = scores.get(i).getAsJsonObject();
            String u = o.get("username").getAsString();
            int s = o.get("score").getAsInt();
            playersList.add(u + " - " + s + " pts");
            if (u.equals(currentUsername)) myScore = s;
        }
        scoreLabel.setText("Your Score: " + myScore);
    }

    private void showFinalResultsAndExit(JsonArray scores, int threshold) {
        timer.stop();
        
        // Convert to list and sort by score (highest first)
        List<JsonObject> scoreList = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            scoreList.add(scores.get(i).getAsJsonObject());
        }
        scoreList.sort((a, b) -> Integer.compare(b.get("score").getAsInt(), a.get("score").getAsInt()));
        
        // Debug: Log the sorted scores
        System.out.println("Final scores sorted by rank:");
        for (int i = 0; i < scoreList.size(); i++) {
            JsonObject o = scoreList.get(i);
            String u = o.get("username").getAsString();
            int s = o.get("score").getAsInt();
            System.out.println((i + 1) + ". " + u + " - " + s + " points");
        }
        
        StringBuilder leaderboardText = new StringBuilder();
        int myScore = 0;
        for (int i = 0; i < scoreList.size(); i++) {
            JsonObject o = scoreList.get(i);
            String u = o.get("username").getAsString();
            int s = o.get("score").getAsInt();
            if (u.equals(currentUsername)) myScore = s;
        }
        boolean escaped = myScore >= threshold;
        String header = escaped ? (currentUsername + " Escaped !") : (currentUsername + " Failed to Escape");
        // Top lines as requested
        leaderboardText.append("your score : ").append(myScore).append("\n\n");
        // Keep detailed info below
        leaderboardText.append("Final Leaderboard:\n\n");
        for (int i = 0; i < scoreList.size(); i++) {
            JsonObject o = scoreList.get(i);
            String u = o.get("username").getAsString();
            int s = o.get("score").getAsInt();
            leaderboardText.append(i + 1).append(". ").append(u)
                    .append(" - ").append(s).append(" points\n");
        }
        leaderboardText.append("\nThreshold: ").append(threshold).append(" points\n");
        leaderboardText.append(escaped ? "Congratulations! You have escaped!" : "You need " + (threshold - myScore) + " more points to escape. Try again!");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(header);
        alert.setContentText(leaderboardText.toString());
        alert.showAndWait();

        // Gracefully leave the room and clear session room code so users can join another room next time
        try {
            String code = Session.getInstance().getRoomCode();
            if (net != null && code != null && !code.equals("null")) {
                JsonObject req = new JsonObject();
                req.addProperty("type", "leaveRoom");
                req.addProperty("roomCode", code);
                req.addProperty("username", currentUsername);
                net.send(req);
            }
        } catch (Exception ignored) {}
        Session.getInstance().setRoomCode(null);

        // Only unlock next level for players who escaped (per-user)
        try {
            String room = Session.getInstance().getSelectedRoom();
            String diff = Session.getInstance().getSelectedDifficulty();
            if (escaped && room != null && diff != null) {
                Session.markLevelAsCompleted(room, diff);
            }
            // Record competitive (network) play and escape info with final score
            LeaderboardDataUtil.recordGame(currentUsername, /*multiplayer=*/true, escaped, room, diff, LocalDateTime.now(), myScore);
        } catch (Exception ignored) {}

        // Navigate back to level selection so the unlocked button is visible immediately
        try {
            String room = Session.getInstance().getSelectedRoom();
            String title = (room != null ? ("Choose Level - " + room) : "Choose Level");
            LogIn.changeScene("com/example/escapeGame/level.fxml", title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setupRoom() {
        String roomCode = Session.getInstance().getRoomCode();
        currentRoom = RoomManager.getInstance().getRoom(roomCode);
        
        if (currentRoom == null) {
            showError("Room not found!");
            return;
        }
        
        roomCodeLabel.setText("Room Code: " + roomCode);
        updatePlayersList();
        updateScore();
        
        if (currentRoom.getGameState() == ClientRoom.GameState.IN_PROGRESS) {
            startQuestion();
        } else {
            showWaitingMessage();
        }
    }
    
    private void startQuestion() {
        Puzzle currentQuestion = currentRoom.getCurrentQuestion();
        if (currentQuestion == null) {
            showGameCompleted();
            return;
        }
        questionLabel.setText(currentQuestion.getQuestion());
        questionCounterLabel.setText("Question " + (currentRoom.getCurrentQuestionIndex() + 1) + "/10");
        // Reset UI for new question
        answerField.clear();
        answerField.setDisable(false);
        submitButton.setDisable(false);
        hasAnswered = false;
        // Reset question label styling for new question
        questionLabel.setStyle("");
        // Start timer
        timeRemaining = getSoloTimeForCurrentDifficulty();
        timerLabel.setText(String.valueOf(timeRemaining));
        questionStartTime = System.currentTimeMillis();
        timer.play();
        scheduleAutoFit();
        
        // Update solo score label if in solo mode
        if (!networkMode) {
            updateScore();
        }
    }

    // Returns per-question time for solo mode based on selected difficulty
    private int getSoloTimeForCurrentDifficulty() {
        try {
            String diff = Session.getInstance().getSelectedDifficulty();
            if (diff == null) return 60;
            if ("Easy".equalsIgnoreCase(diff)) return 90;
            if ("Medium".equalsIgnoreCase(diff)) return 120;
            if ("Hard".equalsIgnoreCase(diff)) return 150;
        } catch (Exception ignored) {}
        return 60;
    }

    // Schedules auto-fit on next pulse to ensure viewport bounds are valid
    private void scheduleAutoFit() {
        if (autoFitScheduled) return;
        autoFitScheduled = true;
        Platform.runLater(() -> {
            try {
                autoFitQuestionFont();
            } finally {
                autoFitScheduled = false;
            }
        });
    }

    // Shrinks or grows the question font so it fits within the scroll viewport height without scrolling (most cases)
    private void autoFitQuestionFont() {
        try {
            if (questionLabel == null) return;
            double viewportW;
            double viewportH;
            if (questionScroll != null && questionScroll.getViewportBounds() != null) {
                viewportW = questionScroll.getViewportBounds().getWidth();
                viewportH = questionScroll.getViewportBounds().getHeight();
            } else {
                // Fallback to label width and a sensible height
                viewportW = Math.max(300, questionLabel.getWidth());
                viewportH = 200;
            }
            if (viewportW <= 0) return;

            String family = questionLabel.getFont().getFamily();
            double currentSize = questionLabel.getFont().getSize();
            // Account for label padding: 20 left/right, 20 top/bottom from FXML
            double wrapW = Math.max(100, viewportW - 40);
            double maxH = Math.max(100, viewportH - 40);

            // Determine a reasonable font size between 14 and 28
            int best = 20; // default
            for (int size = 28; size >= 14; size--) {
                Text probe = new Text(questionLabel.getText() == null ? "" : questionLabel.getText());
                probe.setWrappingWidth(wrapW);
                probe.setFont(Font.font(family, size));
                double h = probe.getLayoutBounds().getHeight();
                if (h <= maxH) { best = size; break; }
            }
            // Optionally try to grow a little if there is ample room
            for (int size = best + 1; size <= 28; size++) {
                Text probe = new Text(questionLabel.getText() == null ? "" : questionLabel.getText());
                probe.setWrappingWidth(wrapW);
                probe.setFont(Font.font(family, size));
                double h = probe.getLayoutBounds().getHeight();
                if (h > maxH) { best = size - 1; break; }
                best = size;
            }
            // Only set font when the computed size actually changes to avoid endless layout thrash
            if (Math.abs(currentSize - best) >= 0.5) {
                questionLabel.setFont(Font.font(family, best));
            }
        } catch (Exception ignored) {}
    }
    
    private void updateTimer() {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerLabel.setText(String.valueOf(timeRemaining));
            
            // Change color based on time remaining
            if (timeRemaining <= 10) {
                timerLabel.setStyle("-fx-text-fill: #e74c3c;");
            } else if (timeRemaining <= 20) {
                timerLabel.setStyle("-fx-text-fill: #f39c12;");
            } else {
                timerLabel.setStyle("-fx-text-fill: #27ae60;");
            }
        } else {
            // Time's up
            timer.stop();
            if (!hasAnswered) {
                submitAnswer(); // Auto-submit empty answer
            }
        }
    }
    
    @FXML
    private void submitAnswer() {
        if (hasAnswered) return;
        
        hasAnswered = true;
        
        String answer = answerField.getText().trim();
        long responseTime = System.currentTimeMillis() - questionStartTime;
        if (networkMode) {
            // Send answer to server
            String code = Session.getInstance().getRoomCode();
            if (code == null || code.equals("null")) {
                System.err.println("ERROR: Cannot submit answer - room code is null for user " + currentUsername);
                showError("Cannot submit answer - not connected to a room!");
                return;
            }
            JsonObject req = new JsonObject();
            req.addProperty("type", "submitAnswer");
            req.addProperty("roomCode", code);
            req.addProperty("username", currentUsername);
            req.addProperty("answer", answer);
            req.addProperty("elapsedMs", responseTime);
            System.out.println("Sending answer: " + answer + " for user: " + currentUsername + " in room: " + code);
            net.send(req);

            // Disable input
            answerField.setDisable(true);
            submitButton.setDisable(true);
            return;
        }

        // Local mode: submit to RoomManager
        RoomManager.getInstance().submitAnswer(currentRoom.getRoomCode(), currentUsername, answer, responseTime);
        // Disable input
        answerField.setDisable(true);
        submitButton.setDisable(true);
        // Update UI
        updateScore();
        updatePlayersList();
        // Show result
        boolean isCorrect = answer.equalsIgnoreCase(currentRoom.getCurrentQuestion().getAnswer());
        String resultText = isCorrect ? "Correct! +" + calculateScore(responseTime) + " points" : "Wrong answer, -5 points";
        questionLabel.setText(resultText);
        // Set color based on answer correctness
        if (isCorrect) {
            questionLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #000000; -fx-border-color: #c3e6cb; -fx-border-width: 2px; -fx-padding: 10px;");
        } else {
            questionLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #000000; -fx-border-color: #f5c6cb; -fx-border-width: 2px; -fx-padding: 10px;");
        }
        // For solo mode, automatically proceed to next question after a delay
        if (!networkMode) {
            // Auto-advance to next question after 2 seconds
            Timeline autoAdvance = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                Platform.runLater(() -> {
                    if (currentRoom.nextQuestion()) {
                        startQuestion();
                    } else {
                        showGameCompleted();
                    }
                });
            }));
            autoAdvance.play();
        } else {
            // Show next question button for host in multiplayer mode
            if (currentRoom.isHost(currentUsername)) {
                nextQuestionButton.setVisible(true);
            }
        }
    }
    
    @FXML
    private void nextQuestion() {
        if (networkMode) return;
        if (!currentRoom.isHost(currentUsername)) return;
        
        nextQuestionButton.setVisible(false);
        
        if (currentRoom.nextQuestion()) {
            startQuestion();
        } else {
            showGameCompleted();
        }
    }
    
    @FXML
    private void leaveRoom() {
        timer.stop();
        if (networkMode) {
            String code = Session.getInstance().getRoomCode();
            if (net != null && code != null) {
                JsonObject req = new JsonObject();
                req.addProperty("type", "leaveRoom");
                req.addProperty("roomCode", code);
                req.addProperty("username", currentUsername);
                net.send(req);
            }
        } else {
            RoomManager.getInstance().leaveRoom(currentRoom.getRoomCode(), currentUsername);
        }
        // Clear room code so user can join another room later
        Session.getInstance().setRoomCode(null);
        // Close chat window when player leaves
        try { ChatWindow.closeChatWindow(); } catch (Exception ignored) {}
        
        try {
            LogIn.changeScene("com/example/escapeGame/afterLogin.fxml", "After Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updatePlayersList() {
        playersList.clear();
        List<Player> players = currentRoom.getPlayers();
        for (Player player : players) {
            String displayText = player.getUsername();
            if (player.isHost()) {
                displayText += " (Host)";
            }
            displayText += " - " + player.getScore() + " pts";
            playersList.add(displayText);
        }
    }
    
    private void updateScore() {
        for (Player player : currentRoom.getPlayers()) {
            if (player.getUsername().equals(currentUsername)) {
                scoreLabel.setText("Your Score: " + player.getScore());
                // Also update the solo score label if in solo mode
                if (!networkMode && soloScoreLabel != null) {
                    soloScoreLabel.setText("Your Score: " + player.getScore());
                }
                break;
            }
        }
    }
    
    private int calculateScore(long responseTimeMs) {
        // Solo mode display: fixed +10 per correct answer
        return 10;
    }
    
    private void showWaitingMessage() {
        questionLabel.setText("Waiting for host to start the game...");
        answerField.setDisable(true);
        submitButton.setDisable(true);
    }
    
    private void showGameCompleted() {
        timer.stop();
        questionLabel.setText("Game Completed!");
        answerField.setDisable(true);
        submitButton.setDisable(true);
        
        // Get player's final score
        int myScore = 0;
        for (Player p : currentRoom.getPlayers()) {
            if (p.getUsername().equals(currentUsername)) { 
                myScore = p.getScore(); 
                break; 
            }
        }
        
        // Check if player escaped (threshold: 50)
        int threshold = 50;
        boolean escaped = myScore >= threshold;
        final int myScoreFinal = myScore;
        final boolean escapedFinal = escaped;
        
        // Create leaderboard display (requested format)
        List<Player> leaderboard = currentRoom.getLeaderboard();
        StringBuilder leaderboardText = new StringBuilder();
        // Top lines as requested
        leaderboardText.append("your score : ").append(myScore).append("\n\n");
        // Keep detailed info below
        leaderboardText.append("Final Leaderboard:\n");
        leaderboardText.append("───────────────────\n");
        for (int i = 0; i < leaderboard.size(); i++) {
            Player player = leaderboard.get(i);
            leaderboardText.append((i + 1)).append(". ").append(player.getUsername())
                          .append(" - ").append(player.getScore()).append(" points\n");
        }
        leaderboardText.append("\nThreshold: ").append(threshold).append(" points\n");
        if (escaped) {
            leaderboardText.append("Congratulations! You have escaped!\n");
            leaderboardText.append("You can now access the next difficulty level!\n");
        } else {
            leaderboardText.append("You need ").append(threshold - myScore).append(" more points to escape.\n");
            leaderboardText.append("Try again to unlock the next level!\n");
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(escaped ? (currentUsername + " Escaped !") : (currentUsername + " Failed to Escape"));
        alert.setContentText(leaderboardText.toString());
        
        // Use Platform.runLater to avoid animation conflicts
        Platform.runLater(() -> {
            alert.showAndWait();
            
            // Record game results and unlock level if escaped
            try {
                String room = Session.getInstance().getSelectedRoom();
                String diff = Session.getInstance().getSelectedDifficulty();
                if (room != null && diff != null && escapedFinal) {
                    Session.markLevelAsCompleted(room, diff);
                }
                // Record solo play and escape info with final score
                LeaderboardDataUtil.recordGame(currentUsername, /*multiplayer=*/false, escapedFinal, room, diff, LocalDateTime.now(), myScoreFinal);
            } catch (Exception ignored) {}

            // Return to level selection to reveal the unlocked level
            try {
                String room = Session.getInstance().getSelectedRoom();
                String title = (room != null ? ("Choose Level - " + room) : "Choose Level");
                LogIn.changeScene("com/example/escapeGame/level.fxml", title);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML private Button openChatButton;
    @FXML private VBox rightPanel;
    @FXML private Label soloScoreLabel;

    @FXML
    private void openChat() {
        System.out.println("Open Chat button clicked!");
        System.err.println("Open Chat button clicked! - ERROR OUTPUT");
        try {
            ChatWindow.showChatWindow();
            System.out.println("ChatWindow.showChatWindow() called successfully");
            System.err.println("ChatWindow.showChatWindow() called successfully - ERROR OUTPUT");
            
            // Request current room state to get updated player list
            requestRoomUpdate();
            
            // Update player list in chat window with current players
            updateChatPlayerList();
            
        } catch (Exception e) {
            System.err.println("Error opening chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void requestRoomUpdate() {
        try {
            if (net != null) {
                com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                msg.addProperty("type", "getRoomState");
                msg.addProperty("roomCode", Session.getInstance().getRoomCode());
                net.send(msg);
                System.out.println("Requested room state update");
            }
        } catch (Exception e) {
            System.err.println("Error requesting room update: " + e.getMessage());
        }
    }
    
    private void updateChatPlayerList() {
        if (playersList != null && !playersList.isEmpty()) {
            java.util.List<String> playerNames = new java.util.ArrayList<>();
            for (String player : playersList) {
                // Remove score information for chat purposes
                String cleanName = player.split(" - ")[0];
                playerNames.add(cleanName);
            }
            ChatWindow.updatePlayerList(playerNames);
            System.out.println("Player list updated: " + playerNames);
        } else {
            System.out.println("No players in list to update - will try again when room update is received");
            // Add a fallback - try to get players from current room if available
            if (currentRoom != null && currentRoom.getPlayers() != null) {
                java.util.List<String> playerNames = new java.util.ArrayList<>();
                for (Player player : currentRoom.getPlayers()) {
                    playerNames.add(player.getUsername());
                }
                ChatWindow.updatePlayerList(playerNames);
                System.out.println("Updated player list from currentRoom: " + playerNames);
            }
        }
    }
}