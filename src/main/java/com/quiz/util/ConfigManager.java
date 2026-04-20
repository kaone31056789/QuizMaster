package com.quiz.util;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Persists user settings (API key, model) to a config file
 * so they don't need to re-enter them every time.
 * Stored at: ~/.quizmaster/config.properties
 */
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".quizmaster";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    private static Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not load config: " + e.getMessage());
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (OutputStream out = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
                props.store(out, "QuizMaster Configuration");
            }
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public static String getApiKey() {
        return props.getProperty("ollama.apikey", "");
    }

    public static void setApiKey(String key) {
        props.setProperty("ollama.apikey", key);
        save();
    }

    public static String getModel() {
        return props.getProperty("ollama.model", "gemma3:4b-cloud");
    }

    public static void setModel(String model) {
        props.setProperty("ollama.model", model);
        save();
    }

    public static String getTheme() {
        return props.getProperty("app.theme", "LIGHT");
    }

    public static void setTheme(String theme) {
        props.setProperty("app.theme", theme);
        save();
    }

    public static String getAIMemory() {
        return props.getProperty("ai.memory", "");
    }

    public static void setAIMemory(String memory) {
        props.setProperty("ai.memory", memory);
        save();
    }

    public static boolean isFirstRun() {
        return !"true".equals(props.getProperty("setup.done"));
    }

    public static void setSetupDone() {
        props.setProperty("setup.done", "true");
        save();
    }

    public static boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }

    public static void clearApiKey() {
        props.remove("ollama.apikey");
        save();
    }

    // === Lifetime Stats ===

    public static int getLifetimePoints() {
        return Integer.parseInt(props.getProperty("stats.lifetime_points", "0"));
    }

    public static void setLifetimePoints(int points) {
        props.setProperty("stats.lifetime_points", String.valueOf(points));
        save();
    }

    public static int getGamesPlayed() {
        return Integer.parseInt(props.getProperty("stats.games_played", "0"));
    }

    public static void setGamesPlayed(int count) {
        props.setProperty("stats.games_played", String.valueOf(count));
        save();
    }

    public static int getBestScore() {
        return Integer.parseInt(props.getProperty("stats.best_score", "0"));
    }

    public static void setBestScore(int score) {
        props.setProperty("stats.best_score", String.valueOf(score));
        save();
    }
}
