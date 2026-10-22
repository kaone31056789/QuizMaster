package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizSession;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.List;

public class ResultView {

    private VBox view;

    public ResultView(QuizSession session) {
        view = new VBox(0);
        view.getStyleClass().add("root");

        int score = session.getScore();
        int total = session.getTotalQuestions();
        int points = session.getTotalPoints();
        int bestStreak = session.getBestStreak();
        String trophyName = session.getTrophyName();
        double pct = total == 0 ? 0 : (double) score / total;
        boolean newBest = points > ConfigManager.getBestScore();

        // Save lifetime stats
        int lifetimePoints = ConfigManager.getLifetimePoints() + points;
        int gamesPlayed = ConfigManager.getGamesPlayed() + 1;
        int bestScore = Math.max(ConfigManager.getBestScore(), points);
        ConfigManager.setLifetimePoints(lifetimePoints);
        ConfigManager.setGamesPlayed(gamesPlayed);
        ConfigManager.setBestScore(bestScore);

        // === Top bar ===
        Button homeTop = Ui.button("Menu", "btn-ghost", Ui.I_ARROW_LEFT);
        homeTop.setOnAction(e -> AppController.showStartScreen());
        HBox topBar = new HBox(10, homeTop, Ui.hgrow(), Ui.brand());
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(22, 40, 0, 40));

        // === Medal ===
        Circle disc = new Circle(46);
        disc.getStyleClass().addAll("medal", medalClass(trophyName));
        StackPane medal = new StackPane(disc, Ui.icon(Ui.I_TROPHY, 40, "icon-paper"));
        medal.setMaxSize(92, 92);

        Difficulty d = session.getDifficulty();
        Label kicker = Ui.label(trophyName.toUpperCase() + " TROPHY  ·  "
                + (d == Difficulty.RANDOM ? "MIXED" : d.name()), "eyebrow-accent");

        TextFlow headline = Ui.headline(score + " of " + total, 88);
        Label msg = Ui.wrapped(getPerformanceMsg(pct), 520, "lede");

        VBox hero = new VBox(14, medal, kicker, headline, msg);
        hero.setAlignment(Pos.CENTER_LEFT);

        // === Scoreboard ===
        Label pointsVal = Ui.label("0", "stat-value-lg");
        IntegerProperty counter = new SimpleIntegerProperty(0);
        counter.addListener((o, a, b) -> pointsVal.setText(Ui.num(b.intValue())));
        VBox pointsStat = new VBox(4, Ui.label(newBest ? "POINTS · NEW BEST" : "POINTS", "eyebrow-accent"), pointsVal);

        HBox board = Ui.statRow(32,
                pointsStat,
                Ui.stat("ACCURACY", Math.round(pct * 100) + "%", "stat-value"),
                Ui.stat("BEST STREAK", String.valueOf(bestStreak), "stat-value"),
                Ui.stat("LIFETIME", Ui.num(lifetimePoints), "stat-value"));
        board.getStyleClass().add("panel");
        board.setPadding(new Insets(24, 28, 24, 28));

        // === Actions ===
        Button playAgainBtn = Ui.button("Play again", "btn-primary", Ui.I_REPLAY);
        playAgainBtn.setDefaultButton(true);
        AnimationHelper.applyHoverEffect(playAgainBtn);
        playAgainBtn.setOnAction(e -> AppController.replayLast());

        Button homeBtn = Ui.button("Change settings", "btn-outline");
        homeBtn.setOnAction(e -> AppController.showStartScreen());

        HBox actions = new HBox(12, playAgainBtn, homeBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox column = new VBox(28, hero, board, actions);
        column.setMaxWidth(820);
        column.setAlignment(Pos.TOP_LEFT);

        // === Wrong Answers Review Section ===
        List<QuizSession.WrongAnswer> wrongAnswers = session.getWrongAnswers();
        if (!wrongAnswers.isEmpty()) {
            column.getChildren().add(buildWrongAnswerReview(wrongAnswers));
        }

        StackPane centered = new StackPane(column);
        centered.setPadding(new Insets(30, 40, 60, 40));
        StackPane.setAlignment(column, Pos.TOP_CENTER);

        // Wrap in ScrollPane so it scrolls if there are many wrong answers
        ScrollPane scrollPane = new ScrollPane(centered);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        view.getChildren().addAll(topBar, scrollPane);

        AnimationHelper.applyBounceIn(medal, 700);
        AnimationHelper.applyStaggeredFadeIn(List.of(kicker, headline, msg, board, actions), 90);

        Timeline count = new Timeline(new KeyFrame(Duration.millis(1200),
                new KeyValue(counter, points, Interpolator.EASE_OUT)));
        count.setDelay(Duration.millis(450));
        count.play();
    }

    /**
     * Build the "Review" section showing each wrong question
     * with the user's answer vs the correct answer.
     */
    private VBox buildWrongAnswerReview(List<QuizSession.WrongAnswer> wrongAnswers) {
        VBox section = new VBox(14);

        Label title = Ui.label("Review", "h2");
        Label count = Ui.label(wrongAnswers.size() + " MISSED", "chip");
        HBox header = new HBox(12, title, count);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(18, 0, 0, 0));

        section.getChildren().addAll(Ui.rule(), header);

        String[] optLabels = {"A", "B", "C", "D"};

        for (int i = 0; i < wrongAnswers.size(); i++) {
            QuizSession.WrongAnswer wa = wrongAnswers.get(i);
            Question q = wa.question;

            Label qNum = Ui.label(String.format("%02d", i + 1), "eyebrow-accent");
            Label qText = Ui.wrapped(q.getText(), Double.MAX_VALUE, "h3");
            HBox.setHgrow(qText, Priority.ALWAYS);
            HBox head = new HBox(14, qNum, qText);
            head.setAlignment(Pos.TOP_LEFT);

            // Your answer
            String yourAnswerText = wa.selectedIndex == -1
                    ? "Time ran out"
                    : optLabels[wa.selectedIndex] + ".  " + q.getOptions().get(wa.selectedIndex);

            int correctIdx = q.getCorrectOptionIndex();
            String correctText = optLabels[correctIdx] + ".  " + q.getOptions().get(correctIdx);

            VBox qCard = new VBox(10, head,
                    answerRow("YOU", yourAnswerText, false),
                    answerRow("ANSWER", correctText, true));
            qCard.getStyleClass().add("panel-flat");
            section.getChildren().add(qCard);
        }

        return section;
    }

    private static Node answerRow(String tag, String text, boolean good) {
        Label tagLbl = Ui.label(tag, "answer-tag", good ? "answer-tag-good" : "answer-tag-bad");
        Label txt = Ui.wrapped(text, Double.MAX_VALUE, "body");
        HBox.setHgrow(txt, Priority.ALWAYS);
        HBox row = new HBox(10, Ui.icon(good ? Ui.I_CHECK : Ui.I_X, 16, good ? "icon-good" : "icon-bad"), tagLbl, txt);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("answer-row", good ? "answer-row-good" : "answer-row-bad");
        return row;
    }

    private static String medalClass(String trophyName) {
        switch (trophyName) {
            case "Champion": return "medal-champion";
            case "Gold":     return "medal-gold";
            case "Silver":   return "medal-silver";
            case "Bronze":   return "medal-bronze";
            default:         return "medal-none";
        }
    }

    private String getPerformanceMsg(double pct) {
        if (pct == 1.0) return "Perfect round. Not a single miss.";
        if (pct >= 0.8) return "Outstanding — that's a podium finish.";
        if (pct >= 0.6) return "Solid round. A couple more and you're on gold.";
        if (pct >= 0.4) return "Not bad. Review the misses below and go again.";
        return "Tough one. The review below will help for next time.";
    }

    public VBox getView() {
        return view;
    }
}
