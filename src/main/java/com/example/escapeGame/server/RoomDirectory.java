package com.example.escapeGame.server;

import com.example.escapeGame.ChatMessage;
import com.example.escapeGame.Puzzle;
import com.example.escapeGame.PuzzleDataUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class RoomSettings {
    private final String room;
    private final String difficulty;
    
    public RoomSettings(String room, String difficulty) {
        this.room = room;
        this.difficulty = difficulty;
    }
    
    public String getRoom() { return room; }
    public String getDifficulty() { return difficulty; }
}

public class RoomDirectory {
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    String createRoom(String host) {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (rooms.containsKey(code));
        RoomState rs = new RoomState(code, host);
        rooms.put(code, rs);
        System.out.println("Room " + code + " created with host: " + host);
        return code;
    }

    void addClientToRoom(String code, String username, ClientHandler client) {
        RoomState rs = rooms.get(code);
        if (rs != null) rs.addClient(username, client);
    }

    void setSelection(String code, String room, String difficulty) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        rs.setSelection(room, difficulty);
    }

    // Return a human-readable reason why a user cannot join the room; null if allowed
    String checkJoinAllowed(String code, String username) {
        RoomState rs = rooms.get(code);
        if (rs == null) return "Room not found";
        return rs.checkJoinAllowed(username);
    }

    boolean joinRoom(String code, String username, ClientHandler client, String room, String difficulty) {
        RoomState rs = rooms.get(code);
        if (rs == null) {
            System.out.println("Room " + code + " not found for user " + username);
            return false;
        }
        
        System.out.println("User " + username + " attempting to join room " + code + " (current players: " + rs.playerCount() + ")");
        // Enforce room/difficulty compatibility
        String configuredRoom = rs.getSelectedRoom();
        String configuredDifficulty = rs.getSelectedDifficulty();
        if (room != null && !room.equals(configuredRoom)) {
            System.out.println("Join rejected for " + username + ": room mismatch (player=" + room + ", room=" + configuredRoom + ")");
            return false;
        }
        if (difficulty != null && !difficulty.equals(configuredDifficulty)) {
            System.out.println("Join rejected for " + username + ": difficulty mismatch (player=" + difficulty + ", room=" + configuredDifficulty + ")");
            return false;
        }

        boolean ok = rs.addPlayer(username, client);
        if (ok) {
            System.out.println("User " + username + " successfully added to room " + code);
            // Do not overwrite room settings on join; host's settings remain authoritative
            maybeAutoStart(code);
        } else {
            System.out.println("Failed to add user " + username + " to room " + code);
        }
        return ok;
    }
    
    // Get room settings for a given room code
    public RoomSettings getRoomSettings(String code) {
        RoomState rs = rooms.get(code);
        if (rs == null) return null;
        return new RoomSettings(rs.getSelectedRoom(), rs.getSelectedDifficulty());
    }

    void leave(String code, String username, ClientHandler client) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        rs.removePlayer(username, client);
        if (rs.isEmpty()) {
            rooms.remove(code);
            System.out.println("Room " + code + " deleted (empty)");
        } else {
            System.out.println("User " + username + " left room " + code + " (remaining: " + rs.playerCount() + ")");
            broadcastRoomUpdate(code);
        }
    }

    boolean startGame(String code, String host, String selectedRoom, String selectedDifficulty) {
        RoomState rs = rooms.get(code);
        if (rs == null) return false;
        if (!rs.isHost(host)) return false;
        // Enforce host-selected desired capacity (2-4)
        if (rs.playerCount() < rs.getDesiredCapacity()) return false;

        // Load puzzles from puzzles.json based on selected room/difficulty
        try {
            List<Puzzle> all = PuzzleDataUtil.loadPuzzles();
            if (all.isEmpty()) {
                System.err.println("No puzzles loaded - cannot start game");
                return false;
            }
            
            rs.setSelection(selectedRoom, selectedDifficulty);
            String roomFilter = rs.getSelectedRoom();
            String difficultyFilter = rs.getSelectedDifficulty();
            List<Puzzle> filtered = new ArrayList<>();
            for (Puzzle p : all) {
                if (roomFilter.equals(p.getRoom()) && difficultyFilter.equals(p.getDifficulty())) {
                    filtered.add(p);
                }
            }
            // Fallbacks to avoid empty question set (e.g., if a room like "Math" has no entries)
            if (filtered.isEmpty()) {
                // Try any room with the chosen difficulty
                for (Puzzle p : all) {
                    if (difficultyFilter.equals(p.getDifficulty())) filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                // Last resort: use all available puzzles
                filtered.addAll(all);
            }
            Collections.shuffle(filtered, random);
            rs.selectQuestions(filtered.subList(0, Math.min(10, filtered.size())));
            rs.markStarted();
            System.out.println("Game started in room " + code + " by " + host + " with " + rs.playerCount() + " players");
            return true;
        } catch (Exception e) {
            System.err.println("Error starting game in room " + code + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void maybeAutoStart(String code) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        int target = rs.getDesiredCapacity();
        if (rs.playerCount() >= target && !rs.isInProgress()) {
            System.out.println("Room " + code + " reached desired capacity (" + target + ") with " + rs.playerCount() + " players, starting in 3 seconds");
            // Announce and give a short window to leave
            broadcastGameStarting(code, "Required players joined (" + target + "). Game starts in 3 seconds unless someone clicks Back.");
            new Timer(true).schedule(new TimerTask() {
                @Override public void run() {
                    RoomState cur = rooms.get(code);
                    if (cur != null && cur.playerCount() >= cur.getDesiredCapacity() && !cur.isInProgress()) {
                        startGame(code, cur.getHost(), cur.getSelectedRoom(), cur.getSelectedDifficulty());
                        broadcastGameStarted(code, "autoCapacity");
                    }
                }
            }, 3000);
        }
    }

    void submitAnswer(String code, String username, String answer, long elapsedMs) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        rs.submitAnswer(username, answer, elapsedMs);
    }

    void broadcastRoomUpdate(String code) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "roomUpdate");
        msg.add("room", roomSnapshot(code));
        rs.broadcast(msg);
    }

    void broadcastGameStarted(String code, String reason) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "gameStarted");
        msg.addProperty("roomCode", code);
        if (reason != null) msg.addProperty("reason", reason);
        rs.broadcast(msg);
        // Give clients a brief moment to load the new scene and register handlers
        new Timer(true).schedule(new TimerTask() {
            @Override public void run() { sendQuestion(code); }
        }, 500);
    }

    // Single implementation of broadcastGameStarting
    void broadcastGameStarting(String code, String message) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "gameStarting");
        msg.addProperty("message", message);
        rs.broadcast(msg);
    }

    void sendQuestion(String code) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "question");
        msg.addProperty("index", rs.getCurrentIndex() + 1);
        msg.addProperty("total", rs.getTotalQuestions());
        Puzzle q = rs.getCurrentQuestion();
        int timeSec;
        if (q != null) {
            msg.addProperty("text", q.getQuestion());
            // Set time based on difficulty level
            String difficulty = rs.getSelectedDifficulty();
            switch (difficulty.toLowerCase()) {
                case "easy":
                    timeSec = 90;
                    break;
                case "medium":
                    timeSec = 120;
                    break;
                case "hard":
                    timeSec = 150;
                    break;
                default:
                    timeSec = 90; // Default to easy if difficulty is unknown
                    break;
            }
            System.out.println("Question time set to " + timeSec + " seconds for difficulty: " + difficulty);
        } else {
            msg.addProperty("text", "No question available for selected room/difficulty.");
            timeSec = 3;
        }
        msg.addProperty("timeSec", timeSec);
        rs.broadcast(msg);
        // Reset round answers and start round timer
        rs.resetRoundState();
        long roundStart = System.currentTimeMillis();
        int finalTimeSec = timeSec;
        rs.startRoundTimer(finalTimeSec, () -> {
            long roundDuration = System.currentTimeMillis() - roundStart;
            // Finalize team scoring for this round
            rs.finalizeRoundAndScore(roundDuration);
            if (rs.advanceToNextQuestion()) {
                sendQuestion(code);
            } else {
                broadcastGameOver(code);
            }
        });
    }

    JsonObject roomSnapshot(String code) {
        RoomState rs = rooms.get(code);
        JsonObject obj = new JsonObject();
        obj.addProperty("code", code);
        if (rs != null) {
            obj.addProperty("host", rs.getHost());
            obj.addProperty("capacity", rs.getDesiredCapacity());
            obj.addProperty("canStart", rs.playerCount() >= rs.getDesiredCapacity() && !rs.isInProgress());
            JsonArray arr = new JsonArray();
            for (String p : rs.getPlayers()) {
                arr.add(p);
            }
            obj.add("players", arr);
        }
        return obj;
    }

    // Host can change the desired capacity (2/3/4). Broadcast update and optionally auto-start.
    void setCapacity(String code, String host, int capacity) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        if (!rs.isHost(host)) return;
        rs.setDesiredCapacity(capacity);
        System.out.println("Host " + host + " set capacity for room " + code + " to " + capacity);
        broadcastRoomUpdate(code);
        // Inform players so they can choose to leave
        broadcastGameStarting(code, "Host set players needed to " + capacity + ". Click 'Back' to leave, otherwise the game will auto-start when the room reaches " + capacity + ".");
        // If we already reached capacity, give a brief window and then start automatically
        if (rs.playerCount() >= rs.getDesiredCapacity() && !rs.isInProgress()) {
            new Timer(true).schedule(new TimerTask() {
                @Override public void run() {
                    RoomState cur = rooms.get(code);
                    if (cur != null && cur.playerCount() >= cur.getDesiredCapacity() && !cur.isInProgress()) {
                        startGame(code, host, cur.getSelectedRoom(), cur.getSelectedDifficulty());
                        broadcastGameStarted(code, "autoCapacityChange");
                    }
                }
            }, 3000);
        }
    }

    void broadcastGameOver(String code) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "gameOver");
        msg.add("scores", rs.getScoresJson());
        msg.addProperty("threshold", 50);
        System.out.println("Broadcasting game over to all players in room " + code);
        rs.broadcast(msg);
    }

    // Generic broadcast for simple events like reaction/chat
    void broadcastEvent(String code, JsonObject msg) {
        RoomState rs = rooms.get(code);
        if (rs == null) return;
        rs.broadcast(msg);
    }


    // Broadcast chat message to all players in a room (except sender)
void broadcastToRoom(String roomCode, ChatMessage message) {
    RoomState rs = rooms.get(roomCode);
    if (rs == null) return;
    
    JsonObject msg = new JsonObject();
    msg.addProperty("type", "chatMessage");
    msg.add("chatMessage", message.toJson());
    
    System.out.println("Broadcasting chat message from " + message.getSender() + " to room " + roomCode + " (excluding sender)");
    rs.broadcastExcept(msg, message.getSender());
}

// Send private message to specific receiver (not back to sender)
void sendPrivateMessage(String receiver, ChatMessage message) {
    RoomState rs = null;
    // Find which room the receiver is in
    for (Map.Entry<String, RoomState> entry : rooms.entrySet()) {
        if (entry.getValue().getPlayers().contains(receiver)) {
            rs = entry.getValue();
            break;
        }
    }
    
    if (rs == null) {
        System.out.println("Receiver " + receiver + " not found in any room");
        return;
    }
    
    JsonObject msg = new JsonObject();
    msg.addProperty("type", "chatMessage");
    msg.add("chatMessage", message.toJson());
    
    System.out.println("Sending private message from " + message.getSender() + " to " + receiver + " (excluding sender)");
    rs.sendToPlayer(receiver, msg);
}
    
}


