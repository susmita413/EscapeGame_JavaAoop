package com.example.escapeGame;

import java.time.LocalDateTime;

public class PlayerScore {
    private String player;
    private int score;
    private long timeSeconds;
    private LocalDateTime date;

    public PlayerScore() {}

    public PlayerScore(String player, int score, long timeSeconds, LocalDateTime date) {
        this.player = player;
        this.score = score;
        this.timeSeconds = timeSeconds;
        this.date = date;
    }

    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(long timeSeconds) { this.timeSeconds = timeSeconds; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}