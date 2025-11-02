package com.example.escapeGame;

import java.time.LocalDateTime;

public class EscapeRecord {
    private String room;
    private String level;
    private LocalDateTime date;
    // Optional fields (may be null for older data)
    private Integer score;          // Final score for this game
    private Boolean multiplayer;    // true if recorded from a competitive game

    public EscapeRecord() {}

    public EscapeRecord(String room, String level, LocalDateTime date) {
        this.room = room;
        this.level = level;
        this.date = date;
    }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Boolean getMultiplayer() { return multiplayer; }
    public void setMultiplayer(Boolean multiplayer) { this.multiplayer = multiplayer; }
}
