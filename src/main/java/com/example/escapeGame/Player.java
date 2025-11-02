package com.example.escapeGame;

public class Player {
    private String username;
    private boolean isHost;
    private int score;
    
    public Player(String username, boolean isHost) {
        this.username = username;
        this.isHost = isHost;
        this.score = 0;
    }
    
    public void addScore(int points) {
        this.score += points;
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public boolean isHost() { return isHost; }
    public int getScore() { return score; }
    public void setUsername(String username) { this.username = username; }
    public void setHost(boolean host) { isHost = host; }
    public void setScore(int score) { this.score = score; }
}
