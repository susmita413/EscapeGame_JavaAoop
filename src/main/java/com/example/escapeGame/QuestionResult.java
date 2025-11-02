package com.example.escapeGame;

public class QuestionResult {
    private String username;
    private boolean isCorrect;
    private long responseTimeMs;
    private int score;
    
    public QuestionResult(String username, boolean isCorrect, long responseTimeMs, int score) {
        this.username = username;
        this.isCorrect = isCorrect;
        this.responseTimeMs = responseTimeMs;
        this.score = score;
    }
    
    // Getters
    public String getUsername() { return username; }
    public boolean isCorrect() { return isCorrect; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public int getScore() { return score; }
}
