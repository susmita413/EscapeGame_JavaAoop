package com.example.escapeGame;

import com.example.escapeGame.net.NetClient;
import javafx.application.Platform;
import javafx.stage.Stage;

public class ChatWindow {
    private static Stage chatStage;
    private static javafx.scene.control.ComboBox<String> playerCombo;
    private static javafx.scene.control.TextArea chatArea;
    
    public static void showChatWindow() {
        System.out.println("ChatWindow.showChatWindow() called");
        
        if (Platform.isFxApplicationThread()) {
            System.out.println("On JavaFX Application Thread, creating window directly");
            createChatWindow();
        } else {
            System.out.println("Not on JavaFX Application Thread, using Platform.runLater");
            Platform.runLater(() -> createChatWindow());
        }
    }
    
    private static void createChatWindow() {
        try {
            System.out.println("Creating chat window...");
            
            if (chatStage != null && chatStage.isShowing()) {
                System.out.println("Chat window already showing, bringing to front");
                chatStage.toFront();
                return;
            }
            
            chatArea = null;
            
            chatStage = new Stage();
            // Show whose chat box this is (window title)
            String currentUserTitle = (Session.getCurrentUser() != null &&
                    Session.getCurrentUser().getUsername() != null &&
                    !Session.getCurrentUser().getUsername().isEmpty())
                    ? Session.getCurrentUser().getUsername()
                    : "You";
            chatStage.setTitle("Chat - " + currentUserTitle);
            
            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
            root.setStyle("-fx-background-color: #4E2D10; -fx-padding: 10;");
            
            // Chat display area
            chatArea = new javafx.scene.control.TextArea();
            chatArea.setPrefHeight(200);
            chatArea.setEditable(false);
            chatArea.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            System.out.println("Chat area created: " + chatArea);
            
            // Message input
            javafx.scene.control.TextField messageField = new javafx.scene.control.TextField();
            messageField.setPromptText("Type your message...");
            messageField.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            
            // Send button
            javafx.scene.control.Button sendButton = new javafx.scene.control.Button("Send");
            sendButton.setStyle("-fx-background-color: #C9942A; -fx-text-fill: black;");
            System.out.println("Send button created: " + sendButton);
            
            // Player selection
            playerCombo = new javafx.scene.control.ComboBox<>();
            playerCombo.setPromptText("Select receiver");
            playerCombo.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            playerCombo.getItems().add("Everyone");
            playerCombo.setValue("Everyone");
            
            // Emoji buttons
            javafx.scene.layout.HBox emojiBox = new javafx.scene.layout.HBox(5);
            javafx.scene.control.Button heartBtn = new javafx.scene.control.Button("❤");
            javafx.scene.control.Button laughBtn = new javafx.scene.control.Button("😂");
            javafx.scene.control.Button thumbsBtn = new javafx.scene.control.Button("👍");
            emojiBox.getChildren().addAll(heartBtn, laughBtn, thumbsBtn);
            
            // Layout
            // Show whose chat box this is (header label)
            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Chat - " + currentUserTitle);
            titleLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            root.getChildren().addAll(
                titleLabel,
                chatArea,
                playerCombo,
                messageField,
                sendButton,
                emojiBox
            );
            
            // Event handlers
            System.out.println("Setting up event handlers...");
            sendButton.setOnAction(e -> {
                System.out.println("Send button clicked!");
                String message = messageField.getText().trim();
                System.out.println("Message text: '" + message + "'");
                if (!message.isEmpty()) {
                    String receiver = playerCombo.getValue() != null ? playerCombo.getValue() : "Everyone";
                    System.out.println("Receiver: " + receiver);
                    
                    messageField.clear();
                    sendChatMessage(message, receiver);
                } else {
                    System.out.println("Message is empty, not sending");
                }
            });
            
            messageField.setOnAction(e -> {
                System.out.println("Enter key pressed in message field");
                sendButton.fire();
            });
            
            // Emoji handlers
            heartBtn.setOnAction(e -> {
                System.out.println("Heart emoji button clicked");
                String receiver = playerCombo.getValue() != null ? playerCombo.getValue() : "Everyone";
                sendEmoji("❤", receiver);
                heartBtn.setDisable(true);
                javafx.application.Platform.runLater(() -> heartBtn.setDisable(false));
            });
            laughBtn.setOnAction(e -> {
                System.out.println("Laugh emoji button clicked");
                String receiver = playerCombo.getValue() != null ? playerCombo.getValue() : "Everyone";
                sendEmoji("😂", receiver);
                laughBtn.setDisable(true);
                javafx.application.Platform.runLater(() -> laughBtn.setDisable(false));
            });
            thumbsBtn.setOnAction(e -> {
                System.out.println("Thumbs up emoji button clicked");
                String receiver = playerCombo.getValue() != null ? playerCombo.getValue() : "Everyone";
                sendEmoji("👍", receiver);
                thumbsBtn.setDisable(true);
                javafx.application.Platform.runLater(() -> thumbsBtn.setDisable(false));
            });
            
            // Set up the scene
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 350);
            chatStage.setScene(scene);
            chatStage.setResizable(false);
            chatStage.show();
            
            System.out.println("Chat window created and shown successfully!");
            
            if (chatArea == null) {
                System.err.println("ERROR: Chat area is null after creation!");
            }
            
            // Note: We don't set up network message handling here anymore
            // The game controller will handle all messages and delegate chat messages to us
            System.out.println("Chat window created - game controller will handle message routing");
            
            // Ask server for current room state so everyone (not only host) sees the latest players list
            try {
                NetClient nc = Session.getInstance().getNetClient();
                if (nc != null) {
                    com.google.gson.JsonObject req = new com.google.gson.JsonObject();
                    req.addProperty("type", "getRoomState");
                    req.addProperty("roomCode", Session.getInstance().getRoomCode());
                    nc.send(req);
                    System.out.println("Requested room state from ChatWindow to refresh recipients list");
                }
            } catch (Exception ignored) {}
            
        } catch (Exception e) {
            System.err.println("Error creating chat window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void sendChatMessage(String message, String receiver) {
        try {
            NetClient netClient = Session.getInstance().getNetClient();
            if (netClient != null) {
                String currentUser = Session.getCurrentUser() != null ? Session.getCurrentUser().getUsername() : "You";
                
                // Display the message immediately for the sender
                String displayMessage;
                if ("Everyone".equals(receiver)) {
                    displayMessage = currentUser + ": " + message;
                } else {
                    displayMessage = "[PRIVATE to " + receiver + "] " + currentUser + ": " + message;
                }
                
                Platform.runLater(() -> {
                    if (chatArea != null) {
                        chatArea.appendText(displayMessage + "\n");
                        chatArea.setScrollTop(Double.MAX_VALUE);
                        System.out.println("Displayed sender's message: " + displayMessage);
                    }
                });
                
                com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                msg.addProperty("type", "chatMessage");
                
                com.google.gson.JsonObject chatMsg = new com.google.gson.JsonObject();
                chatMsg.addProperty("sender", currentUser);
                chatMsg.addProperty("receiver", receiver);
                chatMsg.addProperty("content", message);
                chatMsg.addProperty("type", "Everyone".equals(receiver) ? "PUBLIC" : "PRIVATE");
                
                msg.add("chatMessage", chatMsg);
                msg.addProperty("roomCode", Session.getInstance().getRoomCode());
                
                // Send on a background thread so UI never blocks when sending
                new Thread(() -> {
                    try {
                        netClient.send(msg);
                        System.out.println("Chat message sent successfully: " + message + " to " + receiver);
                    } catch (Exception ex) {
                        System.err.println("Failed to send chat message: " + ex.getMessage());
                    }
                }, "chat-send").start();
            } else {
                System.err.println("No NetClient available - cannot send message");
            }
        } catch (Exception e) {
            System.err.println("Error sending chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void sendEmoji(String emoji, String receiver) {
        System.out.println("sendEmoji called with: " + emoji + " to: " + receiver);
        String actualReceiver = receiver != null ? receiver : "Everyone";
        System.out.println("Sending emoji to: " + actualReceiver);
        
        sendChatMessage(emoji, actualReceiver);
    }
    
    public static void hideChatWindow() {
        Platform.runLater(() -> {
            if (chatStage != null && chatStage.isShowing()) {
                chatStage.hide();
            }
        });
    }

    // Fully close and dispose chat window and clear references
    public static void closeChatWindow() {
        Platform.runLater(() -> {
            try {
                if (chatStage != null) {
                    chatStage.close();
                }
            } catch (Exception ignored) {}
            chatStage = null;
            chatArea = null;
            playerCombo = null;
        });
    }
    
    public static void handleChatMessage(com.google.gson.JsonObject msg) {
        System.out.println("Handling chat message: " + msg);
        
        if (msg.has("type") && msg.get("type").getAsString().equals("chatMessage")) {
            if (msg.has("chatMessage")) {
                com.google.gson.JsonObject chatJson = msg.getAsJsonObject("chatMessage");
                String sender = chatJson.get("sender").getAsString();
                String content = chatJson.get("content").getAsString();
                String type = chatJson.get("type").getAsString();
                
                System.out.println("Processing message from: " + sender + ", content: " + content + ", type: " + type);
                
                // CRITICAL FIX: Don't display messages from the current user (they already see their own messages)
                String currentUser = Session.getCurrentUser() != null ? Session.getCurrentUser().getUsername() : "";
                System.out.println("Current user: " + currentUser + ", Message sender: " + sender);
                if (sender.equals(currentUser)) {
                    System.out.println("*** SKIPPING message from current user to prevent echo ***");
                    return;
                }
                
                String displayMessage;
                if ("PRIVATE".equals(type)) {
                    displayMessage = "[PRIVATE] " + sender + ": " + content;
                } else {
                    displayMessage = sender + ": " + content;
                }
                
                System.out.println("Displaying message: " + displayMessage);
                
                Platform.runLater(() -> {
                    try {
                        if (chatArea != null) {
                            System.out.println("Chat area is not null, appending message...");
                            chatArea.appendText(displayMessage + "\n");
                            System.out.println("Message appended to chat area successfully");
                            chatArea.setScrollTop(Double.MAX_VALUE);
                        } else {
                            System.err.println("Chat area is null - creating new chat window...");
                            showChatWindow();
                            javafx.application.Platform.runLater(() -> {
                                if (chatArea != null) {
                                    chatArea.appendText(displayMessage + "\n");
                                    System.out.println("Message appended to new chat area successfully");
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("Error appending to chat area: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }
    
    public static void updatePlayerList(java.util.List<String> players) {
        System.out.println("Updating player list in chat window: " + players);
        
        if (playerCombo != null && players != null) {
            Platform.runLater(() -> {
                // Clear existing items except "Everyone"
                playerCombo.getItems().clear();
                playerCombo.getItems().add("Everyone");
                
                String currentUser = Session.getCurrentUser() != null ? Session.getCurrentUser().getUsername() : "";
                System.out.println("Current user: " + currentUser);
                
                // Add all players except current user
                for (String player : players) {
                    String cleanName = player;
                    if (player.contains(" - ")) {
                        cleanName = player.split(" - ")[0];
                    }
                    
                    if (!cleanName.equals(currentUser) && !cleanName.isEmpty()) {
                        playerCombo.getItems().add(cleanName);
                        System.out.println("Added player to dropdown: " + cleanName);
                    }
                }
                
                // Set default selection
                playerCombo.setValue("Everyone");
                System.out.println("Player list updated in dropdown: " + playerCombo.getItems());
                System.out.println("Total players in dropdown: " + playerCombo.getItems().size());
                
                // If no other players available, show a message
                if (playerCombo.getItems().size() == 1) {
                    System.out.println("Only 'Everyone' option available - no other players in room");
                }
            });
        } else {
            System.out.println("Cannot update player list - playerCombo is null or players list is null");
            System.out.println("playerCombo: " + playerCombo);
            System.out.println("players: " + players);
        }
    }
}