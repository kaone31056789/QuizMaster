package com.quiz.util;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Persists user settings (API key, model) to a config file
 * so they don't need to re-enter them every time.
 * Stored at: ~/.quizmaster/config.properties
 * (Windows: C:\Users\<name>\.quizmaster\config.properties)
 *
 * The API key can also be supplied via the OLLAMA_API_KEY environment
 * variable, which takes precedence over the saved value.
 */
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".quizmaster";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";
    private static final String API_KEY_ENV = "OLLAMA_API_KEY";

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

    /**
     * Writes to a temp file first, then swaps it in, so a crash or two
     * overlapping saves can never leave a half-written file (which would
     * wipe the saved API key).
     */
    public static synchronized void save() {
        try {
            Path dir = Paths.get(CONFIG_DIR);
            Files.createDirectories(dir);
            Path tmp = dir.resolve("config.properties.tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "QuizMaster Configuration");
            }
            try {
                Files.move(tmp, Paths.get(CONFIG_FILE),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, Paths.get(CONFIG_FILE), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public static String getConfigDir() {
        return CONFIG_DIR;
    }

    public static String getConfigFile() {
        return CONFIG_FILE;
    }

    /** True when the key comes from the OLLAMA_API_KEY environment variable */
    public static boolean isApiKeyFromEnv() {
        String env = System.getenv(API_KEY_ENV);
        return env != null && !env.isBlank();
    }

    public static String getApiKey() {
        if (isApiKeyFromEnv()) return System.getenv(API_KEY_ENV).trim();
        return props.getProperty("ollama.apikey", "");
    }

    /** The key saved in the config file, ignoring any environment override */
    public static String getSavedApiKey() {
        return props.getProperty("ollama.apikey", "");
    }

    public static void setApiKey(String key) {
        props.setProperty("ollama.apikey", key);
        save();
    }

    public static String getModel() {
        return props.getProperty("ollama.model", AIQuestionGenerator.DEFAULT_MODEL);
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
