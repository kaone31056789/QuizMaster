package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.model.Difficulty;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class StartView {

    private VBox view;

    public StartView() {
        view = new VBox(0);
        view.setAlignment(Pos.TOP_CENTER);
        view.getStyleClass().add("root");

        // === Top bar with settings gear & theme toggle ===
        Button settingsBtn = new Button("\u2699");
        settingsBtn.getStyleClass().add("back-button");
        settingsBtn.setStyle("-fx-font-size: 20px; -fx-padding: 4 10;");
        settingsBtn.setOnAction(e -> AppController.showSettings());

        Button themeToggleBtn = new Button("DARK".equals(ConfigManager.getTheme()) ? "\u2600" : "\uD83C\uDF19"); // ☀ / 🌙
        themeToggleBtn.getStyleClass().add("back-button");
        themeToggleBtn.setStyle("-fx-font-size: 20px; -fx-padding: 4 10;");
        themeToggleBtn.setOnAction(e -> {
            boolean isDark = "DARK".equals(ConfigManager.getTheme());
            ConfigManager.setTheme(isDark ? "LIGHT" : "DARK");
            themeToggleBtn.setText(isDark ? "\uD83C\uDF19" : "\u2600");
            AppController.applyTheme();
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(topSpacer, themeToggleBtn, settingsBtn);
        topBar.setPadding(new Insets(12, 16, 0, 16));
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // === Center content ===
        VBox centerBox = new VBox(0);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        // === Icon ===
        Label iconLabel = new Label("\uD83E\uDDE0"); // 🧠
        iconLabel.getStyleClass().add("icon-text");

        // === Title ===
        Label titleLabel = new Label("Quiz Master");
        titleLabel.getStyleClass().add("title-text");
        
        // === Lifetime Stats at Home ===
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER);
        
        Label ptsLabel = new Label("\u2B50 " + ConfigManager.getLifetimePoints() + " Pts");
        ptsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-font-size: 14px;");
        
        Label gLabel = new Label("\uD83C\uDFAE " + ConfigManager.getGamesPlayed() + " Games");
        gLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6; -fx-font-size: 14px;");

        statsBox.getChildren().addAll(ptsLabel, gLabel);

        // === Subtitle ===
        Label subtitleLabel = new Label("Test your knowledge. Prove your brilliance.");
        subtitleLabel.getStyleClass().add("subtitle-text");

        Separator sep = new Separator();
        sep.setMaxWidth(260);
        sep.setPadding(new Insets(10, 0, 10, 0));

        // === Difficulty ===
        Label diffLabel = new Label("DIFFICULTY");
        diffLabel.getStyleClass().add("label-text");

        ComboBox<Difficulty> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll(Difficulty.values());
        difficultyCombo.setValue(Difficulty.EASY);
        difficultyCombo.getStyleClass().add("combo-box");

        // === Question Count ===
        Label countLabel = new Label("QUESTIONS");
        countLabel.getStyleClass().add("label-text");

        ComboBox<Integer> countCombo = new ComboBox<>();
        countCombo.getItems().addAll(5, 10, 15, 20);
        countCombo.setValue(5);
        countCombo.getStyleClass().add("combo-box");

        // === AI toggle (only show if API key is configured) ===
        boolean hasKey = ConfigManager.hasApiKey();

        CheckBox aiToggle = new CheckBox("Use AI-Generated Questions");
        aiToggle.getStyleClass().add("check-box");
        aiToggle.setSelected(hasKey);

        Label aiStatus = new Label();
        if (hasKey) {
            aiStatus.setText("\u2713 AI ready \u2022 Model: " + ConfigManager.getModel());
            aiStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
        } else {
            aiStatus.setText("No API key \u2022 Set up in Settings \u2699");
            aiStatus.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px;");
            aiToggle.setSelected(false);
            aiToggle.setDisable(true);
        }

        // === Start button ===
        Button startButton = new Button("Start Quiz  \u2192");
        startButton.getStyleClass().add("primary-button");
        AnimationHelper.applyHoverEffect(startButton);

        startButton.setOnAction(e -> {
            int questionCount = countCombo.getValue();
            if (aiToggle.isSelected() && hasKey) {
                AppController.startQuizWithAI(
                        difficultyCombo.getValue(),
                        ConfigManager.getApiKey(),
                        ConfigManager.getModel(),
                        questionCount
                );
            } else {
                AppController.startQuiz(difficultyCombo.getValue(), questionCount);
            }
        });

        // === Card ===
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(440);
        card.getChildren().addAll(
                iconLabel, titleLabel, statsBox, subtitleLabel,
                sep,
                diffLabel, difficultyCombo,
                countLabel, countCombo,
                aiToggle, aiStatus,
                startButton
        );

        // === Footer ===
        Label footer = new Label("Powered by JavaFX  \u00B7  AI by Ollama");
        footer.getStyleClass().add("footer-text");
        footer.setPadding(new Insets(15, 0, 0, 0));

        centerBox.getChildren().addAll(card, footer);

        view.getChildren().addAll(topBar, centerBox);
        view.setFillWidth(true);

        AnimationHelper.applySlideUp(card, 550);
    }

    public VBox getView() {
        return view;
    }
}
