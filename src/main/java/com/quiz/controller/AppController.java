package com.quiz.controller;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizSession;
import com.quiz.ui.*;
import com.quiz.util.AIQuestionGenerator;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import com.quiz.util.DataLoader;
import javafx.animation.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

public class AppController {

    private static Stage primaryStage;
    private static HostServices hostServices;

    // Remembered so "Play again" can replay the same round
    private static Difficulty lastDifficulty = Difficulty.EASY;
    private static int lastQuestionCount = 10;
    private static boolean lastUsedAI = false;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(680);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true); // Open in full screen (maximized)
    }

    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    /** Open a web link in the default browser. */
    public static void openUrl(String url) {
        if (hostServices != null) hostServices.showDocument(url);
    }

    /** Open the folder that holds config.properties in the file manager. */
    public static void openConfigFolder() {
        File dir = new File(ConfigManager.getConfigDir());
        dir.mkdirs();
        if (hostServices != null) hostServices.showDocument(dir.toURI().toString());
    }

    public static boolean isDarkTheme() {
        return "DARK".equals(ConfigManager.getTheme());
    }

    public static void applyTheme() {
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().getStylesheets().clear();
            primaryStage.getScene().getStylesheets().add(AppController.class.getResource("/styles.css").toExternalForm());

            if (isDarkTheme()) {
                primaryStage.getScene().getStylesheets().add(AppController.class.getResource("/dark-mode.css").toExternalForm());
            }
        }
    }

    private static void switchRoot(javafx.scene.Parent root, String title) {
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root, 1100, 760);
            primaryStage.setScene(scene);
            applyTheme();
        } else {
            primaryStage.getScene().setRoot(root);
        }
        if (title != null) primaryStage.setTitle(title);
    }

    /**
     * Entry point: show splash animation first.
     */
    public static void showInitialScreen() {
        SplashView splashView = new SplashView();
        switchRoot(splashView.getView(), "Quiz Master");
        primaryStage.show();
    }

    /**
     * Called after splash animation finishes.
     */
    public static void afterSplash() {
        if (ConfigManager.isFirstRun() && !ConfigManager.hasApiKey()) {
            showSetupScreen();
        } else {
            showStartScreen();
        }
    }

    public static void showSetupScreen() {
        SetupView setupView = new SetupView();
        switchRoot(setupView.getView(), "Quiz Master — Setup");
    }

    public static void showStartScreen() {
        StartView startView = new StartView();
        switchRoot(startView.getView(), "Quiz Master");
    }

    public static void showSettings() {
        SettingsView settingsView = new SettingsView();
        switchRoot(settingsView.getView(), "Quiz Master — Settings");
    }

    public static Difficulty getLastDifficulty() { return lastDifficulty; }
    public static int getLastQuestionCount() { return lastQuestionCount; }
    public static boolean getLastUsedAI() { return lastUsedAI; }

    /** Replay a round with the same settings as the last one. */
    public static void replayLast() {
        if (lastUsedAI && ConfigManager.hasApiKey()) {
            startQuizWithAI(lastDifficulty, ConfigManager.getApiKey(), ConfigManager.getModel(), lastQuestionCount);
        } else {
            startQuiz(lastDifficulty, lastQuestionCount);
        }
    }

    public static void startQuiz(Difficulty difficulty, int questionCount) {
        lastDifficulty = difficulty;
        lastQuestionCount = questionCount;
        lastUsedAI = false;

        List<Question> questions = DataLoader.getQuestionsByDifficulty(difficulty);
        if (questions == null || questions.isEmpty()) {
            System.err.println("No questions found for difficulty: " + difficulty);
            return;
        }
        // Limit to user-requested count
        if (questions.size() > questionCount) {
            java.util.Collections.shuffle(questions);
            questions = questions.subList(0, questionCount);
        }
        QuizSession session = new QuizSession(questions, difficulty);
        showQuizScene(session);
    }

    /**
     * AI quiz flow — generates ALL questions in parallel using fastest cloud models.
     */
    public static void startQuizWithAI(Difficulty difficulty, String apiKey, String model, int questionCount) {
        lastDifficulty = difficulty;
        lastQuestionCount = questionCount;
        lastUsedAI = true;

        // === Loading panel ===
        VBox card = new VBox(18);
        card.getStyleClass().add("panel");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(500);
        card.setPadding(new Insets(36, 40, 36, 40));

        // Slowly spinning sparkle
        Node sparkle = Ui.icon(Ui.I_SPARKLE, 34, "icon-accent");
        RotateTransition spin = new RotateTransition(Duration.seconds(6), sparkle);
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.play();

        ScaleTransition pulse = new ScaleTransition(Duration.millis(900), sparkle);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        Label eyebrow = Ui.label("GENERATING ROUND · " + difficulty, "eyebrow-accent");
        Label title = Ui.wrapped("Writing your questions…", 420, "h1");

        Label modelLbl = Ui.label("Connecting to " + ConfigManager.getModel(), "mono");

        ProgressBar loadBar = new ProgressBar(0);
        loadBar.setMaxWidth(Double.MAX_VALUE);
        loadBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        Label progressLbl = Ui.label(String.format("%02d / %02d READY", 0, questionCount), "eyebrow");

        HBox progressRow = new HBox(progressLbl, Ui.hgrow(), modelLbl);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        // === Rotating tips ===
        String[] tips = {
            "Stuck? 50:50 removes two wrong answers.",
            "Faster answers earn up to 50 bonus points.",
            "Chain correct answers to build a streak bonus.",
            "Skip keeps your streak alive — use it wisely.",
            "Press A–D or 1–4 to answer from the keyboard.",
            "Every question gets its own topic, and none repeat."
        };
        Label tipLbl = Ui.wrapped(tips[0], 420, "body");

        Timeline tipCycle = new Timeline();
        for (int i = 0; i < tips.length; i++) {
            final String tip = tips[i];
            tipCycle.getKeyFrames().add(
                new KeyFrame(Duration.seconds(i * 3.5), e -> {
                    tipLbl.setText(tip);
                    AnimationHelper.applyFadeIn(tipLbl, 400);
                })
            );
        }
        tipCycle.getKeyFrames().add(new KeyFrame(Duration.seconds(tips.length * 3.5)));
        tipCycle.setCycleCount(Animation.INDEFINITE);
        tipCycle.play();

        VBox tipBox = new VBox(6, Ui.label("WHILE YOU WAIT", "eyebrow"), tipLbl);
        tipBox.getStyleClass().add("panel-flat");

        Button cancelBtn = Ui.button("Cancel", "btn-ghost", Ui.I_ARROW_LEFT);

        card.getChildren().addAll(sparkle, eyebrow, title, loadBar, progressRow, tipBox, cancelBtn);

        StackPane loadingView = new StackPane(card);
        loadingView.getStyleClass().add("root");
        loadingView.setPadding(new Insets(40));

        AnimationHelper.applySlideUp(card, 500);

        switchRoot(loadingView, "Quiz Master — Loading");

        final boolean[] cancelled = {false};
        Runnable stopAnims = () -> { spin.stop(); pulse.stop(); tipCycle.stop(); };
        cancelBtn.setOnAction(e -> {
            cancelled[0] = true;
            stopAnims.run();
            showStartScreen();
        });

        Thread worker = new Thread(() -> {
            AIQuestionGenerator gen = new AIQuestionGenerator("https://ollama.com", model, apiKey);

            // Live status updates — progress bar + model status
            gen.setStatusCallback((usedModel, qNum, total) -> {
                Platform.runLater(() -> {
                    int displayNum = Math.min(qNum, questionCount);
                    modelLbl.setText("Q" + qNum + " → " + usedModel);
                    progressLbl.setText(String.format("%02d / %02d READY", displayNum, questionCount));
                    loadBar.setProgress((double) qNum / total);
                });
            });

            try {
                long start = System.currentTimeMillis();
                List<Question> aiQuestions = gen.generateQuestions(difficulty, questionCount);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("Generated " + aiQuestions.size() + " questions in " + elapsed + "ms");

                Platform.runLater(() -> {
                    if (cancelled[0]) return;
                    stopAnims.run();
                    if (aiQuestions != null && !aiQuestions.isEmpty()) {
                        QuizSession session = new QuizSession(aiQuestions, difficulty);
                        showQuizScene(session);
                    } else {
                        showConnectionError("The AI didn't return any usable questions. Try again, or pick a different model in Settings.");
                    }
                });
            } catch (Exception ex) {
                System.err.println("AI generation failed: " + ex.getMessage());
                Platform.runLater(() -> {
                    if (cancelled[0]) return;
                    stopAnims.run();
                    showConnectionError(ex.getMessage());
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private static void startQuizFetcherDaemon(QuizSession session, Difficulty difficulty, String apiKey, int batchSize) {
        Thread daemon = new Thread(() -> {
            AIQuestionGenerator gen = new AIQuestionGenerator("https://ollama.com", apiKey);
            while (true) {
                try { Thread.sleep(2000); } catch(Exception e) {}

                // Stop daemon if user quits playing
                if (primaryStage.getScene() == null) break;

                // If they are on another screen or stopped, terminate gracefully
                if (!"Quiz Master — Playing".equals(primaryStage.getTitle())) break;

                // Aggressively prefetch if remaining drops below 3
                if (session.getRemainingQuestionsCount() <= 2) {
                    try {
                        List<Question> extra = gen.generateQuestions(difficulty, batchSize);
                        if (!extra.isEmpty()) {
                            session.addQuestions(extra);
                        }
                    } catch (Exception e) {
                        // Silent fail on background fetch, user just runs out normally
                    }
                }
            }
        });
        daemon.setDaemon(true);
        daemon.start();
    }

    private static void showConnectionError(String errorDetail) {
        StackPane badge = new StackPane(Ui.icon(Ui.I_ALERT, 26, "icon-bad"));
        badge.setStyle("-fx-background-color: -c-bad-soft; -fx-background-radius: 999;");
        badge.setMinSize(56, 56);
        badge.setMaxSize(56, 56);

        Label eyebrow = Ui.label("CONNECTION FAILED", "eyebrow");
        eyebrow.getStyleClass().add("status-bad");

        Label title = Ui.wrapped("Couldn't reach the AI.", 420, "h1");
        Label errorMsg = Ui.wrapped(errorDetail, 420, "body");
        Label help = Ui.wrapped("Check your key under Settings, or play this round with the built-in questions.", 420, "muted");

        Button offlineBtn = Ui.button("Play offline", "btn-primary", Ui.I_ARROW_RIGHT);
        offlineBtn.setOnAction(e -> startQuiz(lastDifficulty, lastQuestionCount));

        Button settingsBtn = Ui.button("Settings", "btn-outline");
        settingsBtn.setOnAction(e -> showSettings());

        Button backBtn = Ui.button("Menu", "btn-ghost");
        backBtn.setOnAction(e -> showStartScreen());

        HBox actions = new HBox(10, offlineBtn, settingsBtn, backBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, badge, eyebrow, title, errorMsg, help, actions);
        card.getStyleClass().add("panel");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(520);
        card.setPadding(new Insets(36, 40, 36, 40));
        VBox.setMargin(actions, new Insets(10, 0, 0, 0));

        StackPane errorView = new StackPane(card);
        errorView.getStyleClass().add("root");
        errorView.setPadding(new Insets(40));

        switchRoot(errorView, "Quiz Master — Error");

        AnimationHelper.applySlideUp(card, 400);
    }

    private static void showQuizScene(QuizSession session) {
        QuizView quizView = new QuizView(session);
        switchRoot(quizView.getView(), "Quiz Master — Playing");
        quizView.focus();
    }

    public static void showResultScreen(QuizSession session) {
        ResultView resultView = new ResultView(session);
        switchRoot(resultView.getView(), "Quiz Master — Results");
    }
}
