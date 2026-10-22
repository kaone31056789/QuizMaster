package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Difficulty;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextFlow;

import java.util.Arrays;
import java.util.List;

public class StartView {

    private VBox view;

    private Difficulty difficulty = AppController.getLastDifficulty();
    private int questionCount = AppController.getLastQuestionCount();

    public StartView() {
        view = new VBox(0);
        view.getStyleClass().add("root");
        view.setPadding(new Insets(22, 40, 22, 40));

        // === Top bar: brand · theme toggle · settings ===
        boolean dark = AppController.isDarkTheme();
        Button themeBtn = Ui.iconButton(dark ? Ui.I_SUN : Ui.I_MOON, dark ? "Light theme" : "Dark theme");
        themeBtn.setOnAction(e -> {
            ConfigManager.setTheme(AppController.isDarkTheme() ? "LIGHT" : "DARK");
            AppController.applyTheme();
            boolean nowDark = AppController.isDarkTheme();
            themeBtn.setGraphic(Ui.icon(nowDark ? Ui.I_SUN : Ui.I_MOON, 18));
            themeBtn.getTooltip().setText(nowDark ? "Light theme" : "Dark theme");
        });

        Button settingsBtn = Ui.iconButton(Ui.I_SLIDERS, "Settings & API key");
        settingsBtn.setOnAction(e -> AppController.showSettings());

        HBox topBar = new HBox(10, Ui.brand(), Ui.hgrow(), themeBtn, settingsBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // === Left column: headline + scoreboard ===
        int games = ConfigManager.getGamesPlayed();
        Label kicker = Ui.label(String.format("ROUND NO. %03d", games + 1), "eyebrow-accent");

        TextFlow headline = Ui.headline("Quiz\nMaster", 96);

        Label lede = Ui.wrapped(
                "Fifteen seconds a question. Three lifelines. "
                + "Fresh AI-written trivia that never repeats.", 440, "lede");

        HBox scoreboard = Ui.statRow(28,
                Ui.stat("LIFETIME PTS", Ui.num(ConfigManager.getLifetimePoints()), "stat-value"),
                Ui.stat("GAMES", Ui.num(games), "stat-value"),
                Ui.stat("BEST", Ui.num(ConfigManager.getBestScore()), "stat-value"));
        VBox.setMargin(scoreboard, new Insets(18, 0, 0, 0));

        VBox left = new VBox(16, kicker, headline, lede, scoreboard);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMaxWidth(500);
        HBox.setHgrow(left, Priority.ALWAYS);

        // === Right column: round setup panel ===
        VBox panel = buildSetupPanel();

        HBox columns = new HBox(64, left, panel);
        columns.setAlignment(Pos.CENTER);
        columns.setMaxWidth(1080);
        VBox.setVgrow(columns, Priority.ALWAYS);

        StackPane center = new StackPane(columns);
        VBox.setVgrow(center, Priority.ALWAYS);

        // === Footer ===
        Label footer = Ui.label("JAVAFX 17  ·  OLLAMA CLOUD  ·  "
                + "PRESS ENTER TO START", "mono");
        HBox footerBar = new HBox(footer);
        footerBar.setAlignment(Pos.CENTER);

        view.getChildren().addAll(topBar, center, footerBar);

        AnimationHelper.applyStaggeredFadeIn(List.of(kicker, headline, lede, scoreboard), 80);
        AnimationHelper.applySlideUp(panel, 550);
    }

    private VBox buildSetupPanel() {
        Label title = Ui.label("Set up your round", "h2");

        // Difficulty
        HBox diffSeg = Ui.segmented(
                Arrays.asList(Difficulty.values()),
                Arrays.asList("Easy", "Medium", "Hard", "Mixed"),
                difficulty, false, d -> difficulty = d);

        // Question count
        HBox countSeg = Ui.segmented(
                Arrays.asList(5, 10, 15, 20),
                Arrays.asList("05", "10", "15", "20"),
                questionCount, true, c -> questionCount = c);

        // AI source
        boolean hasKey = ConfigManager.hasApiKey();
        BooleanProperty useAI = Ui.bool(hasKey);

        StackPane sw = Ui.toggleSwitch(useAI);
        sw.setDisable(!hasKey);

        Label aiTitle = Ui.label("AI-generated questions", "h3");
        Circle statusDot = Ui.dot(hasKey ? "dot-ok" : "dot-bad");
        Label statusText = Ui.label(hasKey ? "Ready · " + ConfigManager.getModel() : "No API key yet", "muted");
        HBox status = new HBox(7, statusDot, statusText);
        status.setAlignment(Pos.CENTER_LEFT);

        VBox aiText = new VBox(3, aiTitle, status);
        if (!hasKey) {
            Button addKey = Ui.button("Add an Ollama key →", "btn-link");
            addKey.setOnAction(e -> AppController.showSettings());
            aiText.getChildren().add(addKey);
        }

        HBox aiRow = new HBox(14, Ui.icon(Ui.I_SPARKLE, 22, "icon-accent"), aiText, Ui.hgrow(), sw);
        aiRow.setAlignment(Pos.CENTER_LEFT);
        aiRow.getStyleClass().add("panel-flat");

        Label offlineNote = Ui.wrapped("Using the built-in question bank (30+ curated questions).", 360, "muted");
        offlineNote.visibleProperty().bind(useAI.not());
        offlineNote.managedProperty().bind(useAI.not());

        // Start
        Button startBtn = Ui.button("Start round", "btn-primary", Ui.I_ARROW_RIGHT);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setDefaultButton(true);
        AnimationHelper.applyHoverEffect(startBtn);
        startBtn.setOnAction(e -> {
            if (useAI.get() && ConfigManager.hasApiKey()) {
                AppController.startQuizWithAI(
                        difficulty,
                        ConfigManager.getApiKey(),
                        ConfigManager.getModel(),
                        questionCount
                );
            } else {
                AppController.startQuiz(difficulty, questionCount);
            }
        });

        VBox panel = new VBox(10,
                title,
                gap(8), Ui.label("DIFFICULTY", "eyebrow"), diffSeg,
                gap(8), Ui.label("QUESTIONS", "eyebrow"), countSeg,
                gap(8), aiRow, offlineNote,
                gap(10), startBtn);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(32));
        panel.setMinWidth(380);
        panel.setPrefWidth(420);
        panel.setMaxWidth(420);
        panel.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return panel;
    }

    private static Node gap(double h) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    public VBox getView() {
        return view;
    }
}
