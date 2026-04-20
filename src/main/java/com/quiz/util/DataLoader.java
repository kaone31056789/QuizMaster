package com.quiz.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class DataLoader {

    private static List<Question> allQuestions = new ArrayList<>();
    private static Set<String> usedQuestionTexts = new HashSet<>();

    public static void loadQuestions() {
        try (Reader reader = new InputStreamReader(
                DataLoader.class.getResourceAsStream("/questions.json"))) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Question>>() {}.getType();
            allQuestions = gson.fromJson(reader, listType);
            if (allQuestions == null) {
                allQuestions = new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Failed to load questions.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get questions by difficulty, avoiding recently used ones.
     * If RANDOM, picks from all difficulties.
     */
    public static List<Question> getQuestionsByDifficulty(Difficulty difficulty) {
        if (allQuestions.isEmpty()) {
            loadQuestions();
        }

        List<Question> pool;
        if (difficulty == Difficulty.RANDOM) {
            pool = new ArrayList<>(allQuestions);
        } else {
            pool = allQuestions.stream()
                    .filter(q -> q.getDifficulty() == difficulty)
                    .collect(Collectors.toList());
        }

        // Shuffle
        Collections.shuffle(pool);

        // Prioritize unseen questions
        List<Question> unseen = pool.stream()
                .filter(q -> !usedQuestionTexts.contains(q.getText()))
                .collect(Collectors.toList());

        List<Question> result;
        if (unseen.size() >= 10) {
            result = new ArrayList<>(unseen.subList(0, Math.min(10, unseen.size())));
        } else {
            // Not enough unseen — reset tracking and use all
            usedQuestionTexts.clear();
            result = new ArrayList<>(pool.subList(0, Math.min(10, pool.size())));
        }

        // Mark as used
        for (Question q : result) {
            usedQuestionTexts.add(q.getText());
        }

        return result;
    }

    /**
     * Reset the used questions tracker.
     */
    public static void resetUsedQuestions() {
        usedQuestionTexts.clear();
    }
}
