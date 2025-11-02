package com.example.escapeGame;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final ConcurrentMap<String, ClientRoom> rooms = new ConcurrentHashMap<>();
    
    private RoomManager() {}
    
    public static RoomManager getInstance() {
        return instance;
    }
    
    public String createRoom(String hostUsername) {
        String roomCode = generateRoomCode();
        ClientRoom room = new ClientRoom(roomCode, hostUsername);
        rooms.put(roomCode, room);
        return roomCode;
    }
    
    public boolean joinRoom(String roomCode, String username) {
        System.out.println("RoomManager.joinRoom called with roomCode: " + roomCode + ", username: " + username);
        ClientRoom room = rooms.get(roomCode);
        System.out.println("Room found: " + (room != null));
        if (room != null) {
            System.out.println("Room state: " + room.getGameState() + ", Player count: " + room.getPlayerCount());
            boolean result = room.addPlayer(username);
            System.out.println("addPlayer result: " + result);
            return result;
        }
        System.out.println("Room not found, returning false");
        return false;
    }
    
    public boolean leaveRoom(String roomCode, String username) {
        ClientRoom room = rooms.get(roomCode);
        if (room != null) {
            boolean isEmpty = room.removePlayer(username);
            if (isEmpty) {
                rooms.remove(roomCode);
            }
            return true;
        }
        return false;
    }
    
    public ClientRoom getRoom(String roomCode) {
        return rooms.get(roomCode);
    }
    
    public boolean startGame(String roomCode, String hostUsername) {
        ClientRoom room = rooms.get(roomCode);
        if (room != null && room.isHost(hostUsername)) {
            room.startGame();
            return true;
        }
        return false;
    }
    
    public void submitAnswer(String roomCode, String username, String answer, long responseTimeMs) {
        ClientRoom room = rooms.get(roomCode);
        if (room != null) {
            room.submitAnswer(username, answer, responseTimeMs);
        }
    }
    
    public boolean nextQuestion(String roomCode, String hostUsername) {
        ClientRoom room = rooms.get(roomCode);
        if (room != null && room.isHost(hostUsername)) {
            return room.nextQuestion();
        }
        return false;
    }
    
    private String generateRoomCode() {
        Random random = new Random();
        String roomCode;
        do {
            roomCode = String.format("%06d", random.nextInt(1000000));
        } while (rooms.containsKey(roomCode));
        return roomCode;
    }
    
    public void cleanupEmptyRooms() {
        rooms.entrySet().removeIf(entry -> entry.getValue().getPlayers().isEmpty());
    }
    
    public java.util.Set<String> getAllRoomCodes() {
        return rooms.keySet();
    }
}
