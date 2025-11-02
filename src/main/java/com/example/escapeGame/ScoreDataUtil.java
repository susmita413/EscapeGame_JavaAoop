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
import java.util.Comparator;
import java.util.List;

public class ScoreDataUtil {
    private static final String SCORE_FILE = "src/main/resources/scores.json";
    
    private static Path getScoreDataPath() {
        Path dir = Paths.get(System.getProperty("user.dir"), "data");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("scores.json");
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapters.LocalDateTimeAdapter())
            .create();

    public static List<PlayerScore> loadScores() {
        try {
            Path path = getScoreDataPath();
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            String json = Files.readString(path);
            Type listType = new TypeToken<ArrayList<PlayerScore>>(){}.getType();
            List<PlayerScore> scores = gson.fromJson(json, listType);
            return scores != null ? scores : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveScores(List<PlayerScore> scores) {
        try {
            Path path = getScoreDataPath();
            // Ensure parent directories exist
            Files.createDirectories(path.getParent());
            try (Writer writer = new FileWriter(path.toFile())) {
                gson.toJson(scores, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addOrUpdateScore(PlayerScore newScore) {
        List<PlayerScore> scores = loadScores();
        boolean updated = false;
        for (int i = 0; i < scores.size(); i++) {
            PlayerScore s = scores.get(i);
            if (s.getPlayer().equalsIgnoreCase(newScore.getPlayer())) {
                if (newScore.getScore() > s.getScore()) {
                    scores.set(i, newScore);
                }
                updated = true;
                break;
            }
        }
        if (!updated) {
            scores.add(newScore);
        }
        saveScores(scores);
    }

    public static void deleteScoreForPlayer(String player) {
        List<PlayerScore> scores = loadScores();
        scores.removeIf(s -> s.getPlayer().equalsIgnoreCase(player));
        saveScores(scores);
    }

    public static List<PlayerScore> topScores() {
        List<PlayerScore> scores = loadScores();
        scores.sort(Comparator.comparingInt(PlayerScore::getScore).reversed());
        return scores;
    }
}
