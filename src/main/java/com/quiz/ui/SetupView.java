package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * First-run setup screen shown only once.
 */
public class SetupView {

    private VBox view;

    public SetupView() {
        view = new VBox(0);
        view.setAlignment(Pos.CENTER);
        view.getStyleClass().add("root");

        // === Welcome header ===
        Label welcomeIcon = new Label("\uD83D\uDC4B"); // 👋
        welcomeIcon.setStyle("-fx-font-size: 52px;");

        Label welcomeTitle = new Label("Welcome to Quiz Master!");
        welcomeTitle.getStyleClass().add("title-text");
        welcomeTitle.setWrapText(true);
        welcomeTitle.setMaxWidth(400);
        welcomeTitle.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #1e1e3a; -fx-text-alignment: center;");

        Label welcomeSub = new Label("Set up AI-powered questions to get started");
        welcomeSub.getStyleClass().add("subtitle-text");
        welcomeSub.setWrapText(true);
        welcomeSub.setMaxWidth(380);
        welcomeSub.setStyle("-fx-text-alignment: center; -fx-font-size: 14px; -fx-text-fill: #7070a0;");

        Separator sep = new Separator();
        sep.setMaxWidth(280);
        sep.setPadding(new Insets(12, 0, 12, 0));

        // === API Key ===
        Label keyLabel = new Label("OLLAMA API KEY");
        keyLabel.getStyleClass().add("label-text");

        PasswordField keyField = new PasswordField();
        keyField.setPromptText("Paste your API key from ollama.com");
        keyField.getStyleClass().add("password-field");
        keyField.setMaxWidth(360);

        Label keyHint = new Label("Get a free key at ollama.com/settings/keys");
        keyHint.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-style: italic;");

        // === Model ===
        Label modelLabel = new Label("PREFERRED MODEL");
        modelLabel.getStyleClass().add("label-text");

        TextField modelField = new TextField("gemma3:4b-cloud");
        modelField.setPromptText("Cloud model name");
        modelField.getStyleClass().add("text-field");
        modelField.setMaxWidth(220);

        Label modelHint = new Label("Auto-fallback to other models if unavailable");
        modelHint.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-font-style: italic;");

        // === Error label ===
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // === Save button ===
        Button saveBtn = new Button("Save & Continue  \u2192");
        saveBtn.getStyleClass().add("primary-button");
        AnimationHelper.applyHoverEffect(saveBtn);

        saveBtn.setOnAction(e -> {
            String key = keyField.getText().trim();
            if (key.isEmpty()) {
                errorLabel.setText("Please enter an API key, or skip below");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            ConfigManager.setApiKey(key);
            ConfigManager.setModel(modelField.getText().trim());
            ConfigManager.setSetupDone();
            AppController.showStartScreen();
        });

        // === Skip button ===
        Button skipBtn = new Button("Skip for now");
        skipBtn.getStyleClass().add("secondary-button");
        AnimationHelper.applyHoverEffect(skipBtn);

        skipBtn.setOnAction(e -> {
            ConfigManager.setSetupDone();
            AppController.showStartScreen();
        });

        Label skipNote = new Label("You can add your API key later in Settings \u2699");
        skipNote.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px;");

        // === Card ===
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.getChildren().addAll(
                welcomeIcon, welcomeTitle, welcomeSub,
                sep,
                keyLabel, keyField, keyHint,
                modelLabel, modelField, modelHint,
                errorLabel,
                saveBtn, skipBtn, skipNote
        );

        view.getChildren().add(card);

        // === Staggered entrance animation ===
        card.setOpacity(0);
        card.setTranslateY(30);

        PauseTransition delay = new PauseTransition(Duration.millis(200));
        delay.setOnFinished(e -> {
            // Card slides up
            FadeTransition cardFade = new FadeTransition(Duration.millis(500), card);
            cardFade.setFromValue(0);
            cardFade.setToValue(1);

            TranslateTransition cardSlide = new TranslateTransition(Duration.millis(500), card);
            cardSlide.setFromY(30);
            cardSlide.setToY(0);

            new ParallelTransition(cardFade, cardSlide).play();

            // Then stagger the children
            PauseTransition childDelay = new PauseTransition(Duration.millis(300));
            childDelay.setOnFinished(ev -> {
                for (int i = 0; i < card.getChildren().size(); i++) {
                    Node child = card.getChildren().get(i);
                    child.setOpacity(0);
                    child.setTranslateY(12);

                    FadeTransition ft = new FadeTransition(Duration.millis(250), child);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis(i * 50));

                    TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
                    tt.setFromY(12);
                    tt.setToY(0);
                    tt.setDelay(Duration.millis(i * 50));

                    new ParallelTransition(ft, tt).play();
                }
            });
            childDelay.play();
        });
        delay.play();
    }

    public VBox getView() {
        return view;
    }
}
