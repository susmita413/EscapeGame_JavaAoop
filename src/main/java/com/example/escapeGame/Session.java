package com.example.escapeGame;

import com.example.escapeGame.net.NetClient;
import java.util.HashSet;
import java.util.Set;

public class Session {
    private static User currentUser;
    private static String selectedRoom;
    private static String selectedDifficulty;
    private static String roomCode;
    private static NetClient netClient;
    private static Session instance;
    // Track completed levels per-user: username -> set of "room_difficulty" keys
    private static java.util.Map<String, java.util.Set<String>> userCompletedLevels = new java.util.HashMap<>();

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public String getSelectedRoom() {
        return selectedRoom;
    }

    public void setSelectedRoom(String room) {
        selectedRoom = room;
    }

    public String getSelectedDifficulty() {
        return selectedDifficulty;
    }

    public void setSelectedDifficulty(String difficulty) {
        selectedDifficulty = difficulty;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        Session.roomCode = roomCode;
    }

    public NetClient getNetClient() {
        return netClient;
    }

    public void setNetClient(NetClient client) {
        netClient = client;
    }
    
    public static void markLevelAsCompleted(String room, String difficulty) {
        String levelKey = room + "_" + difficulty;
        String user = getCurrentUsername();
        userCompletedLevels
                .computeIfAbsent(user, k -> new java.util.HashSet<>())
                .add(levelKey);
    }
    
    public static boolean isLevelCompleted(String room, String difficulty) {
        String levelKey = room + "_" + difficulty;
        String user = getCurrentUsername();
        java.util.Set<String> set = userCompletedLevels.get(user);
        if (set != null && set.contains(levelKey)) {
            return true;
        }
        // Fallback to persisted data in leaderboard.json so progress survives restarts
        try {
            java.util.List<PlayerStats> all = LeaderboardDataUtil.loadAll();
            for (PlayerStats ps : all) {
                if (ps.getUsername() != null && ps.getUsername().equalsIgnoreCase(user) && ps.getEscapes() != null) {
                    for (EscapeRecord er : ps.getEscapes()) {
                        if (room.equals(er.getRoom()) && difficulty.equals(er.getLevel())) {
                            // Cache in memory to speed up next call
                            userCompletedLevels
                                    .computeIfAbsent(user, k -> new java.util.HashSet<>())
                                    .add(levelKey);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    public static boolean isLevelUnlocked(String room, String difficulty) {
        if (difficulty.equals("Easy")) {
            return true;
        } else if (difficulty.equals("Medium")) {
            return isLevelCompleted(room, "Easy");
        } else if (difficulty.equals("Hard")) {
            return isLevelCompleted(room, "Medium");
        }
        return false;
    }

    private static String getCurrentUsername() {
        User u = getCurrentUser();
        return (u != null && u.getUsername() != null) ? u.getUsername() : "guest";
    }
}
