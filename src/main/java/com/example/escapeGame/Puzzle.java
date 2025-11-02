package com.example.escapeGame;

import com.google.gson.annotations.SerializedName;

public class Puzzle {
    private int id;
    private String room;
    private String question;
    @SerializedName(value = "answer", alternate = { "correctAnswer" })
    private String answer;
    private String difficulty;

    public Puzzle() {}

    public Puzzle(int id, String room, String question, String answer, String difficulty) {
        this.id = id;
        this.room = room;
        this.question = question;
        this.answer = answer;
        this.difficulty = difficulty;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}