package com.example.escapeGame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardDataUtil {
    private static final String FILE_NAME = "src/main/resources/leaderboard.json";

    private static Path getDataPath() {
        Path dir = Paths.get(System.getProperty("user.dir"), "data");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("leaderboard.json");
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapters.LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();

    public static List<PlayerStats> loadAll() {
        try {
            Path path = getDataPath();
            if (!Files.exists(path)) return new ArrayList<>();
            String json = Files.readString(path);
            if (json == null || json.trim().isEmpty()) return new ArrayList<>();
            Type listType = new TypeToken<ArrayList<PlayerStats>>(){}.getType();
            List<PlayerStats> stats = gson.fromJson(json, listType);
            return stats != null ? stats : new ArrayList<>();
        } catch (Exception e) {
            // If file is invalid or unreadable, start fresh
            return new ArrayList<>();
        }
    }

    public static void saveAll(List<PlayerStats> all) {
        try {
            Path path = getDataPath();
            Files.createDirectories(path.getParent());
            try (Writer w = new FileWriter(path.toFile())) {
                gson.toJson(all, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static PlayerStats getOrCreate(List<PlayerStats> all, String username) {
        for (PlayerStats ps : all) {
            if (ps.getUsername() != null && ps.getUsername().equalsIgnoreCase(username)) return ps;
        }
        PlayerStats ps = new PlayerStats(username);
        all.add(ps);
        return ps;
    }

    public static void recordGame(String username, boolean multiplayer, boolean escaped, String room, String level, LocalDateTime when) {
        // Backward-compatible method without score
        recordGame(username, multiplayer, escaped, room, level, when, null);
    }

    public static void recordGame(String username, boolean multiplayer, boolean escaped, String room, String level, LocalDateTime when, Integer score) {
        if (username == null || username.isEmpty()) return;
        List<PlayerStats> all = loadAll();
        PlayerStats ps = getOrCreate(all, username);
        if (multiplayer) ps.setMultiPlays(ps.getMultiPlays() + 1); else ps.setSoloPlays(ps.getSoloPlays() + 1);
        if (escaped && room != null && level != null) {
            List<EscapeRecord> escapes = ps.getEscapes();
            if (escapes == null) { escapes = new ArrayList<>(); ps.setEscapes(escapes); }
            EscapeRecord er = new EscapeRecord(room, level, when);
            er.setScore(score);
            er.setMultiplayer(multiplayer);
            escapes.add(er);
        }
        saveAll(all);
    }

    public static void deleteStatsForPlayer(String username) {
        List<PlayerStats> all = loadAll();
        all.removeIf(ps -> ps.getUsername() != null && ps.getUsername().equalsIgnoreCase(username));
        saveAll(all);
    }
}
