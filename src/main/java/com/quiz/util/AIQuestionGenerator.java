package com.quiz.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Generates quiz questions using the Ollama Cloud API.
 * Uses ONE primary model for all questions (parallel), with fallbacks if it fails.
 * Includes a persistent memory system to prevent question repeats across sessions.
 */
public class AIQuestionGenerator {

    private static final String DEFAULT_HOST = "https://ollama.com";

    /** Primary model — proven reliable and fast (4B params) */
    private static final String PRIMARY_MODEL = "gemma3:4b-cloud";

    /** Fallbacks — only models with CONFIRMED cloud tags */
    private static final List<String> FALLBACK_MODELS = Arrays.asList(
            "ministral-3:3b-cloud",           // 3B params — smallest/fastest fallback
            "gemini-3-flash-preview:cloud",    // Built for speed
            "qwen3.5:cloud",                   // Exists but can be slow
            "gemma4:31b-cloud"                 // Larger but confirmed cloud tag
    );

    /**
     * Persistent memory system — stores question text hashes to prevent repeats.
     * Saved to disk via ConfigManager so it survives app restarts.
     */
    private static final Set<String> memorySet = new HashSet<>();
    private static final List<String> memoryList = new ArrayList<>();  // ordered for prompt injection

    static {
        loadMemory();
    }

    private static void loadMemory() {
        String saved = ConfigManager.getAIMemory();
        if (!saved.isEmpty()) {
            String[] items = saved.split("\\|\\|\\|");
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    memorySet.add(item.trim().toLowerCase());
                    memoryList.add(item.trim());
                }
            }
        }
    }

    private static synchronized void addToMemory(String questionText) {
        String key = questionText.trim().toLowerCase();
        if (!memorySet.contains(key)) {
            memorySet.add(key);
            memoryList.add(questionText.trim());

            // Keep memory bounded to last 80 questions
            if (memoryList.size() > 80) {
                String removed = memoryList.remove(0);
                memorySet.remove(removed.toLowerCase());
            }

            // Persist to disk
            ConfigManager.setAIMemory(String.join("|||", memoryList));
        }
    }

    private static boolean isInMemory(String questionText) {
        String key = questionText.trim().toLowerCase();
        if (memorySet.contains(key)) return true;
        
        // Fuzzy match against past memory
        Set<String> words = extractKeywords(questionText);
        for (String past : memoryList) {
            Set<String> pastWords = extractKeywords(past);
            int overlap = 0;
            for (String w : words) {
                if (pastWords.contains(w)) overlap++;
            }
            int smaller = Math.min(words.size(), pastWords.size());
            // If 60% of significant words match, treat as duplicate
            if (smaller > 0 && (double) overlap / smaller >= 0.6) {
                return true;
            }
        }
        return false;
    }

    private String host;
    private String apiKey;
    private volatile String usedModel = "";
    private volatile String lastError = "";

    /** Callback for real-time status updates */
    private StatusCallback statusCallback;

    @FunctionalInterface
    public interface StatusCallback {
        void onStatus(String model, int questionNum, int total);
    }

    public AIQuestionGenerator(String apiKey) {
        this(DEFAULT_HOST, apiKey);
    }

    public AIQuestionGenerator(String host, String apiKey) {
        this.host = host;
        this.apiKey = apiKey;
    }

    // Backward compat — model param ignored, we use PRIMARY_MODEL
    public AIQuestionGenerator(String host, String model, String apiKey) {
        this.host = host;
        this.apiKey = apiKey;
    }

    public void setStatusCallback(StatusCallback callback) {
        this.statusCallback = callback;
    }

    public String getUsedModel() { return usedModel; }
    public String getLastError() { return lastError; }

    /**
     * Quick connection test with primary model.
     */
    public String testConnection() {
        String result = quickTest(PRIMARY_MODEL);
        if (result == null) {
            usedModel = PRIMARY_MODEL;
            return null;
        }
        // Try fallbacks
        for (String fb : FALLBACK_MODELS) {
            result = quickTest(fb);
            if (result == null) {
                usedModel = fb;
                return null;
            }
        }
        return "No working cloud model found.";
    }

    private String quickTest(String model) {
        try {
            URL url = new URL(host + "/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("prompt", "hi");
            body.addProperty("stream", false);
            JsonObject opts = new JsonObject();
            opts.addProperty("num_predict", 3);
            body.add("options", opts);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    while (br.readLine() != null) {}
                }
                return null;
            } else if (code == 401 || code == 403) {
                return "Invalid API key";
            }
            return "HTTP " + code;
        } catch (java.net.UnknownHostException e) {
            return "Cannot resolve host";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Generate questions using the primary model, all in parallel.
     * If primary fails, falls back to the next working model.
     */
    public List<Question> generateQuestions(Difficulty difficulty, int count) throws Exception {
        // Over-request by 2 to compensate for occasional parse failures
        int requestCount = count + 2;
        List<Question> results = generateWithModel(PRIMARY_MODEL, difficulty, requestCount);

        if (results.size() >= count) {
            return results.subList(0, count);
        }

        // Primary didn't get enough — try fallbacks for the remainder
        int needed = count - results.size();
        System.out.println(PRIMARY_MODEL + " got " + results.size() + "/" + count + ". Trying fallbacks for " + needed + "...");

        for (String fallback : FALLBACK_MODELS) {
            if (needed <= 0) break;

            if (statusCallback != null) {
                statusCallback.onStatus(fallback, results.size() + 1, count);
            }

            List<Question> extra = generateWithModel(fallback, difficulty, needed);
            results.addAll(extra);
            needed = count - results.size();

            if (!extra.isEmpty()) {
                System.out.println("  " + fallback + " contributed " + extra.size() + " questions");
            }
        }

        if (results.isEmpty()) {
            throw new Exception("All models failed. Check your API key and connection.");
        }

        Collections.shuffle(results);
        return results.subList(0, Math.min(results.size(), count));
    }

    /** Topic categories to force diverse questions in parallel requests */
    private static final String[][] TOPIC_POOLS = {
        {"science", "physics, chemistry, biology, astronomy, or medicine"},
        {"history", "ancient civilizations, wars, historical figures, or inventions"},
        {"geography", "countries, cities, rivers, mountains, or oceans"},
        {"nature", "animals, plants, ecosystems, weather, or geology"},
        {"culture", "art, music, literature, movies, or languages"},
        {"sports", "Olympic records, athletes, rules, or tournaments"},
        {"technology", "computers, internet, space exploration, or engineering"},
        {"food", "cuisine, ingredients, cooking traditions, or agriculture"},
        {"human body", "anatomy, diseases, nutrition, or psychology"},
    };

    /**
     * Fire all questions in parallel, each with a DIFFERENT topic category
     * to prevent duplicate/similar questions.
     */
    private List<Question> generateWithModel(String model, Difficulty difficulty, int count) {
        usedModel = model;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        List<Future<Question>> futures = new ArrayList<>();

        // Shuffle topics so each parallel request gets a unique category
        List<String[]> topics = new ArrayList<>(Arrays.asList(TOPIC_POOLS));
        Collections.shuffle(topics);

        for (int i = 0; i < count; i++) {
            final int qNum = i + 1;
            final String[] topic = topics.get(i % topics.size());

            if (statusCallback != null) {
                statusCallback.onStatus(model, qNum, count);
            }

            futures.add(executor.submit(() -> {
                try {
                    String prompt = buildPrompt(difficulty, topic, qNum);
                    String response = callAPI(prompt, model);
                    Question q = parseQuestion(response, difficulty);

                    if (q != null) {
                        // Check memory — skip if duplicate
                        if (isInMemory(q.getText())) {
                            System.out.println("  Skipped memory dup: " + q.getText().substring(0, Math.min(40, q.getText().length())));
                            prompt = buildPrompt(difficulty, topic, qNum + 100);
                            response = callAPI(prompt, model);
                            q = parseQuestion(response, difficulty);
                            if (q != null && isInMemory(q.getText())) {
                                return null;
                            }
                        }

                        if (q != null) {
                            addToMemory(q.getText());
                            System.out.println("✓ Q" + qNum + " [" + model + "] (" + topic[0] + ") " + q.getText().substring(0, Math.min(50, q.getText().length())) + "...");
                        }
                    }
                    return q;
                } catch (Exception e) {
                    System.err.println("✗ Q" + qNum + " [" + model + "] " + e.getMessage());
                    return null;
                }
            }));
        }

        List<Question> results = new ArrayList<>();
        for (Future<Question> f : futures) {
            try {
                Question q = f.get(30, TimeUnit.SECONDS);
                if (q != null) results.add(q);
            } catch (TimeoutException e) {
                f.cancel(true);
            } catch (Exception e) {
                // skip
            }
        }
        executor.shutdownNow();

        // Deduplicate — remove questions with similar keywords
        return deduplicateQuestions(results);
    }

    /**
     * Remove questions that are too similar to each other (same topic repeated).
     * Uses keyword overlap: if 50%+ of significant words match, it's a duplicate.
     */
    private List<Question> deduplicateQuestions(List<Question> questions) {
        List<Question> unique = new ArrayList<>();
        for (Question q : questions) {
            boolean isDup = false;
            Set<String> words = extractKeywords(q.getText());
            for (Question existing : unique) {
                Set<String> existingWords = extractKeywords(existing.getText());
                int overlap = 0;
                for (String w : words) {
                    if (existingWords.contains(w)) overlap++;
                }
                int smaller = Math.min(words.size(), existingWords.size());
                if (smaller > 0 && (double) overlap / smaller >= 0.5) {
                    isDup = true;
                    System.out.println("  Dedup removed: " + q.getText().substring(0, Math.min(50, q.getText().length())));
                    break;
                }
            }
            if (!isDup) unique.add(q);
        }
        return unique;
    }

    /** Extract meaningful keywords (4+ chars, lowercase) from text */
    private static Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        for (String word : text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+")) {
            if (word.length() >= 4) keywords.add(word);
        }
        return keywords;
    }

    /**
     * Build a prompt with STRONG difficulty instructions AND a specific topic category
     * to ensure parallel requests generate diverse, non-overlapping questions.
     */
    private String buildPrompt(Difficulty difficulty, String[] topicHint, int questionSeed) {
        // For RANDOM, pick a random difficulty based on the seed
        Difficulty actualDiff = difficulty;
        if (difficulty == Difficulty.RANDOM) {
            Difficulty[] levels = {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD};
            actualDiff = levels[questionSeed % 3];
        }

        String diffBlock;
        switch (actualDiff) {
            case EASY:
                diffBlock = "Difficulty: EASY\n" +
                    "- Answer should be something a 10-year-old knows\n" +
                    "- Correct answer should be obvious, wrong options clearly wrong\n";
                break;
            case MEDIUM:
                diffBlock = "Difficulty: MEDIUM\n" +
                    "- Requires educated adult knowledge\n" +
                    "- Answer is NOT immediately obvious, wrong options are plausible\n";
                break;
            case HARD:
                diffBlock = "Difficulty: HARD\n" +
                    "- Only experts/trivia buffs would know\n" +
                    "- All 4 options must seem equally plausible, designed to trick\n";
                break;
            default:
                diffBlock = "Difficulty: MEDIUM\n";
        }

        StringBuilder sb = new StringBuilder(500);
        sb.append("Generate 1 unique multiple-choice trivia question.\n\n");
        sb.append(diffBlock).append("\n");

        // Force a SPECIFIC topic category for this request
        sb.append("MANDATORY TOPIC: Your question MUST be about ").append(topicHint[0].toUpperCase());
        sb.append(" (").append(topicHint[1]).append(").\n");
        sb.append("Do NOT ask about any other topic category. Stay strictly within ").append(topicHint[0]).append(".\n\n");

        sb.append("Strictly follow the difficulty level. Pick a creative, surprising angle within the topic.\n");

        // Inject recent memory to avoid repeats
        if (!memoryList.isEmpty()) {
            sb.append("\nAvoid these previously asked questions: ");
            int limit = Math.min(8, memoryList.size());
            for (int i = 0; i < limit; i++) {
                String past = memoryList.get(memoryList.size() - 1 - i);
                sb.append(past, 0, Math.min(35, past.length()));
                if (i < limit - 1) sb.append("; ");
            }
            sb.append("\n");
        }

        // Unique seed per thread (nanoTime + questionSeed combo)
        sb.append("\n[").append(System.nanoTime() + questionSeed * 999999L).append("]\n");
        sb.append("Return ONLY JSON: [{\"text\":\"?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctOptionIndex\":0}]");

        return sb.toString();
    }

    private String callAPI(String prompt, String model) throws Exception {
        URL url = new URL(host + "/api/generate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);
        requestBody.addProperty("format", "json");

        JsonObject opts = new JsonObject();
        opts.addProperty("num_predict", 200);
        opts.addProperty("temperature", 0.9);
        requestBody.add("options", opts);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            lastError = "HTTP " + responseCode;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) err.append(line);
                System.err.println(model + " error: " + err);
            } catch (Exception ignored) {}
            throw new RuntimeException(lastError);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Parse a single question from the API response.
     */
    private Question parseQuestion(String responseBody, Difficulty difficulty) {
        try {
            // The Ollama response is a JSON object with a "response" field
            JsonElement rootElement = JsonParser.parseString(responseBody);
            String generatedText;

            if (rootElement.isJsonObject()) {
                JsonObject root = rootElement.getAsJsonObject();
                if (root.has("response")) {
                    generatedText = root.get("response").getAsString().trim();
                } else if (root.has("message")) {
                    generatedText = root.getAsJsonObject("message").get("content").getAsString().trim();
                } else {
                    // The root itself might BE the question object
                    generatedText = responseBody;
                }
            } else {
                generatedText = responseBody;
            }

            // Try to extract a question JSON from the response
            return extractQuestion(generatedText, difficulty);
        } catch (Exception e) {
            System.err.println("Parse failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Robustly extract a Question from text that may contain JSON.
     * Handles: JSON arrays, single JSON objects, or nested JSON in text.
     */
    private Question extractQuestion(String text, Difficulty difficulty) {
        if (text == null || text.isEmpty()) return null;

        // Strategy 1: Try parsing the whole text as a JSON array
        try {
            JsonElement el = JsonParser.parseString(text);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) {
                return questionFromJsonObject(el.getAsJsonArray().get(0).getAsJsonObject(), difficulty);
            }
            if (el.isJsonObject()) {
                return questionFromJsonObject(el.getAsJsonObject(), difficulty);
            }
        } catch (Exception ignored) {}

        // Strategy 2: Find JSON array in text
        int startIdx = text.indexOf('[');
        int endIdx = text.lastIndexOf(']');
        if (startIdx != -1 && endIdx > startIdx) {
            try {
                String jsonStr = text.substring(startIdx, endIdx + 1);
                JsonArray arr = JsonParser.parseString(jsonStr).getAsJsonArray();
                if (arr.size() > 0) {
                    return questionFromJsonObject(arr.get(0).getAsJsonObject(), difficulty);
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Find JSON object in text
        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        if (objStart != -1 && objEnd > objStart) {
            try {
                String jsonStr = text.substring(objStart, objEnd + 1);
                JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                return questionFromJsonObject(obj, difficulty);
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Safely extract a Question from a JsonObject.
     */
    private Question questionFromJsonObject(JsonObject obj, Difficulty difficulty) {
        try {
            if (!obj.has("text") || !obj.has("options") || !obj.has("correctOptionIndex")) {
                return null;
            }

            String text = obj.get("text").getAsString();
            int correctIdx = obj.get("correctOptionIndex").getAsInt();
            JsonArray optArr = obj.getAsJsonArray("options");
            List<String> options = new ArrayList<>();
            for (JsonElement opt : optArr) {
                String optText = opt.getAsString().trim();
                // Strip AI-generated prefixes like "A. ", "B) ", "A) ", "1. ", "1) " etc.
                optText = optText.replaceFirst("^[A-Da-d1-4][.):]\\s*", "");
                options.add(optText);
            }

            if (options.size() == 4 && correctIdx >= 0 && correctIdx <= 3) {
                return new Question(text, options, correctIdx, difficulty);
            }
        } catch (Exception e) {
            // Silently skip malformed objects
        }
        return null;
    }
}
