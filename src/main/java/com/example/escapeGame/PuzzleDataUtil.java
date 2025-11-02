package com.example.escapeGame;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PuzzleDataUtil {
    private static final String PUZZLE_FILE = "src/main/resources/puzzles.json";
    
    private static Path getPuzzleDataPath() {
        // Use project-level data directory for writable puzzles.json
        Path dir = Paths.get(System.getProperty("user.dir"), "data");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        Path path = dir.resolve("puzzles.json");
        if (!Files.exists(path)) {
            // Seed from classpath resource if available; otherwise create empty list
            try (InputStream in = PuzzleDataUtil.class.getClassLoader().getResourceAsStream("puzzles.json")) {
                if (in != null) {
                    Files.copy(in, path);
                } else {
                    Files.writeString(path, "[]");
                }
            } catch (Exception ignored) {}
        }
        return path;
    }

    public static List<Puzzle> loadPuzzles() {
        try {
            Path path = getPuzzleDataPath();
            if (!Files.exists(path)) {
                System.out.println("Puzzle file not found: " + path);
                return new ArrayList<>();
            }
            String json = Files.readString(path);
            Type listType = new TypeToken<ArrayList<Puzzle>>(){}.getType();
            List<Puzzle> puzzles = new Gson().fromJson(json, listType);
            if (puzzles == null) {
                System.out.println("Failed to parse puzzles.json - returning empty list");
                return new ArrayList<>();
            }
            System.out.println("Loaded " + puzzles.size() + " puzzles from puzzles.json");
            return puzzles;
        } catch (Exception e) {
            System.err.println("Error loading puzzles: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void savePuzzles(List<Puzzle> puzzles) {
        try {
            Path path = getPuzzleDataPath();
            // Ensure parent directories exist
            Files.createDirectories(path.getParent());
            try (Writer writer = new FileWriter(path.toFile())) {
                new Gson().toJson(puzzles, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Puzzle createPuzzle(Puzzle puzzle) {
        List<Puzzle> puzzles = loadPuzzles();
        int nextId = puzzles.stream().map(Puzzle::getId).max(Comparator.naturalOrder()).orElse(0) + 1;
        puzzle.setId(nextId);
        puzzles.add(puzzle);
        savePuzzles(puzzles);
        return puzzle;
    }

    public static Optional<Puzzle> getPuzzleById(int id) {
        return loadPuzzles().stream().filter(p -> p.getId() == id).findFirst();
    }

    public static void updatePuzzle(Puzzle updated) {
        List<Puzzle> puzzles = loadPuzzles();
        for (int i = 0; i < puzzles.size(); i++) {
            if (puzzles.get(i).getId() == updated.getId()) {
                puzzles.set(i, updated);
                savePuzzles(puzzles);
                return;
            }
        }
    }

    public static void deletePuzzle(int id) {
        List<Puzzle> puzzles = loadPuzzles();
        puzzles.removeIf(p -> p.getId() == id);
        savePuzzles(puzzles);
    }
}