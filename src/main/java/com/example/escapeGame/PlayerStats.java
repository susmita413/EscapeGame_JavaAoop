package com.example.escapeGame;

import java.util.ArrayList;
import java.util.List;

public class PlayerStats {
    private String username;
    private int soloPlays;
    private int multiPlays;
    private List<EscapeRecord> escapes;

    public PlayerStats() {
        this.escapes = new ArrayList<>();
    }

    public PlayerStats(String username) {
        this.username = username;
        this.escapes = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getSoloPlays() { return soloPlays; }
    public void setSoloPlays(int soloPlays) { this.soloPlays = soloPlays; }

    public int getMultiPlays() { return multiPlays; }
    public void setMultiPlays(int multiPlays) { this.multiPlays = multiPlays; }

    public List<EscapeRecord> getEscapes() { return escapes; }
    public void setEscapes(List<EscapeRecord> escapes) { this.escapes = escapes; }

    public void incSolo() { this.soloPlays++; }
    public void incMulti() { this.multiPlays++; }

    public String getPrimaryModeLabel() {
        if (multiPlays > soloPlays) return "Competitive";
        if (soloPlays > multiPlays) return "Solo";
        return "Balanced";
    }
}
