package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Question;
import com.quiz.model.QuizSession;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class ResultView {

    private VBox view;

    public ResultView(QuizSession session) {
        view = new VBox(0);
        view.setAlignment(Pos.TOP_CENTER);
        view.getStyleClass().add("root");

        int score = session.getScore();
        int total = session.getTotalQuestions();
        int points = session.getTotalPoints();
        int bestStreak = session.getBestStreak();
        String trophy = session.getTrophy();
        String trophyName = session.getTrophyName();
        double pct = (double) score / total;

        // Save lifetime stats
        int lifetimePoints = ConfigManager.getLifetimePoints() + points;
        int gamesPlayed = ConfigManager.getGamesPlayed() + 1;
        int bestScore = Math.max(ConfigManager.getBestScore(), points);
        ConfigManager.setLifetimePoints(lifetimePoints);
        ConfigManager.setGamesPlayed(gamesPlayed);
        ConfigManager.setBestScore(bestScore);

        // === Trophy ===
        Label trophyLabel = new Label(trophy);
        trophyLabel.setStyle("-fx-font-size: 64px;");

        Label trophyNameLabel = new Label(trophyName + " Trophy");
        trophyNameLabel.setStyle(
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;"
        );

        Label titleLabel = new Label("Quiz Complete!");
        titleLabel.getStyleClass().add("title-text");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1e1e3a;");

        Separator sep = new Separator();
        sep.setMaxWidth(200);

        // === Score ===
        Label scoreLabel = new Label(score + " / " + total + " Correct");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(pct) + ";");

        int pctInt = (int) Math.round(pct * 100);
        Label pctLabel = new Label(pctInt + "%");
        pctLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(pct) + ";");

        // === Points card ===
        VBox statsCard = new VBox(6);
        statsCard.setAlignment(Pos.CENTER);
        statsCard.getStyleClass().add("card-inner");
        statsCard.setPadding(new Insets(16, 24, 16, 24));
        statsCard.setMaxWidth(300);

        Label pointsTitleLbl = new Label("\uD83D\uDCB0 POINTS EARNED");
        pointsTitleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0; -fx-font-weight: bold;");

        Label pointsLbl = new Label(String.valueOf(points));
        pointsLbl.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        VBox streakBox = new VBox(2);
        streakBox.setAlignment(Pos.CENTER);
        Label streakTitle = new Label("Best Streak");
        streakTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0;");
        Label streakVal = new Label("\uD83D\uDD25 " + bestStreak);
        streakVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        streakBox.getChildren().addAll(streakTitle, streakVal);

        VBox lifetimeBox = new VBox(2);
        lifetimeBox.setAlignment(Pos.CENTER);
        Label lifetimeTitle = new Label("Lifetime Points");
        lifetimeTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0;");
        Label lifetimeVal = new Label("\u2B50 " + lifetimePoints);
        lifetimeVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7c5ce7;");
        lifetimeBox.getChildren().addAll(lifetimeTitle, lifetimeVal);

        VBox gamesBox = new VBox(2);
        gamesBox.setAlignment(Pos.CENTER);
        Label gamesTitle = new Label("Games Played");
        gamesTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0;");
        Label gamesVal = new Label("\uD83C\uDFAE " + gamesPlayed);
        gamesVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        gamesBox.getChildren().addAll(gamesTitle, gamesVal);

        statsRow.getChildren().addAll(streakBox, lifetimeBox, gamesBox);

        statsCard.getChildren().addAll(pointsTitleLbl, pointsLbl, statsRow);

        // === Performance message ===
        Label msgLabel = new Label(getPerformanceMsg(pct));
        msgLabel.getStyleClass().add("subtitle-text");
        msgLabel.setStyle("-fx-font-size: 14px;");

        // === Buttons ===
        Button playAgainBtn = new Button("Play Again  \u2192");
        playAgainBtn.getStyleClass().add("primary-button");
        AnimationHelper.applyHoverEffect(playAgainBtn);
        playAgainBtn.setOnAction(e -> AppController.showStartScreen());

        Button homeBtn = new Button("Back to Home");
        homeBtn.getStyleClass().add("secondary-button");
        AnimationHelper.applyHoverEffect(homeBtn);
        homeBtn.setOnAction(e -> AppController.showStartScreen());

        HBox buttonRow = new HBox(12, playAgainBtn, homeBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // === Main Results Card ===
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);
        card.setPadding(new Insets(30, 36, 30, 36));
        card.getChildren().addAll(
                trophyLabel, trophyNameLabel,
                titleLabel, sep,
                pctLabel, scoreLabel,
                statsCard, msgLabel,
                buttonRow
        );

        // === Wrong Answers Review Section ===
        List<QuizSession.WrongAnswer> wrongAnswers = session.getWrongAnswers();

        VBox contentBox = new VBox(16);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new Insets(20, 36, 40, 36));
        contentBox.getChildren().add(card);

        if (!wrongAnswers.isEmpty()) {
            VBox reviewSection = buildWrongAnswerReview(wrongAnswers);
            contentBox.getChildren().add(reviewSection);
        }

        // Wrap in ScrollPane so it scrolls if there are many wrong answers
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("root");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        view.getChildren().add(scrollPane);

        AnimationHelper.applySlideUp(card, 500);
        AnimationHelper.applyBounceIn(trophyLabel, 700);
        AnimationHelper.applyBounceIn(pointsLbl, 900);
    }

    /**
     * Build the "Wrong Answers Review" section showing each wrong question
     * with the user's answer vs the correct answer.
     */
    private VBox buildWrongAnswerReview(List<QuizSession.WrongAnswer> wrongAnswers) {
        VBox section = new VBox(12);
        section.setAlignment(Pos.TOP_CENTER);
        section.setMaxWidth(520);

        Separator topSep = new Separator();
        topSep.setMaxWidth(400);

        Label reviewTitle = new Label("\u274C  Wrong Answers Review");
        reviewTitle.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ef4444;"
        );

        Label reviewSubtitle = new Label("Learn from your mistakes:");
        reviewSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #9090b0; -fx-font-style: italic;");

        section.getChildren().addAll(topSep, reviewTitle, reviewSubtitle);

        String[] optLabels = {"A", "B", "C", "D"};

        for (int i = 0; i < wrongAnswers.size(); i++) {
            QuizSession.WrongAnswer wa = wrongAnswers.get(i);
            Question q = wa.question;

            VBox qCard = new VBox(8);
            qCard.getStyleClass().add("card");
            qCard.setPadding(new Insets(16, 20, 16, 20));
            qCard.setMaxWidth(500);
            qCard.setAlignment(Pos.CENTER_LEFT);

            // Question number + text
            Label qNum = new Label("#" + (i + 1));
            qNum.setStyle("-fx-font-size: 11px; -fx-text-fill: #9090b0; -fx-font-weight: bold;");

            Label qText = new Label(q.getText());
            qText.setWrapText(true);
            qText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0e0ff;");

            // Your answer
            String yourAnswerText;
            if (wa.selectedIndex == -1) {
                yourAnswerText = "\u23F1 Time's up (no answer)";
            } else {
                yourAnswerText = optLabels[wa.selectedIndex] + ". " + q.getOptions().get(wa.selectedIndex);
            }

            Label yourLabel = new Label("Your answer:");
            yourLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0;");

            Label yourAnswer = new Label("\u274C  " + yourAnswerText);
            yourAnswer.setWrapText(true);
            yourAnswer.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #ef4444; " +
                "-fx-background-color: rgba(239,68,68,0.1); " +
                "-fx-padding: 6 10; -fx-background-radius: 6;"
            );

            // Correct answer
            int correctIdx = q.getCorrectOptionIndex();
            String correctText = optLabels[correctIdx] + ". " + q.getOptions().get(correctIdx);

            Label correctLabel = new Label("Correct answer:");
            correctLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9090b0;");

            Label correctAnswer = new Label("\u2705  " + correctText);
            correctAnswer.setWrapText(true);
            correctAnswer.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #22c55e; " +
                "-fx-background-color: rgba(34,197,94,0.1); " +
                "-fx-padding: 6 10; -fx-background-radius: 6;"
            );

            qCard.getChildren().addAll(qNum, qText, yourLabel, yourAnswer, correctLabel, correctAnswer);
            section.getChildren().add(qCard);
        }

        return section;
    }

    private String getScoreColor(double pct) {
        if (pct >= 0.8) return "#22c55e";
        if (pct >= 0.6) return "#f59e0b";
        if (pct >= 0.4) return "#f97316";
        return "#ef4444";
    }

    private String getPerformanceMsg(double pct) {
        if (pct == 1.0) return "Perfect! Absolutely flawless!";
        if (pct >= 0.8) return "Outstanding performance!";
        if (pct >= 0.6) return "Good job! Well played!";
        if (pct >= 0.4) return "Not bad, keep improving!";
        return "Keep practicing! You'll get there!";
    }

    public VBox getView() {
        return view;
    }
}
