package com.example.escapeGame;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

public class PuzzleManagerController {
    @FXML private TableView<Puzzle> puzzleTable;
    @FXML private TableColumn<Puzzle, Integer> colId;
    @FXML private TableColumn<Puzzle, String> colRoom;
    @FXML private TableColumn<Puzzle, String> colQuestion;
    @FXML private TableColumn<Puzzle, String> colAnswer;
    @FXML private TableColumn<Puzzle, String> colDifficulty;

    @FXML private TextField roomField;
    @FXML private TextField questionField;
    @FXML private TextField answerField;
    @FXML private ComboBox<String> difficultyBox;

    private final ObservableList<Puzzle> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("room"));
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colAnswer.setCellValueFactory(new PropertyValueFactory<>("answer"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficulty"));

        List<Puzzle> puzzles = PuzzleDataUtil.loadPuzzles();
        data.setAll(puzzles);
        puzzleTable.setItems(data);
    }

    @FXML
    private void handleAdd() {
        String room = roomField.getText();
        String question = questionField.getText();
        String answer = answerField.getText();
        String difficulty = difficultyBox.getValue();
        if (room == null || room.isBlank() || question == null || question.isBlank() || answer == null || answer.isBlank() || difficulty == null) {
            return;
        }
        Puzzle created = PuzzleDataUtil.createPuzzle(new Puzzle(0, room, question, answer, difficulty));
        data.add(created);
        clearForm();
    }

    @FXML
    private void handleEdit() {
        Puzzle selected = puzzleTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!roomField.getText().isBlank()) selected.setRoom(roomField.getText());
        if (!questionField.getText().isBlank()) selected.setQuestion(questionField.getText());
        if (!answerField.getText().isBlank()) selected.setAnswer(answerField.getText());
        if (difficultyBox.getValue() != null) selected.setDifficulty(difficultyBox.getValue());
        PuzzleDataUtil.updatePuzzle(selected);
        puzzleTable.refresh();
        clearForm();
    }

    @FXML
    private void handleDelete() {
        Puzzle selected = puzzleTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        PuzzleDataUtil.deletePuzzle(selected.getId());
        data.remove(selected);
    }

    @FXML
    private void goBack() throws IOException {
        LogIn.changeScene("com/example/escapeGame/adminPanel.fxml", "Admin Panel");
    }

    private void clearForm() {
        roomField.clear();
        questionField.clear();
        answerField.clear();
        difficultyBox.getSelectionModel().clearSelection();
    }
}
