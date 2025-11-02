package com.example.escapeGame;

import com.example.escapeGame.Player;
import com.example.escapeGame.QuestionResult;
import com.example.escapeGame.Puzzle;
import java.time.LocalDateTime;
import java.util.*;

public class ClientRoom {
    private String roomCode;
    private String hostUsername;
    private List<Player> players;
    private GameState gameState;
    private LocalDateTime createdAt;
    private int currentQuestionIndex;
    private List<Puzzle> selectedQuestions;
    private List<QuestionResult> questionResults;
    
    public enum GameState {
        WAITING_FOR_PLAYERS,
        READY_TO_START,
        IN_PROGRESS,
        COMPLETED
    }

    public ClientRoom(String roomCode, String hostUsername) {
        this.roomCode = roomCode;
        this.hostUsername = hostUsername;
        this.players = new ArrayList<>();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
        this.createdAt = LocalDateTime.now();
        this.currentQuestionIndex = 0;
        this.selectedQuestions = new ArrayList<>();
        this.questionResults = new ArrayList<>();
    }

    // Getters and setters
    public String getRoomCode() {
        return roomCode;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public String getHostUsername() {
        return hostUsername;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public boolean addPlayer(String username) {
        if (players.size() >= 4) {
            return false; // Room is full
        }
        if (players.stream().anyMatch(p -> p.getUsername().equals(username))) {
            return false; // Player already in room
        }
        boolean isHost = players.isEmpty(); // First player is the host
        players.add(new Player(username, isHost));
        return true;
    }

    public boolean removePlayer(String username) {
        return players.removeIf(p -> p.getUsername().equals(username));
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isHost(String username) {
        return hostUsername.equals(username);
    }

    public void startGame() {
        this.gameState = GameState.IN_PROGRESS;
    }

    public boolean nextQuestion() {
        if (currentQuestionIndex < selectedQuestions.size() - 1) {
            currentQuestionIndex++;
            return true;
        }
        return false;
    }

    public List<Player> getLeaderboard() {
        // Create a new list to avoid modifying the original players list
        List<Player> sortedPlayers = new ArrayList<>(players);
        
        // Sort players by score in descending order
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        
        return sortedPlayers;
    }
    
    public void submitAnswer(String username, String answer, long responseTimeMs) {
        // Check if the answer is correct
        Puzzle currentQuestion = getCurrentQuestion();
        boolean isCorrect = currentQuestion != null && 
                          currentQuestion.getAnswer().equalsIgnoreCase(answer.trim());
        
        // Fixed scoring: +10 per correct answer, -5 for wrong (solo/local mode)
        int score = isCorrect ? 10 : -5;
        
        // Update player's score once
        players.stream()
               .filter(p -> p.getUsername().equals(username))
               .findFirst()
               .ifPresent(player -> player.addScore(score));
        
        // Record the result
        questionResults.add(new QuestionResult(username, isCorrect, responseTimeMs, score));
    }
    
    public List<Puzzle> getSelectedQuestions() {
        return new ArrayList<>(selectedQuestions);
    }
    
    public void setSelectedQuestions(List<Puzzle> questions) {
        this.selectedQuestions = new ArrayList<>(questions);
    }
    
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    public Puzzle getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < selectedQuestions.size()) {
            return selectedQuestions.get(currentQuestionIndex);
        }
        return null;
    }
}
