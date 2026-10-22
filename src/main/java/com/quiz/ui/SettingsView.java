package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.util.AnimationHelper;
import com.quiz.util.ConfigManager;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

/**
 * Settings screen for managing API key and model preferences.
 */
public class SettingsView {

    private VBox view;

    public SettingsView() {
        view = new VBox(0);
        view.getStyleClass().add("root");
        view.setPadding(new Insets(22, 40, 22, 40));

        // === Top bar ===
        Button backBtn = Ui.button("Menu", "btn-ghost", Ui.I_ARROW_LEFT);
        backBtn.setCancelButton(true);

        HBox topBar = new HBox(10, backBtn, Ui.hgrow(), Ui.brand());
        topBar.setAlignment(Pos.CENTER_LEFT);

        // === Left: title, appearance, stats ===
        Label kicker = Ui.label("PREFERENCES", "eyebrow-accent");
        TextFlow headline = Ui.headline("Settings", 80);

        HBox themeSeg = Ui.segmented(
                Arrays.asList("LIGHT", "DARK"),
                Arrays.asList("Paper", "Studio"),
                ConfigManager.getTheme(), false, t -> {
                    ConfigManager.setTheme(t);
                    AppController.applyTheme();
                });
        themeSeg.setMaxWidth(260);

        VBox appearance = new VBox(10, Ui.label("THEME", "eyebrow"), themeSeg);
        VBox.setMargin(appearance, new Insets(12, 0, 0, 0));

        HBox stats = Ui.statRow(28,
                Ui.stat("GAMES", Ui.num(ConfigManager.getGamesPlayed()), "stat-value"),
                Ui.stat("POINTS", Ui.num(ConfigManager.getLifetimePoints()), "stat-value"),
                Ui.stat("BEST", Ui.num(ConfigManager.getBestScore()), "stat-value"));
        VBox statsBox = new VBox(12, Ui.label("LIFETIME", "eyebrow"), stats);
        VBox.setMargin(statsBox, new Insets(12, 0, 0, 0));

        VBox left = new VBox(16, kicker, headline, appearance, statsBox);
        left.setAlignment(Pos.TOP_LEFT);
        left.setMaxWidth(460);
        HBox.setHgrow(left, Priority.ALWAYS);

        // === Right: AI connection panel ===
        KeyForm form = new KeyForm(ConfigManager.getSavedApiKey());

        // Leaving with a newly pasted key keeps it instead of silently dropping it
        backBtn.setOnAction(e -> {
            if (form.hasUnsavedKey()) form.save();
            AppController.showStartScreen();
        });

        Label feedback = Ui.label("", "muted");
        feedback.getStyleClass().add("status-ok");
        feedback.setVisible(false);

        Button saveBtn = Ui.button("Save changes", "btn-primary", Ui.I_CHECK);
        saveBtn.setDefaultButton(true);
        AnimationHelper.applyHoverEffect(saveBtn);
        saveBtn.setOnAction(e -> {
            form.save();
            flash(feedback, "✓ Saved");
        });

        Button clearBtn = Ui.button("Remove key", "btn-danger");
        clearBtn.setOnAction(e -> {
            form.clear();
            flash(feedback, "Key removed");
        });

        HBox actions = new HBox(10, saveBtn, clearBtn, Ui.hgrow(), feedback);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(14, Ui.label("AI connection", "h2"), form.getView(), actions);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(32));
        panel.setMinWidth(400);
        panel.setPrefWidth(480);
        panel.setMaxWidth(480);
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

        AnimationHelper.applyStaggeredFadeIn(List.of(kicker, headline, appearance, statsBox), 80);
        AnimationHelper.applySlideUp(panel, 500);
    }

    private static void flash(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        AnimationHelper.applyFadeIn(label, 200);
        PauseTransition hide = new PauseTransition(Duration.seconds(2.5));
        hide.setOnFinished(e -> label.setVisible(false));
        hide.play();
    }

    public VBox getView() {
        return view;
    }
}
