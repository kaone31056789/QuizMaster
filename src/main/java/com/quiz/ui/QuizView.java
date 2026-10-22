package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizSession;
import com.quiz.util.AnimationHelper;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizView {

    private static final int QUESTION_SECONDS = 15;
    private static final double RING_R = 28;

    private StackPane root;
    private StackPane overlay;
    private QuizSession session;
    private Question currentQuestion;

    private Label questionLabel;
    private Label questionCountLabel;
    private Label difficultyChip;
    private Label pointsLabel;
    private HBox streakChip;
    private Label streakText;
    private ProgressBar progressBar;
    private VBox optionsBox;
    private List<HBox> optionButtons;
    private VBox card;

    // Countdown ring
    private Arc ringArc;
    private Label timerLabel;
    private Timeline ringAnim;

    private Timeline timeline;
    private int timeSeconds;
    private int totalSeconds;
    private boolean answered = false;
    private boolean closed = false;
    private final IntegerProperty shownPoints = new SimpleIntegerProperty(0);

    // === Lifelines ===
    private boolean fiftyFiftyUsed = false;
    private boolean skipUsed = false;
    private boolean extraTimeUsed = false;
    private Button fiftyFiftyBtn;
    private Button skipBtn;
    private Button extraTimeBtn;

    public QuizView(QuizSession session) {
        this.session = session;

        // === HUD ===
        Button backBtn = Ui.button("Menu", "btn-ghost", Ui.I_ARROW_LEFT);
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> {
            closed = true;
            stopTimers();
            AppController.showStartScreen();
        });

        questionCountLabel = Ui.label("", "eyebrow");

        streakText = Ui.label("", "chip", "chip-hot");
        streakText.setGraphic(Ui.icon(Ui.I_FLAME, 14));
        streakChip = new HBox(streakText);
        streakChip.setAlignment(Pos.CENTER);
        streakChip.setVisible(false);

        pointsLabel = Ui.label("0", "hud-score");
        shownPoints.addListener((o, a, b) -> pointsLabel.setText(Ui.num(b.intValue())));
        VBox scoreBox = new VBox(0, Ui.label("SCORE", "eyebrow"), pointsLabel);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);

        HBox hud = new HBox(16, backBtn, Ui.hgrow(), questionCountLabel, Ui.hgrow(), streakChip, scoreBox);
        hud.setAlignment(Pos.CENTER);

        // === Progress ===
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(progressBar, new Insets(14, 0, 0, 0));

        // === Question column ===
        difficultyChip = Ui.label("", "chip");

        HBox metaRow = new HBox(12, difficultyChip, Ui.hgrow(), buildTimerRing());
        metaRow.setAlignment(Pos.CENTER_LEFT);

        questionLabel = new Label();
        questionLabel.getStyleClass().add("question-text");
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(Double.MAX_VALUE);
        questionLabel.setMinHeight(Label.USE_PREF_SIZE);

        optionsBox = new VBox(10);
        VBox.setMargin(optionsBox, new Insets(10, 0, 0, 0));

        HBox lifelineBar = buildLifelineBar();
        Label keysHint = Ui.label("A–D OR 1–4 TO ANSWER", "mono");
        HBox bottomRow = new HBox(10, lifelineBar, Ui.hgrow(), keysHint);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(bottomRow, new Insets(12, 0, 0, 0));

        card = new VBox(16, metaRow, questionLabel, optionsBox, bottomRow);
        card.setMaxWidth(820);
        card.setAlignment(Pos.TOP_LEFT);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(28, 0, 10, 0));
        StackPane.setAlignment(card, Pos.CENTER);
        VBox.setVgrow(center, Priority.ALWAYS);

        VBox content = new VBox(0, hud, progressBar, center);
        content.setPadding(new Insets(22, 40, 26, 40));

        // Overlay layer for floating feedback (never affects layout)
        overlay = new StackPane();
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);

        root = new StackPane(content, overlay);
        root.getStyleClass().add("root");
        root.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKey);

        loadNextQuestion();
    }

    /** Give keyboard focus to the quiz so A–D / 1–4 work immediately. */
    public void focus() {
        Platform.runLater(() -> root.requestFocus());
    }

    private Node buildTimerRing() {
        double size = RING_R * 2 + 10;
        double c = size / 2;

        Circle track = new Circle(c, c, RING_R);
        track.getStyleClass().add("ring-track");

        ringArc = new Arc(c, c, RING_R, RING_R, 90, -360);
        ringArc.setType(ArcType.OPEN);
        ringArc.getStyleClass().add("ring-arc");

        Pane ring = new Pane(track, ringArc);
        ring.setMinSize(size, size);
        ring.setPrefSize(size, size);
        ring.setMaxSize(size, size);

        timerLabel = Ui.label(String.valueOf(QUESTION_SECONDS), "ring-text");

        StackPane box = new StackPane(ring, timerLabel);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    private void handleKey(KeyEvent e) {
        switch (e.getCode()) {
            case A: case DIGIT1: case NUMPAD1: pickByKey(0); break;
            case B: case DIGIT2: case NUMPAD2: pickByKey(1); break;
            case C: case DIGIT3: case NUMPAD3: pickByKey(2); break;
            case D: case DIGIT4: case NUMPAD4: pickByKey(3); break;
            case F: useFiftyFifty(); break;
            case S: useSkip(); break;
            case T: useExtraTime(); break;
            default: return;
        }
        e.consume();
    }

    private void pickByKey(int index) {
        if (optionButtons == null || index >= optionButtons.size()) return;
        HBox opt = optionButtons.get(index);
        if (!opt.isDisabled()) handleAnswer(index, opt);
    }

    /**
     * Build the lifeline bar with 3 lifeline buttons.
     */
    private HBox buildLifelineBar() {
        // 50:50 — remove 2 wrong options
        fiftyFiftyBtn = lifeline("50:50", Ui.I_HALF, "Remove two wrong answers  (F)");
        fiftyFiftyBtn.setOnAction(e -> useFiftyFifty());

        // Skip — skip question, no penalty
        skipBtn = lifeline("SKIP", Ui.I_SKIP, "Skip without penalty  (S)");
        skipBtn.setOnAction(e -> useSkip());

        // Extra Time — add 10 seconds
        extraTimeBtn = lifeline("+10s", Ui.I_CLOCK, "Add ten seconds  (T)");
        extraTimeBtn.setOnAction(e -> useExtraTime());

        HBox bar = new HBox(10, fiftyFiftyBtn, skipBtn, extraTimeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private Button lifeline(String text, String iconPath, String tip) {
        Button b = new Button(text);
        b.getStyleClass().add("lifeline");
        b.setGraphic(Ui.icon(iconPath, 16));
        b.setTooltip(new Tooltip(tip));
        b.setFocusTraversable(false);
        return b;
    }

    // === LIFELINE: 50:50 ===
    private void useFiftyFifty() {
        if (fiftyFiftyUsed || answered) return;
        fiftyFiftyUsed = true;
        fiftyFiftyBtn.setDisable(true);

        int correctIdx = currentQuestion.getCorrectOptionIndex();

        // Collect wrong option indices
        List<Integer> wrongIndices = new ArrayList<>();
        for (int i = 0; i < optionButtons.size(); i++) {
            if (i != correctIdx) wrongIndices.add(i);
        }

        // Randomly pick 2 wrong options to hide
        Collections.shuffle(wrongIndices);
        for (int idx : wrongIndices.subList(0, Math.min(2, wrongIndices.size()))) {
            HBox opt = optionButtons.get(idx);
            opt.setDisable(true);
            opt.getStyleClass().add("eliminated");
        }

        showLifelineUsed("50:50 — two answers gone");
    }

    // === LIFELINE: Skip ===
    private void useSkip() {
        if (skipUsed || answered) return;
        skipUsed = true;
        skipBtn.setDisable(true);

        answered = true;
        stopTimers();

        showLifelineUsed("Skipped — streak safe");

        // Skip without penalty — don't mark wrong, don't add score
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> loadNextQuestion());
        pause.play();
    }

    // === LIFELINE: Extra Time ===
    private void useExtraTime() {
        if (extraTimeUsed || answered) return;
        extraTimeUsed = true;
        extraTimeBtn.setDisable(true);

        timeSeconds += 10;
        totalSeconds = Math.max(totalSeconds, timeSeconds);
        timerLabel.setText(String.valueOf(timeSeconds));
        setUrgent(false);
        startRing();
        AnimationHelper.applyBounceIn(timerLabel, 300);

        showLifelineUsed("+10 seconds");
    }

    /**
     * Show a floating pill when a lifeline is used.
     */
    private void showLifelineUsed(String text) {
        Label lbl = Ui.label(text, "float-note");
        StackPane.setAlignment(lbl, Pos.TOP_CENTER);
        StackPane.setMargin(lbl, new Insets(112, 0, 0, 0));
        overlay.getChildren().add(lbl);

        FadeTransition ft = new FadeTransition(Duration.millis(700), lbl);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setDelay(Duration.millis(900));

        TranslateTransition tt = new TranslateTransition(Duration.millis(1600), lbl);
        tt.setFromY(10);
        tt.setToY(-14);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> overlay.getChildren().remove(lbl));
        pt.play();
    }

    private void loadNextQuestion() {
        if (closed) return;
        answered = false;
        if (!session.hasNextQuestion()) {
            stopTimers();
            AppController.showResultScreen(session);
            return;
        }

        currentQuestion = session.getNextQuestion();
        int qIndex = session.getCurrentQuestionIndex();
        int total = session.getTotalQuestions();

        questionCountLabel.setText(String.format("QUESTION %02d / %02d", qIndex, total));
        questionLabel.setText(currentQuestion.getText());

        Difficulty d = currentQuestion.getDifficulty() != null ? currentQuestion.getDifficulty() : session.getDifficulty();
        difficultyChip.setText(d == Difficulty.RANDOM ? "MIXED" : d.name());

        updateStreak();

        double progress = (double) (qIndex - 1) / total;
        Timeline bar = new Timeline(new KeyFrame(Duration.millis(400),
                new KeyValue(progressBar.progressProperty(), progress, Interpolator.EASE_BOTH)));
        bar.play();

        optionsBox.getChildren().clear();
        optionButtons = new ArrayList<>();

        String[] letters = {"A", "B", "C", "D"};
        List<String> options = currentQuestion.getOptions();

        List<Node> animNodes = new ArrayList<>();
        for (int i = 0; i < options.size() && i < letters.length; i++) {
            HBox opt = buildOption(letters[i], options.get(i));
            final int index = i;
            opt.setOnMouseClicked(e -> handleAnswer(index, opt));
            optionsBox.getChildren().add(opt);
            optionButtons.add(opt);
            animNodes.add(opt);
        }

        AnimationHelper.applyStaggeredFadeIn(animNodes, 60);

        if (qIndex == 1) {
            AnimationHelper.applySlideUp(card, 400);
        } else {
            AnimationHelper.applyFadeIn(questionLabel, 300);
        }

        startTimer();
    }

    private HBox buildOption(String letter, String text) {
        Label tile = Ui.label(letter, "option-letter");
        Label body = Ui.label(text, "option-text");
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setMinHeight(Label.USE_PREF_SIZE);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox opt = new HBox(16, tile, body);
        opt.getStyleClass().add("option");
        opt.setAlignment(Pos.CENTER_LEFT);
        opt.setMaxWidth(Double.MAX_VALUE);
        opt.setAccessibleText(letter + ". " + text);
        return opt;
    }

    private void updateStreak() {
        int streak = session.getStreak();
        if (streak >= 2) {
            streakText.setText(streak + "× STREAK");
            if (!streakChip.isVisible()) {
                streakChip.setVisible(true);
                AnimationHelper.applyBounceIn(streakChip, 350);
            }
        } else {
            streakChip.setVisible(false);
        }
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();
        timeSeconds = QUESTION_SECONDS;
        totalSeconds = QUESTION_SECONDS;
        timerLabel.setText(String.valueOf(timeSeconds));
        setUrgent(false);
        startRing();

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeSeconds--;
                    timerLabel.setText(String.valueOf(Math.max(timeSeconds, 0)));

                    if (timeSeconds <= 5 && timeSeconds > 0) {
                        setUrgent(true);
                        AnimationHelper.applyBounceIn(timerLabel, 250);
                    }

                    if (timeSeconds <= 0) {
                        timeline.stop();
                        handleTimeout();
                    }
                })
        );
        timeline.playFromStart();
    }

    /** Sweep the ring smoothly from the current time down to zero. */
    private void startRing() {
        if (ringAnim != null) ringAnim.stop();
        ringArc.setLength(-360.0 * timeSeconds / totalSeconds);
        ringAnim = new Timeline(new KeyFrame(Duration.seconds(timeSeconds),
                new KeyValue(ringArc.lengthProperty(), 0, Interpolator.LINEAR)));
        ringAnim.play();
    }

    private void setUrgent(boolean urgent) {
        for (Node n : new Node[]{ringArc, timerLabel}) {
            n.getStyleClass().remove("urgent");
            if (urgent) n.getStyleClass().add("urgent");
        }
    }

    private void stopTimers() {
        if (timeline != null) timeline.stop();
        if (ringAnim != null) ringAnim.stop();
    }

    private void handleAnswer(int selectedIndex, HBox selected) {
        if (answered) return;
        answered = true;
        stopTimers();

        for (HBox opt : optionButtons) opt.setDisable(true);

        int correctIndex = currentQuestion.getCorrectOptionIndex();

        AnimationHelper.applyRevealFlash(selected, 2);

        PauseTransition revealPause = new PauseTransition(Duration.millis(600));
        revealPause.setOnFinished(e -> {
            for (int i = 0; i < optionButtons.size(); i++) {
                if (i != selectedIndex && i != correctIndex) optionButtons.get(i).getStyleClass().add("dimmed");
            }

            if (selectedIndex == correctIndex) {
                selected.getStyleClass().add("correct");
                int earned = session.awardCorrectAnswer(timeSeconds);

                // Show floating +points
                showPointsEarned(earned);

                // Pop heavy combo graphic for high streak dopamine
                if (session.getStreak() >= 2) {
                    AnimationHelper.spawnComboGraphic(overlay, session.getStreak());
                }
            } else {
                selected.getStyleClass().add("wrong");
                optionButtons.get(correctIndex).getStyleClass().add("correct");
                session.markWrong(selectedIndex);

                // Shake the screen heavily on wrong answers
                AnimationHelper.applyScreenShake(card);
            }

            countPointsTo(session.getTotalPoints());
            updateStreak();

            PauseTransition nextPause = new PauseTransition(Duration.millis(1100));
            nextPause.setOnFinished(ev -> loadNextQuestion());
            nextPause.play();
        });
        revealPause.play();
    }

    private void countPointsTo(int target) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(500),
                new KeyValue(shownPoints, target, Interpolator.EASE_OUT)));
        t.play();
    }

    private void showPointsEarned(int points) {
        Label floatingPts = Ui.label("+" + points, "float-points");
        StackPane.setAlignment(floatingPts, Pos.TOP_RIGHT);
        StackPane.setMargin(floatingPts, new Insets(70, 44, 0, 0));
        overlay.getChildren().add(floatingPts);

        FadeTransition ft = new FadeTransition(Duration.millis(900), floatingPts);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setDelay(Duration.millis(300));

        TranslateTransition tt = new TranslateTransition(Duration.millis(1200), floatingPts);
        tt.setFromY(20);
        tt.setToY(-30);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> overlay.getChildren().remove(floatingPts));
        pt.play();
    }

    private void handleTimeout() {
        if (answered) return;
        answered = true;
        stopTimers();
        ringArc.setLength(0);

        for (HBox opt : optionButtons) opt.setDisable(true);

        int correctIndex = currentQuestion.getCorrectOptionIndex();
        for (int i = 0; i < optionButtons.size(); i++) {
            optionButtons.get(i).getStyleClass().add(i == correctIndex ? "correct" : "dimmed");
        }
        session.markWrong();
        updateStreak();
        showLifelineUsed("Time's up");

        PauseTransition pause = new PauseTransition(Duration.millis(1400));
        pause.setOnFinished(e -> loadNextQuestion());
        pause.play();
    }

    public Parent getView() {
        return root;
    }
}
