package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Settings screen for managing API key and model preferences.
 */
public class SettingsView {

    private VBox view;

    public SettingsView() {
        view = new VBox(0);
        view.setAlignment(Pos.CENTER);
        view.getStyleClass().add("root");

        // === Header ===
        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> AppController.showStartScreen());

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setPadding(new Insets(16, 20, 0, 20));

        Label settingsIcon = new Label("\u2699"); // ⚙
        settingsIcon.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label("Settings");
        titleLabel.getStyleClass().add("title-text");

        Separator sep = new Separator();
        sep.setMaxWidth(260);
        sep.setPadding(new Insets(8, 0, 8, 0));

        // === API Key ===
        Label keyLabel = new Label("OLLAMA API KEY");
        keyLabel.getStyleClass().add("label-text");

        String currentKey = ConfigManager.getApiKey();

        PasswordField keyField = new PasswordField();
        keyField.setText(currentKey);
        keyField.setPromptText("Paste your API key");
        keyField.getStyleClass().add("password-field");
        keyField.setMaxWidth(340);

        Label keyStatus = new Label();
        if (!currentKey.isEmpty()) {
            keyStatus.setText("\u2713 API key is configured");
            keyStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
        } else {
            keyStatus.setText("No API key set — AI questions disabled");
            keyStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        }

        // === Theme Toggle ===
        Label themeLabel = new Label("THEME");
        themeLabel.getStyleClass().add("label-text");

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("LIGHT", "DARK");
        themeBox.setValue(ConfigManager.getTheme());
        themeBox.getStyleClass().add("combo-box");
        
        themeBox.setOnAction(e -> {
            ConfigManager.setTheme(themeBox.getValue());
            AppController.applyTheme();
        });

        // === Model ===
        Label modelLabel = new Label("PREFERRED MODEL");
        modelLabel.getStyleClass().add("label-text");

        TextField modelField = new TextField(ConfigManager.getModel());
        modelField.setPromptText("Cloud model name");
        modelField.getStyleClass().add("text-field");
        modelField.setMaxWidth(220);

        Label modelHint = new Label("Auto-fallback: gemma3:4b-cloud \u2192 qwen3.5:cloud \u2192 gemma4:31b-cloud \u2192 ...");
        modelHint.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 10px; -fx-font-style: italic;");
        modelHint.setWrapText(true);
        modelHint.setMaxWidth(380);

        Separator sep2 = new Separator();
        sep2.setMaxWidth(260);
        sep2.setPadding(new Insets(6, 0, 6, 0));

        // === Status / feedback ===
        Label feedbackLabel = new Label();
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMaxWidth(360);

        // === Save button ===
        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("primary-button");
        AnimationHelper.applyHoverEffect(saveBtn);

        saveBtn.setOnAction(e -> {
            String newKey = keyField.getText().trim();
            String newModel = modelField.getText().trim();

            ConfigManager.setApiKey(newKey);
            ConfigManager.setModel(newModel.isEmpty() ? "qwen3.5:cloud" : newModel);

            if (!newKey.isEmpty()) {
                keyStatus.setText("\u2713 API key saved");
                keyStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
            } else {
                keyStatus.setText("API key cleared — AI questions disabled");
                keyStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
            }

            feedbackLabel.setText("\u2713 Settings saved successfully!");
            feedbackLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px; -fx-font-weight: bold;");
            feedbackLabel.setVisible(true);
            feedbackLabel.setManaged(true);
        });

        // === Clear key button ===
        Button clearBtn = new Button("Clear API Key");
        clearBtn.getStyleClass().add("danger-button");
        clearBtn.setOnAction(e -> {
            keyField.clear();
            ConfigManager.clearApiKey();
            keyStatus.setText("API key cleared — AI questions disabled");
            keyStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        });

        HBox buttonRow = new HBox(12, saveBtn, clearBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // === Lifetime Stats ===
        VBox statsBox = new VBox(5);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: #f8f9fc; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        
        Label statsTitle = new Label("LIFETIME STATS");
        statsTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        
        VBox gBox = new VBox(2, new Label("Games"), new Label("\uD83C\uDFAE " + ConfigManager.getGamesPlayed()));
        gBox.setAlignment(Pos.CENTER);
        ((Label)gBox.getChildren().get(0)).setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        ((Label)gBox.getChildren().get(1)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
        
        VBox pBox = new VBox(2, new Label("Points"), new Label("\u2B50 " + ConfigManager.getLifetimePoints()));
        pBox.setAlignment(Pos.CENTER);
        ((Label)pBox.getChildren().get(0)).setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        ((Label)pBox.getChildren().get(1)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8b5cf6;");
        
        VBox bBox = new VBox(2, new Label("Best Score"), new Label("\uD83C\uDFC6 " + ConfigManager.getBestScore()));
        bBox.setAlignment(Pos.CENTER);
        ((Label)bBox.getChildren().get(0)).setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        ((Label)bBox.getChildren().get(1)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        
        statsRow.getChildren().addAll(gBox, pBox, bBox);
        statsBox.getChildren().addAll(statsTitle, statsRow);

        // === Card ===
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);
        card.getChildren().addAll(
                settingsIcon, titleLabel, sep,
                keyLabel, keyField, keyStatus,
                themeLabel, themeBox,
                modelLabel, modelField, modelHint,
                sep2,
                statsBox,
                feedbackLabel,
                buttonRow
        );

        VBox content = new VBox(0, topBar, new VBox(card));
        ((VBox) content.getChildren().get(1)).setAlignment(Pos.CENTER);
        ((VBox) content.getChildren().get(1)).setPadding(new Insets(10, 0, 0, 0));
        content.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(content.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        content.getStyleClass().add("root");

        view = content;

        AnimationHelper.applyFadeIn(card, 400);
    }

    public VBox getView() {
        return view;
    }
}
