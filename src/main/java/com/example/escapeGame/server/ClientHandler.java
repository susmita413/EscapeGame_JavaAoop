package com.example.escapeGame.server;

import com.example.escapeGame.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;

class ClientHandler implements Runnable {
    private final Socket socket;
    private final RoomDirectory rooms;
    private final Gson gson = new Gson();
    private volatile PrintWriter out;
    private volatile String currentRoomCode;
    private volatile String username;
    private volatile boolean identified = false;

    ClientHandler(Socket socket, RoomDirectory rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            this.out = writer;
            System.out.println("Client connected from: " + socket.getInetAddress() + " (waiting for identification)");
            String line;
            while ((line = reader.readLine()) != null) {
                handle(line);
            }
        } catch (IOException e) {
            if (identified && username != null) {
                System.out.println("User " + username + " disconnected: " + e.getMessage());
            } else {
                System.out.println("Client disconnected: " + e.getMessage());
            }
        } finally {
            if (currentRoomCode != null && username != null) {
                System.out.println("User " + username + " leaving room " + currentRoomCode);
                rooms.leave(currentRoomCode, username, this);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handle(String line) {
        try {
            System.out.println("Received message: " + line);
            JsonObject msg = gson.fromJson(line, JsonObject.class);
            if (!msg.has("type")) {
                sendError("Message missing 'type' field");
                return;
            }
            String type = msg.get("type").getAsString();
            switch (type) {
                case "ping":
                    send(json("pong")); break;
                case "createRoom": {
                    username = msg.get("username").getAsString();
                    identifyUser(username);
                    System.out.println("User " + username + " creating room");
                    String code = rooms.createRoom(username);
                    currentRoomCode = code;
                    rooms.addClientToRoom(code, username, this);
                    System.out.println("Room " + code + " created by " + username);
                    // Persist initial selection (room/difficulty) if provided
                    try {
                        String room = msg.has("room") ? msg.get("room").getAsString() : null;
                        String difficulty = msg.has("difficulty") ? msg.get("difficulty").getAsString() : null;
                        if (room != null || difficulty != null) {
                            rooms.setSelection(code, room, difficulty);
                            System.out.println("Room " + code + " settings set - Room: " + room + ", Difficulty: " + difficulty);
                        }
                    } catch (Exception ignored) {}
                    JsonObject resp = new JsonObject();
                    resp.addProperty("type", "roomCreated");
                    resp.addProperty("roomCode", code);
                    send(resp);
                    rooms.broadcastRoomUpdate(code);
                    break;
                }
                case "joinRoom": {
                    String code = msg.get("roomCode").getAsString();
                    username = msg.get("username").getAsString();
                    identifyUser(username);
                    String room = msg.has("room") ? msg.get("room").getAsString() : null;
                    String difficulty = msg.has("difficulty") ? msg.get("difficulty").getAsString() : null;
                    
                    System.out.println("User " + username + " joining room " + code);
                    
                    // First check if the room exists and get its settings
                    RoomSettings roomSettings = rooms.getRoomSettings(code);
                    if (roomSettings == null) {
                        System.out.println("Room not found: " + code);
                        sendError("Room not found");
                        break;
                    }
                    
                    System.out.println("Room settings - Room: " + roomSettings.getRoom() + ", Difficulty: " + roomSettings.getDifficulty());
                    
                    // Enforce room/difficulty compatibility before attempting to join
                    if (room != null && !room.equals(roomSettings.getRoom())) {
                        sendError("Room selection mismatch. This room is configured for '" + roomSettings.getRoom() + " - " + roomSettings.getDifficulty() + "'. Please select the same room and difficulty to join.");
                        break;
                    }
                    if (difficulty != null && !difficulty.equals(roomSettings.getDifficulty())) {
                        sendError("Difficulty mismatch. This room is configured for '" + roomSettings.getRoom() + " - " + roomSettings.getDifficulty() + "'. Please select the same room and difficulty to join.");
                        break;
                    }

                    // Check if user can join and provide specific reason if not
                    String reason = rooms.checkJoinAllowed(code, username);
                    if (reason != null) {
                        System.out.println("Join rejected for " + username + ": " + reason);
                        sendError(reason);
                        break;
                    }

                    // If settings match or weren't specified, try to join
                    boolean ok = rooms.joinRoom(code, username, this, room, difficulty);
                    System.out.println("Join result: " + ok);
                    JsonObject resp = new JsonObject();
                    if (ok) {
                        currentRoomCode = code;
                        resp.addProperty("type", "joined");
                        resp.add("room", rooms.roomSnapshot(code));
                        send(resp);
                        rooms.broadcastRoomUpdate(code);
                        System.out.println("User " + username + " successfully joined room: " + code);
                    } else {
                        System.out.println("User " + username + " failed to join room: " + code);
                        sendError("Unable to join room");
                    }
                    break;
                }
                case "startGame": {
                    if (!msg.has("roomCode") || !msg.has("username")) {
                        sendError("Missing required fields for startGame");
                        break;
                    }
                    String code = msg.get("roomCode").getAsString();
                    String host = msg.get("username").getAsString();
                    identifyUser(host);
                    String room = msg.has("room") ? msg.get("room").getAsString() : null;
                    String difficulty = msg.has("difficulty") ? msg.get("difficulty").getAsString() : null;
                    boolean started = rooms.startGame(code, host, room, difficulty);
                    if (started) rooms.broadcastGameStarted(code, "manual");
                    else sendError("Cannot start game");
                    break;
                }
                case "setCapacity": {
                    if (!msg.has("roomCode") || !msg.has("username") || !msg.has("capacity")) {
                        sendError("Missing required fields for setCapacity");
                        break;
                    }
                    String code = msg.get("roomCode").getAsString();
                    String host = msg.get("username").getAsString();
                    int capacity;
                    try { capacity = msg.get("capacity").getAsInt(); } catch (Exception e) { capacity = 2; }
                    identifyUser(host);
                    rooms.setCapacity(code, host, capacity);
                    break;
                }
                case "submitAnswer": {
                    if (!msg.has("roomCode") || !msg.has("username") || !msg.has("answer") || !msg.has("elapsedMs")) {
                        sendError("Missing required fields for submitAnswer");
                        break;
                    }
                    String code = msg.get("roomCode").getAsString();
                    String user = msg.get("username").getAsString();
                    identifyUser(user);
                    String answer = msg.get("answer").getAsString();
                    long elapsed = msg.get("elapsedMs").getAsLong();
                    System.out.println("User " + user + " submitted answer: \"" + answer + "\" in " + elapsed + "ms");
                    rooms.submitAnswer(code, user, answer, elapsed);
                    break;
                }
                case "reaction": {
                    // Expect: roomCode, username, emoji
                    String code = msg.has("roomCode") ? msg.get("roomCode").getAsString() : currentRoomCode;
                    String user = msg.has("username") ? msg.get("username").getAsString() : this.username;
                    String emoji = msg.has("emoji") ? msg.get("emoji").getAsString() : null;
                    if (code == null || user == null || emoji == null || emoji.isEmpty()) {
                        sendError("Missing required fields for reaction");
                        break;
                    }
                    identifyUser(user);
                    currentRoomCode = code;
                    JsonObject outMsg = new JsonObject();
                    outMsg.addProperty("type", "reaction");
                    outMsg.addProperty("username", this.username);
                    outMsg.addProperty("emoji", emoji);
                    rooms.broadcastEvent(code, outMsg);
                    break;
                }
                case "chat": {
                    // Expect: roomCode, username, text (preset only)
                    String code = msg.has("roomCode") ? msg.get("roomCode").getAsString() : currentRoomCode;
                    String user = msg.has("username") ? msg.get("username").getAsString() : this.username;
                    String text = msg.has("text") ? msg.get("text").getAsString() : null;
                    if (code == null || user == null || text == null || text.isEmpty()) {
                        sendError("Missing required fields for chat");
                        break;
                    }
                    identifyUser(user);
                    currentRoomCode = code;
                    JsonObject outMsg = new JsonObject();
                    outMsg.addProperty("type", "chat");
                    outMsg.addProperty("username", this.username);
                    outMsg.addProperty("text", text);
                    rooms.broadcastEvent(code, outMsg);
                    break;
                }

                case "chatMessage": {
                    if (!msg.has("chatMessage")) {
                        sendError("Missing chatMessage field");
                        break;
                    }
                    JsonObject chatJson = msg.getAsJsonObject("chatMessage");
                    ChatMessage chatMessage = ChatMessage.fromJson(chatJson);
                    identifyUser(chatMessage.getSender());
                    currentRoomCode = msg.has("roomCode") ? msg.get("roomCode").getAsString() : currentRoomCode;
                
                    if (chatMessage.getType() == ChatMessage.MessageType.PUBLIC) {
                        rooms.broadcastToRoom(currentRoomCode, chatMessage);
                    } else if (chatMessage.getType() == ChatMessage.MessageType.PRIVATE) {
                        rooms.sendPrivateMessage(chatMessage.getReceiver(), chatMessage);
                    }
                    break;
                }

                case "getRoomState": {
                    String code = msg.has("roomCode") ? msg.get("roomCode").getAsString() : currentRoomCode;
                    if (code == null) {
                        sendError("No room code provided");
                        break;
                    }
                    
                    JsonObject resp = new JsonObject();
                    resp.addProperty("type", "roomUpdate");
                    resp.add("room", rooms.roomSnapshot(code));
                    send(resp);
                    System.out.println("Sent room state for room: " + code);
                    break;
                }
                case "leaveRoom": {
                    if (!msg.has("roomCode") || !msg.has("username")) {
                        sendError("Missing required fields for leaveRoom");
                        break;
                    }
                    String code = msg.get("roomCode").getAsString();
                    String user = msg.get("username").getAsString();
                    identifyUser(user);
                    rooms.leave(code, user, this);
                    break;
                }
                default:
                    sendError("Unknown type: " + type);
            }
        } catch (Exception ex) {
            System.err.println("Error processing message: " + ex.getMessage());
            ex.printStackTrace();
            // Send appropriate error message based on exception type
            if (ex instanceof NullPointerException) {
                sendError("Missing required fields in message");
            } else if (ex.getMessage().contains("JsonSyntaxException")) {
                // Don't send error for JSON parsing issues to avoid duplicate error messages
                System.err.println("JSON parsing error - ignoring message");
            } else {
                sendError("Invalid message format: " + ex.getMessage());
            }
        }
    }

    void send(JsonObject obj) { out.println(gson.toJson(obj)); }

    void sendError(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("type", "error");
        err.addProperty("message", message);
        send(err);
    }

    private void identifyUser(String username) {
        if (!identified) {
            this.username = username;
            this.identified = true;
            System.out.println("Client identified as: " + username + " from " + socket.getInetAddress());
        }
    }

    private JsonObject json(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        return o;
    }
}


