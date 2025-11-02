package com.example.escapeGame;

import com.example.escapeGame.ClientRoom;
import javafx.application.Platform;
import com.example.escapeGame.net.NetClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MultiplayerRoomController {
    
    @FXML private Label createRoomCodeLabel;
    @FXML private Button createRoomButton;
    @FXML private TextField roomCodeField;
    @FXML private Button joinRoomButton;
    @FXML private VBox roomInfoBox;
    @FXML private Label roomInfoLabel;
    @FXML private Label playersLabel;
    @FXML private ListView<String> playersListView;
    @FXML private ComboBox<Integer> capacityCombo;
    @FXML private Button startGameButton;
    @FXML private Button backButton;
    // Removed test-only switchUser button and handler
    
    private ClientRoom currentRoom;
    private NetClient net;
    private String currentUsername;
    private Timer roomUpdateTimer;
    private ObservableList<String> playersList;
    
    public void initialize() {
        // Check if user is logged in
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to access multiplayer mode!");
            // Navigate back to login
            try {
                LogIn.changeScene("com/example/escapeGame/login.fxml", "Login");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        currentUsername = currentUser.getUsername();
        // Try to connect to local server; if it fails, fallback to local mode
        try {
            net = new NetClient("127.0.0.1", 9090);
            net.setOnMessage(this::onServerMessage);
            net.setOnError(msg -> System.out.println("NET ERROR: " + msg));
            net.connect();
            System.out.println("Connected to multiplayer server");
            // Share NetClient across scenes
            Session.getInstance().setNetClient(net);
        } catch (Exception ex) {
            System.out.println("No server found, using local fallback");
            net = null;
            Session.getInstance().setNetClient(null);
        }
        playersList = FXCollections.observableArrayList();
        playersListView.setItems(playersList);
        // Capacity dropdown: 2/3/4
        if (capacityCombo != null) {
            capacityCombo.setItems(FXCollections.observableArrayList(2, 3, 4));
            capacityCombo.setValue(2);
            capacityCombo.setDisable(true); // enabled only for host in net mode
            capacityCombo.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                // Only host in network mode can change it
                if (net != null) {
                    String code = Session.getInstance().getRoomCode();
                    if (code != null) {
                        JsonObject req = new JsonObject();
                        req.addProperty("type", "setCapacity");
                        req.addProperty("roomCode", code);
                        req.addProperty("username", currentUsername);
                        req.addProperty("capacity", newV);
                        net.send(req);
                    }
                }
            });
        }
        
        // Handle Enter key press in room code field
        roomCodeField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                joinRoom();
            }
        });
        
        // Initialize create room code display
        createRoomCodeLabel.setText("Click 'Create Room' to generate code");
        
        // Debug info
        System.out.println("Current user: " + currentUsername);
        System.out.println("Available rooms: " + RoomManager.getInstance().getAllRoomCodes());
    }
    
    @FXML
    private void createRoom() {
        if (net != null) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "createRoom");
            req.addProperty("username", currentUsername);
            // Send selection so server can auto-start with correct pool if it fills
            String selectedRoom = Session.getInstance().getSelectedRoom();
            String selectedDifficulty = Session.getInstance().getSelectedDifficulty();
            if (selectedRoom != null) req.addProperty("room", selectedRoom);
            if (selectedDifficulty != null) req.addProperty("difficulty", selectedDifficulty);
            net.send(req);
        } else {
            String roomCode = RoomManager.getInstance().createRoom(currentUsername);
            createRoomCodeLabel.setText("Room Code: " + roomCode);
            currentRoom = RoomManager.getInstance().getRoom(roomCode);
            Session.getInstance().setRoomCode(roomCode);
            setupRoomUI();
            startRoomUpdates();
        }
    }
    
    @FXML
    private void joinRoom() {
        String roomCode = roomCodeField.getText().trim();
        if (roomCode.isEmpty()) {
            showError("Please enter a room code");
            return;
        }

        // If already in a room, avoid sending another join request
        String currentCode = Session.getInstance().getRoomCode();
        if (currentCode != null && !"null".equals(currentCode)) {
            if (currentCode.equals(roomCode)) {
                showInfo("You are already in this room (" + currentCode + ").");
            } else {
                showError("You are already in room " + currentCode + ". Please leave it first to join another room.");
            }
            return;
        }

        if (net != null) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "joinRoom");
            req.addProperty("roomCode", roomCode);
            req.addProperty("username", currentUsername);
            // Send player's current selection so server can validate compatibility
            String selectedRoom = Session.getInstance().getSelectedRoom();
            String selectedDifficulty = Session.getInstance().getSelectedDifficulty();
            if (selectedRoom != null) req.addProperty("room", selectedRoom);
            if (selectedDifficulty != null) req.addProperty("difficulty", selectedDifficulty);
            net.send(req);
        } else {
            // For testing purposes, show available rooms
            System.out.println("Available rooms: " + RoomManager.getInstance().getAllRoomCodes());
            joinRoomWithCode(roomCode);
        }
    }
    
    private void joinRoomWithCode(String roomCode) {
        // Check if user is already in a room
        if (currentRoom != null) {
            showError("You are already in a room. Please leave the current room first.");
            return;
        }
        
        System.out.println("Attempting to join room: " + roomCode + " with username: " + currentUsername);
        boolean success = RoomManager.getInstance().joinRoom(roomCode, currentUsername);
        
        if (success) {
            currentRoom = RoomManager.getInstance().getRoom(roomCode);
            Session.getInstance().setRoomCode(roomCode);
            setupRoomUI();
            startRoomUpdates();
            System.out.println("Successfully joined room: " + roomCode);
        } else {
            System.out.println("Failed to join room: " + roomCode);
            showError("Room might be full or doesn't exist.");
        }
    }
    
    private void setupRoomUI() {
        if (currentRoom == null) return;
        
        roomInfoBox.setVisible(true);
        roomInfoLabel.setText("Room Code: " + currentRoom.getRoomCode());
        updatePlayersList();
        
        // Show start button only for host
        startGameButton.setVisible(currentRoom.isHost(currentUsername));
        
        // Disable create/join buttons
        createRoomButton.setDisable(true);
        joinRoomButton.setDisable(true);
        roomCodeField.setDisable(true);
    }
    
    private void updatePlayersList() {
        if (currentRoom == null) return;
        
        playersList.clear();
        for (Player player : currentRoom.getPlayers()) {
            String displayText = player.getUsername();
            if (player.isHost()) {
                displayText += " (Host)";
            }
            playersList.add(displayText);
        }
        
        // Local mode uses fixed max 4
        playersLabel.setText("Players: " + currentRoom.getPlayerCount() + "/4");
        
        // Update start button visibility
        boolean canStart = currentRoom.isHost(currentUsername) && currentRoom.getPlayerCount() >= 2;
        startGameButton.setVisible(canStart);

        // In local (no-server) mode, auto-start when 4 players are present
        if (net == null && currentRoom.isHost(currentUsername) && currentRoom.getPlayerCount() >= 4 && currentRoom.getGameState() != ClientRoom.GameState.IN_PROGRESS) {
            startGame();
        }
    }
    
    private void startRoomUpdates() {
        // Update room information every 2 seconds
        roomUpdateTimer = new Timer();
        roomUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentRoom != null) {
                        ClientRoom updatedRoom = RoomManager.getInstance().getRoom(currentRoom.getRoomCode());
                        if (updatedRoom != null) {
                            currentRoom = updatedRoom;
                            updatePlayersList();
                        } else {
                            // Room was deleted, go back to main menu
                            Platform.runLater(() -> {
                                showError("Room was closed by host");
                                goBack();
                            });
                        }
                    }
                });
            }
        }, 0, 2000);
    }
    
    @FXML
    private void startGame() {
        if (net != null) {
            String code = Session.getInstance().getRoomCode();
            if (code == null) return;
            JsonObject req = new JsonObject();
            req.addProperty("type", "startGame");
            req.addProperty("roomCode", code);
            req.addProperty("username", currentUsername);
            // Include selected room and difficulty so server filters puzzles.json accordingly
            String selectedRoom = Session.getInstance().getSelectedRoom();
            String selectedDifficulty = Session.getInstance().getSelectedDifficulty();
            if (selectedRoom != null) req.addProperty("room", selectedRoom);
            if (selectedDifficulty != null) req.addProperty("difficulty", selectedDifficulty);
            net.send(req);
        } else {
            if (currentRoom == null || !currentRoom.isHost(currentUsername)) {
                return;
            }
            boolean success = RoomManager.getInstance().startGame(currentRoom.getRoomCode(), currentUsername);
            if (success) {
                try { LogIn.changeScene("com/example/escapeGame/competitiveGame.fxml", "Competitive Game"); } catch (IOException e) { e.printStackTrace(); }
            } else {
                showError("Failed to start game");
            }
        }
    }
    
    @FXML
    private void goBack() {
        if (roomUpdateTimer != null) {
            roomUpdateTimer.cancel();
        }

        if (currentRoom != null) {
            RoomManager.getInstance().leaveRoom(currentRoom.getRoomCode(), currentUsername);
        }

        // Inform server in network mode
        if (net != null) {
            String code = Session.getInstance().getRoomCode();
            if (code != null) {
                JsonObject req = new JsonObject();
                req.addProperty("type", "leaveRoom");
                req.addProperty("roomCode", code);
                req.addProperty("username", currentUsername);
                net.send(req);
            }
            try { net.close(); } catch (Exception ignored) {}
            Session.getInstance().setNetClient(null);
        }
        // Clear local room code to allow joining another room later
        Session.getInstance().setRoomCode(null);

        try {
            LogIn.changeScene("com/example/escapeGame/afterLogin.fxml", "After Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // removed test-only switchUser()
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        // Wrap and show full message
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        alert.getDialogPane().setContent(content);

        // Make dialog larger and resizable so long text isn't truncated
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(450);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setOnShown(e -> alert.getDialogPane().getScene().getWindow().sizeToScene());

        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        // Wrap and show full message
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        alert.getDialogPane().setContent(content);

        // Make dialog larger and resizable so long text isn't truncated
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(450);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setOnShown(e -> alert.getDialogPane().getScene().getWindow().sizeToScene());

        alert.showAndWait();
    }

    private void showInfoNonBlocking(String message, double seconds) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        // Wrap and show full message
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        alert.getDialogPane().setContent(content);

        // Make dialog larger and resizable so long text isn't truncated
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(450);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setOnShown(e -> alert.getDialogPane().getScene().getWindow().sizeToScene());

        alert.show();
        PauseTransition delay = new PauseTransition(Duration.seconds(seconds));
        delay.setOnFinished(ev -> alert.close());
        delay.play();
    }

    private void onServerMessage(JsonObject msg) {
        String type = msg.get("type").getAsString();
        switch (type) {
            case "roomCreated": {
                String code = msg.get("roomCode").getAsString();
                Session.getInstance().setRoomCode(code);
                createRoomCodeLabel.setText("Room Code: " + code);
                roomInfoBox.setVisible(true);
                roomInfoLabel.setText("Room Code: " + code);
                playersList.clear();
                playersList.add(currentUsername + " (Host)");
                // Default capacity is 2 until server snapshot arrives
                if (capacityCombo != null) {
                    capacityCombo.setDisable(false);
                    capacityCombo.setValue(2);
                }
                playersLabel.setText("Players: 1/" + (capacityCombo != null ? capacityCombo.getValue() : 2));
                startGameButton.setVisible(true);
                startGameButton.setDisable(true);
                // Disable join/create controls once in a room
                createRoomButton.setDisable(true);
                joinRoomButton.setDisable(true);
                roomCodeField.setDisable(true);
                break;
            }
            case "joined":
            case "roomUpdate": {
                JsonObject room = msg.get("room").getAsJsonObject();
                String code = room.get("code").getAsString();
                Session.getInstance().setRoomCode(code);
                roomInfoBox.setVisible(true);
                roomInfoLabel.setText("Room Code: " + code);
                playersList.clear();
                JsonArray arr = room.getAsJsonArray("players");
                for (JsonElement e : arr) {
                    playersList.add(e.getAsString());
                }
                int capacity = room.has("capacity") ? room.get("capacity").getAsInt() : 2;
                playersLabel.setText("Players: " + arr.size() + "/" + capacity);
                boolean canStart = room.has("canStart") && room.get("canStart").getAsBoolean();
                // Only host can start
                String host = room.has("host") ? room.get("host").getAsString() : null;
                boolean isHost = host != null && host.equals(currentUsername);
                startGameButton.setVisible(true);
                startGameButton.setDisable(!(canStart && isHost));
                // Capacity control only for host
                if (capacityCombo != null) {
                    capacityCombo.setDisable(!isHost);
                    if (capacityCombo.getValue() == null || !capacityCombo.getValue().equals(capacity)) {
                        capacityCombo.setValue(capacity);
                    }
                }
                // Disable join/create controls once in a room
                createRoomButton.setDisable(true);
                joinRoomButton.setDisable(true);
                roomCodeField.setDisable(true);
                break;
            }
            case "gameStarted": {
                try {
                    LogIn.changeScene("com/example/escapeGame/competitiveGame.fxml", "Competitive Game");
                } catch (IOException ignored) {
                }
                break;
            }
            case "gameStarting": {
                String text = msg.has("message") ? msg.get("message").getAsString() : "Game is starting";
                showInfoNonBlocking(text, 3.0);
                break;
            }
            case "question": {
                // Handled by CompetitiveGameController on scene load; could also display pre-game countdown
                break;
            }
            case "error": {
                showError(msg.get("message").getAsString());
                break;
            }
        }
    }
}
