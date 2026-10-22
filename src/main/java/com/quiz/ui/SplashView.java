package com.quiz.ui;

import com.quiz.controller.AppController;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Animated splash screen shown on app startup.
 * Monogram pop → wordmark rise → accent rule draws → auto-transition.
 */
public class SplashView {

    private VBox view;

    public SplashView() {
        view = new VBox(18);
        view.setAlignment(Pos.CENTER);
        view.getStyleClass().add("root");

        // === Monogram ===
        Label q = Ui.label("Q", "monogram-text");
        q.setStyle("-fx-font-size: 58px;");
        StackPane tile = new StackPane(q);
        tile.getStyleClass().add("monogram");
        tile.setStyle("-fx-background-radius: 24px;");
        tile.setMinSize(104, 104);
        tile.setMaxSize(104, 104);
        tile.setOpacity(0);
        tile.setScaleX(0.4);
        tile.setScaleY(0.4);
        tile.setRotate(-12);

        // === Wordmark ===
        TextFlow title = Ui.headline("Quiz Master", 64);
        title.setMaxWidth(Region.USE_PREF_SIZE);
        title.setOpacity(0);
        title.setTranslateY(18);

        // === Accent rule ===
        Region rule = new Region();
        rule.setStyle("-fx-background-color: -c-accent; -fx-background-radius: 2;");
        rule.setMinHeight(3);
        rule.setMaxHeight(3);
        rule.setMaxWidth(220);
        rule.setScaleX(0);

        Label subtitle = Ui.label("AI-POWERED TRIVIA  ·  OLLAMA CLOUD", "eyebrow");
        subtitle.setOpacity(0);

        view.getChildren().addAll(tile, title, rule, subtitle);

        // === Animation timeline ===

        // 1. Monogram: spring in with a little twist
        ScaleTransition tileScale = new ScaleTransition(Duration.millis(650), tile);
        tileScale.setToX(1);
        tileScale.setToY(1);
        tileScale.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                // ease-out-back: overshoots slightly, then settles
                double c = 1.7, u = t - 1;
                return 1 + (c + 1) * u * u * u + c * u * u;
            }
        });
        RotateTransition tileRot = new RotateTransition(Duration.millis(650), tile);
        tileRot.setToAngle(0);
        tileRot.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition tileFade = new FadeTransition(Duration.millis(300), tile);
        tileFade.setToValue(1);

        // 2. Wordmark rises
        FadeTransition titleFade = new FadeTransition(Duration.millis(500), title);
        titleFade.setToValue(1);
        titleFade.setDelay(Duration.millis(350));
        TranslateTransition titleSlide = new TranslateTransition(Duration.millis(500), title);
        titleSlide.setToY(0);
        titleSlide.setDelay(Duration.millis(350));
        titleSlide.setInterpolator(Interpolator.EASE_OUT);

        // 3. Accent rule draws from the centre out
        ScaleTransition ruleDraw = new ScaleTransition(Duration.millis(600), rule);
        ruleDraw.setToX(1);
        ruleDraw.setDelay(Duration.millis(650));
        ruleDraw.setInterpolator(Interpolator.EASE_BOTH);

        // 4. Subtitle
        FadeTransition subFade = new FadeTransition(Duration.millis(400), subtitle);
        subFade.setToValue(1);
        subFade.setDelay(Duration.millis(900));

        new ParallelTransition(tileScale, tileRot, tileFade, titleFade, titleSlide, ruleDraw, subFade).play();

        // 5. Fade out and transition
        PauseTransition holdPause = new PauseTransition(Duration.millis(2000));
        holdPause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(350), view);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> AppController.afterSplash());
            fadeOut.play();
        });
        holdPause.play();
    }

    public VBox getView() {
        return view;
    }
}
