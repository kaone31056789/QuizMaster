package com.quiz.model;

import java.util.List;

public class Question {
    private String text;
    private List<String> options;
    private int correctOptionIndex;
    private Difficulty difficulty;

    public Question() {
        // default constructor for Jackson/Gson
    }

    public Question(String text, List<String> options, int correctOptionIndex, Difficulty difficulty) {
        this.text = text;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.difficulty = difficulty;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
}
