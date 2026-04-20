package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Question;
import com.quiz.model.QuizSession;
import com.quiz.util.AnimationHelper;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizView {

    private VBox view;
    private QuizSession session;
    private Question currentQuestion;

    private Label questionLabel;
    private Label timerLabel;
    private Label questionCountLabel;
    private Label pointsLabel;
    private Label streakLabel;
    private ProgressBar progressBar;
    private VBox optionsBox;
    private List<Button> optionButtons;
    private VBox card;

    private Timeline timeline;
    private int timeSeconds;
    private boolean answered = false;

    // === Lifelines ===
    private boolean fiftyFiftyUsed = false;
    private boolean skipUsed = false;
    private boolean extraTimeUsed = false;
    private Button fiftyFiftyBtn;
    private Button skipBtn;
    private Button extraTimeBtn;

    public QuizView(QuizSession session) {
        this.session = session;
        view = new VBox(14);
        view.setAlignment(Pos.TOP_CENTER);
        view.setPadding(new Insets(20, 36, 20, 36));
        view.getStyleClass().add("root");

        // === Top bar ===
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);

        Button backBtn = new Button("\u2190 Menu");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            if (timeline != null) timeline.stop();
            AppController.showStartScreen();
        });

        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);

        questionCountLabel = new Label("Question 1 of " + session.getTotalQuestions());
        questionCountLabel.getStyleClass().add("question-number-text");

        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);

        // Points display
        pointsLabel = new Label("\uD83D\uDCB0 0");
        pointsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        topBar.getChildren().addAll(backBtn, s1, questionCountLabel, s2, pointsLabel);

        // === Streak + Timer row ===
        HBox statBar = new HBox();
        statBar.setAlignment(Pos.CENTER);

        streakLabel = new Label("");
        streakLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);

        timerLabel = new Label("\u23F1 15");
        timerLabel.getStyleClass().add("timer-text");

        statBar.getChildren().addAll(streakLabel, s3, timerLabel);

        // === Progress bar ===
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-bar");

        // === Lifeline buttons ===
        HBox lifelineBar = buildLifelineBar();

        // === Question card ===
        card = new VBox(18);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(650);
        card.setPadding(new Insets(28));

        questionLabel = new Label();
        questionLabel.getStyleClass().add("question-text");
        questionLabel.setWrapText(true);

        optionsBox = new VBox(10);
        optionsBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(questionLabel, optionsBox);

        VBox.setVgrow(card, Priority.ALWAYS);
        view.getChildren().addAll(topBar, statBar, progressBar, lifelineBar, card);

        loadNextQuestion();
    }

    /**
     * Build the lifeline bar with 3 lifeline buttons.
     */
    private HBox buildLifelineBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(4, 0, 4, 0));

        // 50:50 — remove 2 wrong options
        fiftyFiftyBtn = new Button("\uD83C\uDFAF 50:50");
        styleLifelineButton(fiftyFiftyBtn, "#8b5cf6");
        fiftyFiftyBtn.setOnAction(e -> useFiftyFifty());

        // Skip — skip question, no penalty
        skipBtn = new Button("\u23E9 Skip");
        styleLifelineButton(skipBtn, "#3b82f6");
        skipBtn.setOnAction(e -> useSkip());

        // Extra Time — add 10 seconds
        extraTimeBtn = new Button("\u23F0 +10s");
        styleLifelineButton(extraTimeBtn, "#22c55e");
        extraTimeBtn.setOnAction(e -> useExtraTime());

        bar.getChildren().addAll(fiftyFiftyBtn, skipBtn, extraTimeBtn);
        return bar;
    }

    private void styleLifelineButton(Button btn, String color) {
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 16;" +
            "-fx-background-radius: 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + color + "80, 8, 0.3, 0, 2);"
        );
        AnimationHelper.applyHoverEffect(btn);
    }

    private void disableLifelineButton(Button btn) {
        btn.setDisable(true);
        btn.setStyle(
            "-fx-background-color: #3a3a5c;" +
            "-fx-text-fill: #666688;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 16;" +
            "-fx-background-radius: 20;" +
            "-fx-opacity: 0.5;"
        );
    }

    // === LIFELINE: 50:50 ===
    private void useFiftyFifty() {
        if (fiftyFiftyUsed || answered) return;
        fiftyFiftyUsed = true;
        disableLifelineButton(fiftyFiftyBtn);

        int correctIdx = currentQuestion.getCorrectOptionIndex();

        // Collect wrong option indices
        List<Integer> wrongIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != correctIdx) wrongIndices.add(i);
        }

        // Randomly pick 2 wrong options to hide
        Collections.shuffle(wrongIndices);
        int hide1 = wrongIndices.get(0);
        int hide2 = wrongIndices.get(1);

        // Fade out and disable the 2 wrong options
        for (int idx : new int[]{hide1, hide2}) {
            Button btn = optionButtons.get(idx);
            btn.setDisable(true);
            FadeTransition ft = new FadeTransition(Duration.millis(300), btn);
            ft.setToValue(0.25);
            ft.play();
            btn.setStyle(btn.getStyle() + "-fx-opacity: 0.25;");
        }

        // Flash animation on lifeline usage
        showLifelineUsed("\uD83C\uDFAF 50:50 Used!");
    }

    // === LIFELINE: Skip ===
    private void useSkip() {
        if (skipUsed || answered) return;
        skipUsed = true;
        disableLifelineButton(skipBtn);

        answered = true;
        if (timeline != null) timeline.stop();

        showLifelineUsed("\u23E9 Question Skipped!");

        // Skip without penalty — don't mark wrong, don't add score
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> loadNextQuestion());
        pause.play();
    }

    // === LIFELINE: Extra Time ===
    private void useExtraTime() {
        if (extraTimeUsed || answered) return;
        extraTimeUsed = true;
        disableLifelineButton(extraTimeBtn);

        timeSeconds += 10;
        timerLabel.setText("\u23F1 " + timeSeconds);

        // Remove urgency styling if it was active
        timerLabel.getStyleClass().removeAll("timer-text-urgent");
        if (!timerLabel.getStyleClass().contains("timer-text")) {
            timerLabel.getStyleClass().add("timer-text");
        }

        showLifelineUsed("\u23F0 +10 Seconds!");
    }

    /**
     * Show a floating label when a lifeline is used.
     */
    private void showLifelineUsed(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;" +
            "-fx-effect: dropshadow(gaussian, #8b5cf680, 10, 0.5, 0, 0);"
        );
        view.getChildren().add(lbl);

        FadeTransition ft = new FadeTransition(Duration.millis(1000), lbl);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setDelay(Duration.millis(400));

        TranslateTransition tt = new TranslateTransition(Duration.millis(1200), lbl);
        tt.setByY(-40);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> view.getChildren().remove(lbl));
        pt.play();
    }

    private void loadNextQuestion() {
        answered = false;
        if (!session.hasNextQuestion()) {
            AppController.showResultScreen(session);
            return;
        }

        currentQuestion = session.getNextQuestion();
        int qIndex = session.getCurrentQuestionIndex();
        int total = session.getTotalQuestions();

        questionCountLabel.setText("Question " + qIndex + " of " + total);
        questionLabel.setText(currentQuestion.getText());
        pointsLabel.setText("\uD83D\uDCB0 " + session.getTotalPoints());

        if (session.getStreak() >= 2) {
            streakLabel.setText("\uD83D\uDD25 " + session.getStreak() + " streak!");
        } else {
            streakLabel.setText("");
        }

        double progress = (double) (qIndex - 1) / total;
        progressBar.setProgress(progress);

        optionsBox.getChildren().clear();
        optionButtons = new ArrayList<>();

        String[] labels = {"A", "B", "C", "D"};
        List<String> options = currentQuestion.getOptions();

        List<Node> animNodes = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Button btn = new Button(labels[i] + ".  " + options.get(i));
            btn.getStyleClass().add("option-button");
            btn.setMaxWidth(500);
            btn.setPrefWidth(500);
            AnimationHelper.applyHoverEffect(btn);

            final int index = i;
            btn.setOnAction(e -> handleAnswer(index, btn));
            optionsBox.getChildren().add(btn);
            optionButtons.add(btn);
            animNodes.add(btn);
        }

        AnimationHelper.applyStaggeredFadeIn(animNodes, 70);

        if (qIndex == 1) {
            AnimationHelper.applySlideUp(card, 400);
        } else {
            AnimationHelper.applyFadeIn(card, 250);
        }

        startTimer();
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();
        timeSeconds = 15;
        timerLabel.setText("\u23F1 " + timeSeconds);
        timerLabel.getStyleClass().removeAll("timer-text-urgent");
        if (!timerLabel.getStyleClass().contains("timer-text")) {
            timerLabel.getStyleClass().add("timer-text");
        }

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeSeconds--;
                    timerLabel.setText("\u23F1 " + timeSeconds);

                    if (timeSeconds <= 5 && timeSeconds > 0) {
                        timerLabel.getStyleClass().removeAll("timer-text");
                        if (!timerLabel.getStyleClass().contains("timer-text-urgent")) {
                            timerLabel.getStyleClass().add("timer-text-urgent");
                        }
                    }

                    if (timeSeconds <= 0) {
                        timeline.stop();
                        handleTimeout();
                    }
                })
        );
        timeline.playFromStart();
    }

    private void handleAnswer(int selectedIndex, Button selectedButton) {
        if (answered) return;
        answered = true;
        timeline.stop();

        for (Button btn : optionButtons) btn.setDisable(true);

        int correctIndex = currentQuestion.getCorrectOptionIndex();

        AnimationHelper.applyRevealFlash(selectedButton, 3);

        PauseTransition revealPause = new PauseTransition(Duration.millis(700));
        revealPause.setOnFinished(e -> {
            if (selectedIndex == correctIndex) {
                selectedButton.getStyleClass().add("option-button-correct");
                int earned = session.awardCorrectAnswer(timeSeconds);

                // Show floating +points
                showPointsEarned(earned);
                
                // Pop heavy combo graphic for high streak dopamine
                if (session.getStreak() >= 2) {
                    AnimationHelper.spawnComboGraphic(view, session.getStreak());
                }
            } else {
                selectedButton.getStyleClass().add("option-button-wrong");
                optionButtons.get(correctIndex).getStyleClass().add("option-button-correct");
                session.markWrong(selectedIndex);
                streakLabel.setText("");
                
                // Shake the screen heavily on wrong answers
                AnimationHelper.applyScreenShake(card);
            }

            pointsLabel.setText("\uD83D\uDCB0 " + session.getTotalPoints());
            AnimationHelper.applyBounceIn(pointsLabel, 300);

            if (session.getStreak() >= 2) {
                streakLabel.setText("\uD83D\uDD25 " + session.getStreak() + " streak!");
            }

            PauseTransition nextPause = new PauseTransition(Duration.millis(900));
            nextPause.setOnFinished(ev -> loadNextQuestion());
            nextPause.play();
        });
        revealPause.play();
    }

    private void showPointsEarned(int points) {
        Label floatingPts = new Label("+" + points);
        floatingPts.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        view.getChildren().add(floatingPts);
        floatingPts.setTranslateY(-20);

        FadeTransition ft = new FadeTransition(Duration.millis(800), floatingPts);
        ft.setFromValue(1);
        ft.setToValue(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(800), floatingPts);
        tt.setByY(-50);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> view.getChildren().remove(floatingPts));
        pt.play();
    }

    private void handleTimeout() {
        if (answered) return;
        answered = true;

        for (Button btn : optionButtons) btn.setDisable(true);

        int correctIndex = currentQuestion.getCorrectOptionIndex();
        optionButtons.get(correctIndex).getStyleClass().add("option-button-correct");
        session.markWrong();
        streakLabel.setText("");

        PauseTransition pause = new PauseTransition(Duration.millis(1200));
        pause.setOnFinished(e -> loadNextQuestion());
        pause.play();
    }

    public VBox getView() {
        return view;
    }
}
