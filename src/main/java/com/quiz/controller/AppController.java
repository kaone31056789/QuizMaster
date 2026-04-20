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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class AppController {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(650);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true); // Open in full screen (maximized)
    }

    public static void applyTheme() {
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().getStylesheets().clear();
            primaryStage.getScene().getStylesheets().add(AppController.class.getResource("/styles.css").toExternalForm());
            
            if ("DARK".equals(ConfigManager.getTheme())) {
                primaryStage.getScene().getStylesheets().add(AppController.class.getResource("/dark-mode.css").toExternalForm());
            }
        }
    }

    private static void switchRoot(javafx.scene.Parent root, String title) {
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root, 850, 650);
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
        if (ConfigManager.isFirstRun()) {
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

    public static void startQuiz(Difficulty difficulty, int questionCount) {
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
        VBox loadingView = new VBox(0);
        loadingView.setAlignment(Pos.CENTER);
        loadingView.getStyleClass().add("root");

        // === Animated loading card ===
        VBox loadingCard = new VBox(14);
        loadingCard.setAlignment(Pos.CENTER);
        loadingCard.setMaxWidth(400);
        loadingCard.setPadding(new Insets(40, 30, 40, 30));
        loadingCard.getStyleClass().add("card");

        // Pulsing robot icon with rotation
        Label loadingIcon = new Label("\uD83E\uDD16");
        loadingIcon.setStyle("-fx-font-size: 52px;");

        // Pulse animation on the icon
        ScaleTransition iconPulse = new ScaleTransition(Duration.millis(800), loadingIcon);
        iconPulse.setFromX(1.0); iconPulse.setFromY(1.0);
        iconPulse.setToX(1.15); iconPulse.setToY(1.15);
        iconPulse.setCycleCount(Animation.INDEFINITE);
        iconPulse.setAutoReverse(true);
        iconPulse.setInterpolator(Interpolator.EASE_BOTH);
        iconPulse.play();

        // Gentle rotation
        RotateTransition iconRotate = new RotateTransition(Duration.millis(3000), loadingIcon);
        iconRotate.setFromAngle(-5);
        iconRotate.setToAngle(5);
        iconRotate.setCycleCount(Animation.INDEFINITE);
        iconRotate.setAutoReverse(true);
        iconRotate.setInterpolator(Interpolator.EASE_BOTH);
        iconRotate.play();

        // === Orbiting spinner dots ===
        javafx.scene.layout.StackPane spinnerPane = new javafx.scene.layout.StackPane();
        spinnerPane.setPrefSize(120, 120);
        spinnerPane.setMaxSize(120, 120);

        HBox dotsRow = new HBox(8);
        dotsRow.setAlignment(Pos.CENTER);
        String[] dotColors = {"#8b5cf6", "#3b82f6", "#22c55e", "#f59e0b", "#ef4444"};
        Label[] dots = new Label[5];
        for (int i = 0; i < 5; i++) {
            dots[i] = new Label("\u25CF");
            dots[i].setStyle("-fx-font-size: 12px; -fx-text-fill: " + dotColors[i] + ";");

            // Staggered bounce animation per dot
            TranslateTransition bounce = new TranslateTransition(Duration.millis(500), dots[i]);
            bounce.setFromY(0);
            bounce.setToY(-12);
            bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setAutoReverse(true);
            bounce.setDelay(Duration.millis(i * 100));
            bounce.setInterpolator(Interpolator.EASE_BOTH);
            bounce.play();

            dotsRow.getChildren().add(dots[i]);
        }

        // === Animated text with cycling dots ===
        Label loadingLabel = new Label("Generating questions");
        loadingLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c0c0e0;");

        // Cycle the dots: "." → ".." → "..." → ""
        Timeline dotsCycle = new Timeline(
            new KeyFrame(Duration.millis(400), e -> loadingLabel.setText("Generating questions.")),
            new KeyFrame(Duration.millis(800), e -> loadingLabel.setText("Generating questions..")),
            new KeyFrame(Duration.millis(1200), e -> loadingLabel.setText("Generating questions...")),
            new KeyFrame(Duration.millis(1600), e -> loadingLabel.setText("Generating questions"))
        );
        dotsCycle.setCycleCount(Animation.INDEFINITE);
        dotsCycle.play();

        // === Model status label ===
        Label modelLbl = new Label("Connecting to AI...");
        modelLbl.setStyle("-fx-text-fill: #9090b0; -fx-font-size: 12px;");

        // === Progress counter ===
        Label progressLbl = new Label("0 / " + questionCount + " questions ready");
        progressLbl.setStyle("-fx-text-fill: #7c5ce7; -fx-font-size: 13px; -fx-font-weight: bold;");

        // === Fun tips that cycle ===
        String[] tips = {
            "\uD83D\uDCA1 Tip: Use 50:50 lifeline when stuck!",
            "\u23F0 Tip: Faster answers earn more points!",
            "\uD83D\uDD25 Tip: Build streaks for combo bonuses!",
            "\u23E9 Tip: Skip saves your streak!",
            "\uD83E\uDDE0 Tip: Each question has a unique topic!"
        };
        Label tipLbl = new Label(tips[0]);
        tipLbl.setStyle("-fx-text-fill: #6b6b9a; -fx-font-size: 11px; -fx-font-style: italic;");
        tipLbl.setWrapText(true);

        Timeline tipCycle = new Timeline();
        for (int i = 0; i < tips.length; i++) {
            final String tip = tips[i];
            tipCycle.getKeyFrames().add(
                new KeyFrame(Duration.seconds(i * 3), e -> {
                    tipLbl.setOpacity(0);
                    tipLbl.setText(tip);
                    FadeTransition ft = new FadeTransition(Duration.millis(400), tipLbl);
                    ft.setFromValue(0); ft.setToValue(1);
                    ft.play();
                })
            );
        }
        tipCycle.getKeyFrames().add(new KeyFrame(Duration.seconds(tips.length * 3)));
        tipCycle.setCycleCount(Animation.INDEFINITE);
        tipCycle.play();

        // === Progress bar ===
        ProgressBar loadBar = new ProgressBar(0);
        loadBar.setPrefWidth(280);
        loadBar.setMaxWidth(280);
        loadBar.getStyleClass().add("progress-bar");

        Separator sep = new Separator();
        sep.setMaxWidth(200);

        loadingCard.getChildren().addAll(
            loadingIcon, dotsRow, loadingLabel,
            modelLbl, loadBar, progressLbl,
            sep, tipLbl
        );

        loadingView.getChildren().add(loadingCard);

        // Slide the card in
        AnimationHelper.applySlideUp(loadingCard, 500);

        switchRoot(loadingView, "Quiz Master \u2014 Loading");

        new Thread(() -> {
            AIQuestionGenerator gen = new AIQuestionGenerator("https://ollama.com", apiKey);

            // Live status updates — progress bar + model status
            gen.setStatusCallback((usedModel, qNum, total) -> {
                Platform.runLater(() -> {
                    int displayNum = Math.min(qNum, questionCount);
                    modelLbl.setText("Q" + qNum + " \u2192 " + usedModel);
                    progressLbl.setText(displayNum + " / " + questionCount + " questions ready");
                    loadBar.setProgress((double) qNum / total);
                });
            });

            try {
                long start = System.currentTimeMillis();
                List<Question> aiQuestions = gen.generateQuestions(difficulty, questionCount);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("Generated " + aiQuestions.size() + " questions in " + elapsed + "ms");

                Platform.runLater(() -> {
                    if (aiQuestions != null && !aiQuestions.isEmpty()) {
                        QuizSession session = new QuizSession(aiQuestions, difficulty);
                        showQuizScene(session);
                    } else {
                        showConnectionError("The AI failed to generate valid questions. Try again or check your model.");
                    }
                });
            } catch (Exception ex) {
                System.err.println("AI generation failed: " + ex.getMessage());
                Platform.runLater(() -> showConnectionError("AI Failed: " + ex.getMessage()));
            }
        }).start();
    }

    private static void startQuizFetcherDaemon(QuizSession session, Difficulty difficulty, String apiKey, int batchSize) {
        Thread daemon = new Thread(() -> {
            AIQuestionGenerator gen = new AIQuestionGenerator("https://ollama.com", apiKey);
            while (true) {
                try { Thread.sleep(2000); } catch(Exception e) {}
                
                // Stop daemon if user quits playing
                if (primaryStage.getScene() == null) break;
                
                // If they are on another screen or stopped, terminate gracefully
                if (!"Quiz Master \u2014 Playing".equals(primaryStage.getTitle())) break;

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
        VBox errorView = new VBox(16);
        errorView.setAlignment(Pos.CENTER);
        errorView.getStyleClass().add("root");

        Label errorIcon = new Label("\u26A0");
        errorIcon.setStyle("-fx-font-size: 48px;");

        Label errorTitle = new Label("Connection Failed");
        errorTitle.setStyle("-fx-font-size: 22px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");

        Label errorMsg = new Label(errorDetail);
        errorMsg.setStyle("-fx-text-fill: #7070a0; -fx-font-size: 13px; -fx-text-alignment: center;");
        errorMsg.setWrapText(true);
        errorMsg.setMaxWidth(380);

        Label helpText = new Label("Check your API key in Settings");
        helpText.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-style: italic;");

        Button backBtn = new Button("Back to Menu");
        backBtn.getStyleClass().add("primary-button");
        AnimationHelper.applyHoverEffect(backBtn);
        backBtn.setOnAction(e -> showStartScreen());

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.getChildren().addAll(errorIcon, errorTitle, errorMsg, helpText, backBtn);

        errorView.getChildren().add(card);

        switchRoot(errorView, "Quiz Master — Error");

        AnimationHelper.applySlideUp(card, 400);
    }

    private static void showQuizScene(QuizSession session) {
        QuizView quizView = new QuizView(session);
        switchRoot(quizView.getView(), "Quiz Master — Playing");
    }

    public static void showResultScreen(QuizSession session) {
        ResultView resultView = new ResultView(session);
        switchRoot(resultView.getView(), "Quiz Master — Results");
    }
}
