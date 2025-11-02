package com.example.escapeGame.server;

import com.example.escapeGame.Puzzle;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomState {
    // Removed unused field 'code' as it was assigned but never used
    private final String host;
    private final Set<String> players = ConcurrentHashMap.newKeySet();
    private final Map<String, ClientHandler> clientSinks = new ConcurrentHashMap<>();
    private final List<Puzzle> questions = new ArrayList<>();
    private volatile int currentIndex = -1;
    private volatile boolean inProgress = false;
    private volatile String selectedRoom = "Riddle Chamber";
    private volatile String selectedDifficulty = "Easy";
    // Host-selected desired capacity (min players required to start automatically). Range: 2-4
    private volatile int desiredCapacity = 2;
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Set<String> answeredThisRound = ConcurrentHashMap.newKeySet();
    private final Set<String> correctThisRound = ConcurrentHashMap.newKeySet();
    private volatile boolean unanimousCorrectThisRound = false;
    private volatile boolean timeBonusThisRound = false;
    private volatile Timer roundTimer;

    public RoomState(String code, String host) {
        // Code parameter is kept for backward compatibility but not stored
        this.host = host;
        players.add(host);
        scores.put(host, 0);
    }

    // Returns a human-readable reason if the user cannot join; otherwise null
    public String checkJoinAllowed(String username) {
        if (isInProgress()) return "Game already in progress.";
        if (players.contains(username)) return "You are already in this room.";
        if (players.size() >= 4) return "Room is full (4/4 players).";
        return null;
    }

    boolean addPlayer(String username, ClientHandler client) {
        System.out.println("addPlayer called - Username: " + username + ", Current players: " + players.size() + ", InProgress: " + inProgress);
        if (isInProgress()) {
            System.out.println("Cannot join - game in progress");
            return false;
        }
        // Enforce max capacity of 4 players (host included)
        if (players.size() >= 4) {
            System.out.println("Cannot join - room full (size: " + players.size() + ")");
            return false;
        }
        if (!players.add(username)) {
            System.out.println("Cannot join - player already exists");
            return false;
        }
        if (client != null) {
            clientSinks.put(username, client);
            System.out.println("Added client handler for " + username + ", total handlers: " + clientSinks.size());
        } else {
            System.out.println("WARNING: No client handler provided for " + username);
        }
        scores.putIfAbsent(username, 0);
        System.out.println("Successfully added player: " + username + ", New count: " + players.size());
        return true;
    }

    void addClient(String username, ClientHandler clientHandler) {
        if (clientHandler != null) {
            clientSinks.put(username, clientHandler);
        }
    }

    void removePlayer(String username, ClientHandler client) {
        players.remove(username);
        clientSinks.remove(username);
        // keep scores if player reconnects; do not remove
    }

    boolean isEmpty() { return players.isEmpty(); }
    boolean isHost(String username) { return host.equals(username); }
    int playerCount() { return players.size(); }
    Set<String> getPlayers() { return new TreeSet<>(players); }
    String getHost() { return host; }
    int getDesiredCapacity() { return desiredCapacity; }
    void setDesiredCapacity(int capacity) {
        if (capacity < 2) capacity = 2;
        if (capacity > 4) capacity = 4;
        this.desiredCapacity = capacity;
    }

    void selectQuestions(List<Puzzle> list) {
        questions.clear();
        questions.addAll(list);
    }

    void markStarted() {
        inProgress = true;
        currentIndex = 0;
        answeredThisRound.clear();
    }

    boolean isInProgress() { return inProgress; }

    int getCurrentIndex() { return currentIndex; }
    int getTotalQuestions() { return questions.size(); }
    Puzzle getCurrentQuestion() { return (currentIndex >= 0 && currentIndex < questions.size()) ? questions.get(currentIndex) : null; }

    void setSelection(String room, String difficulty) {
        if (room != null && !room.isEmpty()) selectedRoom = room;
        if (difficulty != null && !difficulty.isEmpty()) selectedDifficulty = difficulty;
    }
    String getSelectedRoom() { return selectedRoom; }
    String getSelectedDifficulty() { return selectedDifficulty; }

    void submitAnswer(String username, String answer, long elapsedMs) {
        Puzzle q = getCurrentQuestion();
        if (q == null) {
            System.out.println("No current question for user " + username);
            return;
        }
        // Only accept first answer per player for this round
        if (answeredThisRound.contains(username)) {
            System.out.println("User " + username + " already answered this round");
            return;
        }
        answeredThisRound.add(username);

        boolean correct = isCorrectAnswer(q.getAnswer(), answer);
        int delta = correct ? scoreFromOrder(true) : -5; // -5 penalty for wrong answer
        if (correct) {
            correctThisRound.add(username);
        }
        int oldScore = scores.getOrDefault(username, 0);
        scores.compute(username, (k, v) -> (v == null ? 0 : v) + delta);
        int newScore = scores.get(username);
        System.out.println("User " + username + " " + (correct ? "correct" : "incorrect") + " answer. Score: " + oldScore + " -> " + newScore + " (" + delta + ")");

        JsonObject res = new JsonObject();
        res.addProperty("type", "answerResult");
        res.addProperty("username", username);
        res.addProperty("correct", correct);
        res.addProperty("scoreDelta", delta);
        // Send only to the player who submitted the answer
        ClientHandler ch = clientSinks.get(username);
        if (ch != null) {
            try { 
                ch.send(res);
                System.out.println("Sent answerResult to " + username);
            } catch (Exception e) {
                System.err.println("Failed to send answerResult to " + username + ": " + e.getMessage());
            }
        } else {
            System.err.println("No client handler found for " + username);
        }

        // Broadcast scores update
        JsonObject scoreMsg = new JsonObject();
        scoreMsg.addProperty("type", "scoreUpdate");
        scoreMsg.add("scores", getScoresJson());
        System.out.println("Broadcasting score update: " + scoreMsg.toString());
        broadcast(scoreMsg);
        
        // Check if all players have answered
        if (allAnsweredThisRound()) {
            System.out.println("All players have answered, finalizing and waiting 3 seconds before next question");
            // Cancel the current round timer since all players have answered
            cancelRoundTimer();
            // Calculate and apply team bonuses
            finalizeRoundAndScore(elapsedMs);
            // Delay 3 seconds so last player can see their result before advancing
            new java.util.Timer(true).schedule(new java.util.TimerTask() {
                @Override public void run() { try { nextQuestion(); } catch (Exception ignored) {} }
            }, 3000);
        }
    }

    // Called by directory when the round timer ends to compute team bonuses
    void finalizeRoundAndScore(long roundDurationMs) {
        int playersCount = players.size();
        // Unanimous bonus: all answered and all correct
        unanimousCorrectThisRound = answeredThisRound.size() == playersCount && correctThisRound.size() == playersCount && playersCount > 0;
        // Time bonus: at least one correct and time <= 10s
        timeBonusThisRound = !correctThisRound.isEmpty() && roundDurationMs <= 10_000L;

        int teamBonus = 0;
        if (unanimousCorrectThisRound) teamBonus += 2; // everyone correct -> +2 points per player
        if (timeBonusThisRound) teamBonus += 5; // unchanged: optional time bonus

        if (teamBonus > 0 && playersCount > 0) {
            int perPlayer = teamBonus; // award bonus to each player equally (team-based display still possible client-side)
            for (String p : players) {
                scores.compute(p, (k, v) -> (v == null ? 0 : v) + perPlayer);
            }
        }

        // Broadcast a team score update with breakdown for UI
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "teamScoreUpdate");
        msg.addProperty("unanimous", unanimousCorrectThisRound);
        msg.addProperty("timeBonus", timeBonusThisRound);
        msg.add("scores", getScoresJson());
        broadcast(msg);
    }

    private int scoreFromElapsed(boolean correct, long ms) {
        if (!correct) return 0;
        if (ms <= 5000) return 10;
        if (ms <= 10000) return 8;
        if (ms <= 15000) return 6;
        if (ms <= 20000) return 4;
        if (ms <= 25000) return 2;
        return 1;
    }

    // Award points by order of correct submissions: 1st=10, 2nd=8, 3rd=6, 4th+=4
    private int scoreFromOrder(boolean correct) {
        if (!correct) return 0;
        int nth = correctThisRound.size() + 1; // next correct rank
        if (nth == 1) return 10;
        if (nth == 2) return 8;
        if (nth == 3) return 6;
        return 4;
    }

    private boolean isCorrectAnswer(String expected, String given) {
        if (expected == null || given == null) return false;
        String e = normalizeAnswer(expected);
        String g = normalizeAnswer(given);
        return !e.isEmpty() && e.equals(g);
    }

    private String normalizeAnswer(String s) {
        // remove non-alphanumeric, lower case, trim
        return s.replaceAll("[^\\p{Alnum}]+", "").toLowerCase(Locale.ROOT).trim();
    }

    JsonObject getScoresObject() {
        JsonObject obj = new JsonObject();
        for (String p : getPlayers()) {
            obj.addProperty(p, scores.getOrDefault(p, 0));
        }
        return obj;
    }

    com.google.gson.JsonArray getScoresJson() {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String p : getPlayers()) {
            JsonObject o = new JsonObject();
            o.addProperty("username", p);
            o.addProperty("score", scores.getOrDefault(p, 0));
            arr.add(o);
        }
        return arr;
    }

    int getScore(String username) { return scores.getOrDefault(username, 0); }

    void resetRoundState() {
        answeredThisRound.clear();
        correctThisRound.clear();
        unanimousCorrectThisRound = false;
        timeBonusThisRound = false;
    }

    boolean allAnsweredThisRound() {
        return answeredThisRound.size() >= players.size();
    }

    void cancelRoundTimer() {
        try { if (roundTimer != null) roundTimer.cancel(); } catch (Exception ignored) {}
        roundTimer = null;
    }

    void startRoundTimer(int seconds, Runnable onTimeout) {
        cancelRoundTimer();
        roundTimer = new Timer(true);
        roundTimer.schedule(new java.util.TimerTask() {
            @Override public void run() { try { onTimeout.run(); } catch (Exception ignored) {} }
        }, seconds * 1000L);
    }

    private void nextQuestion() {
        // Reset round state for the new question
        resetRoundState();
        
        // Move to next question
        boolean hasMoreQuestions = advanceToNextQuestion();
        
        if (hasMoreQuestions) {
            // Broadcast the new question to all players
            JsonObject nextQuestionMsg = new JsonObject();
            nextQuestionMsg.addProperty("type", "nextQuestion");
            // Provide both legacy and unified fields for robustness
            nextQuestionMsg.addProperty("index", currentIndex + 1);
            nextQuestionMsg.addProperty("total", questions.size());
            nextQuestionMsg.addProperty("questionIndex", currentIndex);
            nextQuestionMsg.addProperty("totalQuestions", questions.size());
            
            // Include the question details
            Puzzle currentQ = getCurrentQuestion();
            nextQuestionMsg.addProperty("question", currentQ.getQuestion());
            nextQuestionMsg.addProperty("difficulty", currentQ.getDifficulty());
            nextQuestionMsg.addProperty("room", currentQ.getRoom());

            // Compute time for this question based on selected difficulty
            int timeSec;
            switch (selectedDifficulty.toLowerCase()) {
                case "easy":
                    timeSec = 90;
                    break;
                case "medium":
                    timeSec = 120;
                    break;
                case "hard":
                    timeSec = 150;
                    break;
                default:
                    timeSec = 90; // default
                    break;
            }
            nextQuestionMsg.addProperty("timeSec", timeSec);
            
            broadcast(nextQuestionMsg);
            
            // Start a new timer for this question matching the broadcast duration
            long roundStart = System.currentTimeMillis();
            startRoundTimer(timeSec, () -> {
                // This will be called if the timer runs out
                System.out.println("Time's up for question " + currentIndex);
                long roundDuration = System.currentTimeMillis() - roundStart;
                finalizeRoundAndScore(roundDuration);
                nextQuestion();
            });
        }
    }
    
    boolean advanceToNextQuestion() {
        currentIndex++;
        boolean hasMoreQuestions = currentIndex < questions.size();
        if (!hasMoreQuestions) {
            // Game over, no more questions
            JsonObject gameOverMsg = new JsonObject();
            gameOverMsg.addProperty("type", "gameOver");
            gameOverMsg.add("scores", getScoresJson());
            broadcast(gameOverMsg);
            inProgress = false;
        }
        return hasMoreQuestions;
    }

    void broadcast(JsonObject msg) {
        System.out.println("Broadcasting message to " + clientSinks.size() + " clients: " + msg.toString());
        for (Map.Entry<String, ClientHandler> entry : clientSinks.entrySet()) {
            String username = entry.getKey();
            ClientHandler ch = entry.getValue();
            try { 
                ch.send(msg);
                System.out.println("Sent message to " + username);
            } catch (Exception e) {
                System.err.println("Failed to send message to " + username + ": " + e.getMessage());
            }
        }
    }

    // Broadcast message to all players except the specified username
void broadcastExcept(JsonObject msg, String excludeUsername) {
    System.out.println("Broadcasting message to all clients except " + excludeUsername + ": " + msg.toString());
    for (Map.Entry<String, ClientHandler> entry : clientSinks.entrySet()) {
        String username = entry.getKey();
        if (username.equals(excludeUsername)) {
            System.out.println("Skipping message to " + username + " (excluded)");
            continue;
        }
        ClientHandler ch = entry.getValue();
        try { 
            ch.send(msg);
            System.out.println("Sent message to " + username);
        } catch (Exception e) {
            System.err.println("Failed to send message to " + username + ": " + e.getMessage());
        }
    }
}

// Send message to a specific player
void sendToPlayer(String username, JsonObject msg) {
    System.out.println("Sending message to specific player " + username + ": " + msg.toString());
    ClientHandler ch = clientSinks.get(username);
    if (ch != null) {
        try { 
            ch.send(msg);
            System.out.println("Sent message to " + username);
        } catch (Exception e) {
            System.err.println("Failed to send message to " + username + ": " + e.getMessage());
        }
    } else {
        System.err.println("Player " + username + " not found in room");
    }
}

}


