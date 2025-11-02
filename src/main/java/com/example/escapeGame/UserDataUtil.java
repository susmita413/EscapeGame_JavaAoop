package com.example.escapeGame;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UserDataUtil {
    private static final String USER_FILE = "src/main/resources/users.json";
    
    private static Path getUserDataPath() {
        // Use project-level data directory for writable users.json
        Path dir = Paths.get(System.getProperty("user.dir"), "data");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        Path path = dir.resolve("users.json");
        if (!Files.exists(path)) {
            // Seed from classpath resource if available; otherwise create empty list
            try (InputStream in = UserDataUtil.class.getClassLoader().getResourceAsStream("users.json")) {
                if (in != null) {
                    Files.copy(in, path);
                } else {
                    Files.writeString(path, "[]");
                }
            } catch (Exception ignored) {}
        }
        return path;
    }

    public static List<User> loadUsers() {
        try {
            Path path = getUserDataPath();
            if (Files.exists(path)) {
                String json = Files.readString(path);
                if (json == null || json.trim().isEmpty()) return new ArrayList<>();
                Type userListType = new TypeToken<ArrayList<User>>() {}.getType();
                List<User> list = new Gson().fromJson(json, userListType);
                return list != null ? list : new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveUsers(List<User> users) {
        try {
            Path path = getUserDataPath();
            // Ensure parent directories exist
            Files.createDirectories(path.getParent());
            try (Writer writer = new FileWriter(path.toFile())) {
                new Gson().toJson(users, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isUsernameTaken(String username) {
        for (User u : loadUsers()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public static void addUser(User newUser) {
        List<User> users = loadUsers();
        users.add(newUser);
        saveUsers(users);
    }

    public static User getUserByUsername(String username) {
        for (User u : loadUsers()) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    public static void updateUser(User updatedUser) {
        List<User> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(updatedUser.getUsername())) {
                users.set(i, updatedUser);
                saveUsers(users);
                return;
            }
        }
    }
}
