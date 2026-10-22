package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * First-run setup screen shown only once.
 */
public class SetupView {

    private VBox view;

    public SetupView() {
        view = new VBox(0);
        view.getStyleClass().add("root");
        view.setPadding(new Insets(22, 40, 22, 40));

        HBox topBar = new HBox(Ui.brand(), Ui.hgrow(), Ui.label("FIRST-TIME SETUP", "eyebrow"));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // === Left: welcome + steps ===
        Label kicker = Ui.label("WELCOME", "eyebrow-accent");
        TextFlow headline = Ui.headline("Before we\nbegin", 80);
        Label lede = Ui.wrapped("QuizMaster writes fresh questions with Ollama Cloud. "
                + "Add a free API key to unlock them — or skip and play the built-in set.", 440, "lede");

        VBox steps = new VBox(12,
                step("01", "Sign in at ollama.com"),
                step("02", "Open Settings → Keys and create a key"),
                step("03", "Paste it on the right and press Save"));
        VBox.setMargin(steps, new Insets(10, 0, 0, 0));

        VBox left = new VBox(16, kicker, headline, lede, steps);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMaxWidth(480);
        HBox.setHgrow(left, Priority.ALWAYS);

        // === Right: key form ===
        KeyForm form = new KeyForm(ConfigManager.getSavedApiKey());

        Label errorLabel = Ui.label("", "muted");
        errorLabel.getStyleClass().add("status-bad");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button saveBtn = Ui.button("Save & continue", "btn-primary", Ui.I_ARROW_RIGHT);
        saveBtn.setDefaultButton(true);
        AnimationHelper.applyHoverEffect(saveBtn);
        saveBtn.setOnAction(e -> {
            if (form.getKey().isEmpty()) {
                errorLabel.setText("Paste a key first, or skip for now.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            form.save();
            ConfigManager.setSetupDone();
            AppController.showStartScreen();
        });

        Button skipBtn = Ui.button("Skip for now", "btn-ghost");
        skipBtn.setOnAction(e -> {
            ConfigManager.setSetupDone();
            AppController.showStartScreen();
        });

        HBox actions = new HBox(10, saveBtn, skipBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(14, Ui.label("Connect Ollama", "h2"), form.getView(), errorLabel, actions);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(32));
        panel.setMinWidth(400);
        panel.setPrefWidth(460);
        panel.setMaxWidth(460);
        panel.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        HBox columns = new HBox(64, left, panel);
        columns.setAlignment(Pos.CENTER);
        columns.setMaxWidth(1080);
        columns.setPadding(new Insets(24, 0, 24, 0));

        VBox centered = new VBox(columns);
        centered.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(topBar, scroll);

        AnimationHelper.applyStaggeredFadeIn(List.of(kicker, headline, lede, steps), 90);
        AnimationHelper.applySlideUp(panel, 550);
        form.focusKey();
    }

    private static HBox step(String n, String text) {
        Label num = Ui.label(n, "eyebrow-accent");
        num.setMinWidth(28);
        HBox row = new HBox(12, num, Ui.label(text, "body"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public VBox getView() {
        return view;
    }
}
