package com.example.escapeGame;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardController {
    // Players summary table
    @FXML private TableView<PlayerStats> playersTable;
    @FXML private TableColumn<PlayerStats, String> colSummaryPlayer;
    @FXML private TableColumn<PlayerStats, Number> colSolo;
    @FXML private TableColumn<PlayerStats, Number> colMulti;
    @FXML private TableColumn<PlayerStats, String> colPrimaryMode;
    @FXML private TableColumn<PlayerStats, String> colHardAchievements;
    @FXML private Button btnReset;

    // Escapes table (flattened across users)
    @FXML private TableView<UserEscape> escapesTable;
    @FXML private TableColumn<UserEscape, String> colEscapeUser;
    @FXML private TableColumn<UserEscape, String> colEscapeRoom;
    @FXML private TableColumn<UserEscape, String> colEscapeLevel;
    @FXML private TableColumn<UserEscape, String> colEscapeMode;
    @FXML private TableColumn<UserEscape, String> colEscapeHighScore;
    @FXML private TableColumn<UserEscape, String> colEscapeDate;

    private final ObservableList<PlayerStats> playerRows = FXCollections.observableArrayList();
    private final ObservableList<UserEscape> escapeRows = FXCollections.observableArrayList();
    // Cache for achievements column (username -> text)
    private final java.util.Map<String, String> hardAchievementsByUser = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        // Players
        colSummaryPlayer.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        colSolo.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getSoloPlays()));
        colMulti.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getMultiPlays()));
        colPrimaryMode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPrimaryModeLabel()));
        if (colHardAchievements != null) {
            colHardAchievements.setCellValueFactory(cd -> new SimpleStringProperty(
                    hardAchievementsByUser.getOrDefault(cd.getValue().getUsername(), "")));
        }

        // Admin-only: Reset Player button is enabled for admins, disabled for normal users
        if (btnReset != null) {
            boolean admin = isAdmin();
            btnReset.setDisable(!admin);
        }

        // Escapes
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colEscapeUser.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUser()));
        colEscapeRoom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRoom()));
        colEscapeLevel.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getLevel() == null ? "" :
                        ("Hard".equalsIgnoreCase(cd.getValue().getLevel()) ? "Hard 🏆" : cd.getValue().getLevel())));
        if (colEscapeMode != null) {
            colEscapeMode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMode()));
        }
        if (colEscapeHighScore != null) {
            colEscapeHighScore.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHighScoreText()));
        }
        colEscapeDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDate() == null ? "" : cd.getValue().getDate().format(fmt)));

        reload();
    }

    @FXML
    private void handleResetPlayer() {
        // Enforce admin check even if UI state was altered
        if (!isAdmin()) {
            return;
        }
        PlayerStats selected = playersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        LeaderboardDataUtil.deleteStatsForPlayer(selected.getUsername());
        reload();
    }

    @FXML
    private void goBack() throws IOException {
        if (isAdmin()) {
            LogIn.changeScene("com/example/escapeGame/adminPanel.fxml", "Admin Panel");
        } else {
            LogIn.changeScene("com/example/escapeGame/room.fxml", "Choose Room");
        }
    }

    private void reload() {
        List<PlayerStats> all = LeaderboardDataUtil.loadAll();

        // Build universe of rooms that have Hard difficulty available
        java.util.Set<String> hardRoomsUniverse = new java.util.HashSet<>(); // lower-cased room names
        try {
            List<Puzzle> puzzles = PuzzleDataUtil.loadPuzzles();
            for (Puzzle p : puzzles) {
                if (p.getRoom() != null && p.getDifficulty() != null && "Hard".equalsIgnoreCase(p.getDifficulty())) {
                    hardRoomsUniverse.add(p.getRoom().toLowerCase());
                }
            }
        } catch (Exception ignored) {}

        // Compute per-user Hard-level flag, per (user,room,level) best MP & Solo scores, and per-user best MP score
        java.util.Map<String, Boolean> userHasHard = new java.util.HashMap<>();
        java.util.Map<String, Integer> bestMpScoreByKey = new java.util.HashMap<>(); // key: user|room|level
        java.util.Map<String, Integer> bestSoloScoreByKey = new java.util.HashMap<>(); // key: user|room|level
        java.util.Map<String, Integer> bestMpScoreByUser = new java.util.HashMap<>(); // key: user
        hardAchievementsByUser.clear();
        for (PlayerStats ps : all) {
            if (ps.getEscapes() == null) {
                userHasHard.put(ps.getUsername(), false);
                hardAchievementsByUser.put(ps.getUsername(), "");
                continue;
            }
            boolean hasHard = false;
            int userBestMp = 0;
            java.util.Set<String> playerHardLower = new java.util.HashSet<>();
            java.util.List<String> playerHardRooms = new ArrayList<>();
            for (EscapeRecord er : ps.getEscapes()) {
                if (er.getLevel() != null && "Hard".equalsIgnoreCase(er.getLevel())) {
                    hasHard = true;
                    if (er.getRoom() != null) {
                        String lc = er.getRoom().toLowerCase();
                        if (!playerHardLower.contains(lc)) {
                            playerHardLower.add(lc);
                            playerHardRooms.add(er.getRoom());
                        }
                    }
                }
                // Consider scores for both modes
                if ( er.getScore() != null ) {
                    String key = (ps.getUsername() + "|" + er.getRoom() + "|" + er.getLevel()).toLowerCase();
                    if (Boolean.TRUE.equals(er.getMultiplayer())) {
                        bestMpScoreByKey.merge(key, er.getScore(), Math::max);
                        if (er.getScore() > userBestMp) userBestMp = er.getScore();
                    } else {
                        bestSoloScoreByKey.merge(key, er.getScore(), Math::max);
                    }
                }
            }
            userHasHard.put(ps.getUsername(), hasHard);
            bestMpScoreByUser.put(ps.getUsername().toLowerCase(), userBestMp);

            // Compose achievements text: crowns per Hard room (abbreviated), or three-crown BOSS if all Hard rooms are completed
            String achievementsText = "";
            if (!playerHardRooms.isEmpty()) {
                boolean isBoss = !hardRoomsUniverse.isEmpty() && playerHardLower.containsAll(hardRoomsUniverse);
                if (isBoss) {
                    achievementsText = "\uD83D\uDC51\uD83D\uDC51\uD83D\uDC51"; // 👑👑👑
                } else {
                    // Sort for stable display and join with commas
                    java.util.Collections.sort(playerHardRooms, String.CASE_INSENSITIVE_ORDER);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < playerHardRooms.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("\uD83D\uDC51 ").append(shortenRoom(playerHardRooms.get(i))); // 👑 + abbreviated room
                    }
                    achievementsText = sb.toString();
                }
            }
            hardAchievementsByUser.put(ps.getUsername(), achievementsText);
        }

        // Sort players summary as requested
        all.sort((a, b) -> {
            int aEsc = (a.getEscapes() == null) ? 0 : a.getEscapes().size();
            int bEsc = (b.getEscapes() == null) ? 0 : b.getEscapes().size();
            int byEscapes = Integer.compare(bEsc, aEsc); // desc
            if (byEscapes != 0) return byEscapes;
            int aBest = bestMpScoreByUser.getOrDefault(a.getUsername().toLowerCase(), 0);
            int bBest = bestMpScoreByUser.getOrDefault(b.getUsername().toLowerCase(), 0);
            int byBestMp = Integer.compare(bBest, aBest); // desc
            if (byBestMp != 0) return byBestMp;
            return a.getUsername().compareToIgnoreCase(b.getUsername());
        });
        playerRows.setAll(all);
        playersTable.setItems(playerRows);

        // Build rows: include both solo and multiplayer escapes; show Mode and compute per-(user,room,level) MP high score
        List<UserEscape> flat = new ArrayList<>();
        for (PlayerStats ps : all) {
            if (ps.getEscapes() == null) continue;
            for (EscapeRecord er : ps.getEscapes()) {
                boolean isMp = Boolean.TRUE.equals(er.getMultiplayer());
                String key = (ps.getUsername() + "|" + er.getRoom() + "|" + er.getLevel()).toLowerCase();
                Integer hi = isMp ? bestMpScoreByKey.get(key) : bestSoloScoreByKey.get(key);
                boolean champion = Boolean.TRUE.equals(userHasHard.get(ps.getUsername()));
                flat.add(new UserEscape(ps.getUsername(), er.getRoom(), er.getLevel(), er.getDate(), hi, champion, isMp));
            }
        }
        // Sort by: Room (custom), Level (Hard > Medium > Easy), High Score desc, Date desc (most recent first), then Player name asc
        flat.sort((a, b) -> {
            int byRoom = Integer.compare(roomOrderIndex(a.getRoom()), roomOrderIndex(b.getRoom()));
            if (byRoom != 0) return byRoom;
            int byLevel = Integer.compare(levelOrderIndex(a.getLevel()), levelOrderIndex(b.getLevel()));
            if (byLevel != 0) return byLevel;
            int as = a.getHighScore() == null ? 0 : a.getHighScore();
            int bs = b.getHighScore() == null ? 0 : b.getHighScore();
            int byScore = Integer.compare(bs, as); // desc
            if (byScore != 0) return byScore;
            java.time.LocalDateTime ad = a.getDate();
            java.time.LocalDateTime bd = b.getDate();
            if (ad != null && bd != null) {
                int byDate = bd.compareTo(ad); // desc (most recent first)
                if (byDate != 0) return byDate;
            } else if (ad == null && bd != null) {
                return 1; // nulls last
            } else if (ad != null && bd == null) {
                return -1;
            }
            return a.getUser().compareToIgnoreCase(b.getUser());
        });

        escapeRows.setAll(flat);
        escapesTable.setItems(escapeRows);
    }

    private boolean isAdmin() {
        User u = Session.getCurrentUser();
        return u != null && u.getRole() != null && u.getRole().equalsIgnoreCase("admin");
    }

    // Map full room names to short codes (for Hard Achievements column)
    private static String shortenRoom(String room) {
        if (room == null) return "";
        String trimmed = room.trim();
        String normalized = trimmed.toLowerCase().replaceAll("\\s+", "");
        if ("riddlechamber".equals(normalized)) return "RC";
        if ("programminglab".equals(normalized)) return "PL";
        if ("mathquiz".equals(normalized)) return "MQ";
        // Fallback: initials
        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.toString();
    }

    // Define custom ordering for rooms in Escapes table
    private static int roomOrderIndex(String room) {
        if (room == null) return Integer.MAX_VALUE;
        String normalized = room.trim().toLowerCase().replaceAll("\\s+", "");
        if ("riddlechamber".equals(normalized)) return 0;
        if ("programminglab".equals(normalized)) return 1;
        if ("mathquiz".equals(normalized)) return 2;
        return 3; // others go last
    }

    // Define custom ordering for levels in Escapes table
    private static int levelOrderIndex(String level) {
        if (level == null) return Integer.MAX_VALUE;
        String normalized = level.trim().toLowerCase();
        if ("hard".equals(normalized)) return 0;
        if ("medium".equals(normalized)) return 1;
        if ("easy".equals(normalized)) return 2;
        return 3; // others go last
    }

    public static class UserEscape {
        private final String user;
        private final String room;
        private final String level;
        private final java.time.LocalDateTime date;
        private final Integer highScore; // may be null
        private final boolean champion;  // true if user has any Hard escape
        private final boolean multiplayer; // true if this escape was multiplayer

        public UserEscape(String user, String room, String level, java.time.LocalDateTime date, Integer highScore, boolean champion, boolean multiplayer) {
            this.user = user; this.room = room; this.level = level; this.date = date; this.highScore = highScore; this.champion = champion; this.multiplayer = multiplayer;
        }
        public String getUser() { return user; }
        public String getRoom() { return room; }
        public String getLevel() { return level; }
        public java.time.LocalDateTime getDate() { return date; }
        public Integer getHighScore() { return highScore; }
        public String getHighScoreText() {
            if (highScore == null) return "-";
            return String.valueOf(highScore);
        }
        public String getMode() { return multiplayer ? "Multiplayer" : "Solo"; }
        public boolean isMultiplayer() { return multiplayer; }
        public boolean isChampion() { return champion; }
    }
}