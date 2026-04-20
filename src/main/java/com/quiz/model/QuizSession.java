package com.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizSession {
    private List<Question> questions;
    private int currentQuestionIndex;
    private int score;
    private int totalPoints;
    private int streak;
    private int bestStreak;
    private Difficulty difficulty;
    private List<WrongAnswer> wrongAnswers;

    /** Stores info about a wrong answer for review */
    public static class WrongAnswer {
        public final Question question;
        public final int selectedIndex;

        public WrongAnswer(Question question, int selectedIndex) {
            this.question = question;
            this.selectedIndex = selectedIndex;
        }
    }

    public QuizSession(List<Question> questions, Difficulty difficulty) {
        this.questions = questions;
        this.difficulty = difficulty;
        this.currentQuestionIndex = 0;
        this.score = 0;
        this.totalPoints = 0;
        this.streak = 0;
        this.bestStreak = 0;
        this.wrongAnswers = new ArrayList<>();
        Collections.shuffle(this.questions);
    }

    public synchronized void addQuestions(List<Question> newQuestions) {
        this.questions.addAll(newQuestions);
    }

    public synchronized int getRemainingQuestionsCount() {
        return questions.size() - currentQuestionIndex;
    }

    public synchronized boolean hasNextQuestion() {
        return currentQuestionIndex < questions.size();
    }

    public Question getNextQuestion() {
        if (hasNextQuestion()) {
            return questions.get(currentQuestionIndex++);
        }
        return null;
    }

    public Question getCurrentQuestion() {
        if (currentQuestionIndex > 0 && currentQuestionIndex <= questions.size()) {
            return questions.get(currentQuestionIndex - 1);
        }
        return null;
    }

    /**
     * Award points for a correct answer.
     * Points = base (by difficulty) + time bonus + streak bonus.
     */
    public int awardCorrectAnswer(int timeRemaining) {
        score++;
        streak++;
        if (streak > bestStreak) bestStreak = streak;

        // Base points by difficulty
        int base;
        Difficulty qDiff = getCurrentQuestion() != null ? getCurrentQuestion().getDifficulty() : difficulty;
        if (qDiff == null) qDiff = difficulty;
        switch (qDiff) {
            case EASY: base = 100; break;
            case MEDIUM: base = 200; break;
            case HARD: base = 300; break;
            default: base = 150;
        }

        // Time bonus: up to 50 extra points
        int timeBonus = (int) (timeRemaining * 3.33);

        // Streak bonus: 10 per streak level (max 50)
        int streakBonus = Math.min(streak * 10, 50);

        int earned = base + timeBonus + streakBonus;
        totalPoints += earned;
        return earned;
    }

    public void markWrong(int selectedIndex) {
        streak = 0;
        Question q = getCurrentQuestion();
        if (q != null) {
            wrongAnswers.add(new WrongAnswer(q, selectedIndex));
        }
    }

    /** Mark wrong without a selection (timeout) */
    public void markWrong() {
        streak = 0;
        Question q = getCurrentQuestion();
        if (q != null) {
            wrongAnswers.add(new WrongAnswer(q, -1)); // -1 = timed out
        }
    }

    public List<WrongAnswer> getWrongAnswers() {
        return wrongAnswers;
    }

    public void incrementScore() {
        score++;
    }

    public int getScore() { return score; }
    public int getTotalPoints() { return totalPoints; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }
    public int getTotalQuestions() { return questions.size(); }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public Difficulty getDifficulty() { return difficulty; }

    /**
     * Get trophy based on performance.
     */
    public String getTrophy() {
        double pct = (double) score / getTotalQuestions();
        if (pct == 1.0) return "\uD83C\uDFC6";  // 🏆
        if (pct >= 0.8) return "\uD83E\uDD47";   // 🥇
        if (pct >= 0.6) return "\uD83E\uDD48";   // 🥈
        if (pct >= 0.4) return "\uD83E\uDD49";   // 🥉
        return "\u2B50";                           // ⭐
    }

    public String getTrophyName() {
        double pct = (double) score / getTotalQuestions();
        if (pct == 1.0) return "Champion";
        if (pct >= 0.8) return "Gold";
        if (pct >= 0.6) return "Silver";
        if (pct >= 0.4) return "Bronze";
        return "Participant";
    }
}
