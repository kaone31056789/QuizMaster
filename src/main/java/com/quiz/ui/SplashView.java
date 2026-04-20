package com.quiz.ui;

import com.quiz.controller.AppController;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Animated splash screen shown on app startup.
 * Logo reveal → title fade → subtitle → auto-transition.
 */
public class SplashView {

    private VBox view;

    public SplashView() {
        view = new VBox(6);
        view.setAlignment(Pos.CENTER);
        view.getStyleClass().add("root");

        // === Logo ===
        Label logo = new Label("\uD83E\uDDE0"); // 🧠
        logo.setStyle("-fx-font-size: 72px;");
        logo.setOpacity(0);
        logo.setScaleX(0.3);
        logo.setScaleY(0.3);

        // === Title ===
        Label title = new Label("Quiz Master");
        title.setStyle(
            "-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #1e1e3a;"
        );
        title.setOpacity(0);
        title.setTranslateY(20);

        // === Subtitle ===
        Label subtitle = new Label("Powered by AI");
        subtitle.setStyle(
            "-fx-font-size: 14px; -fx-text-fill: #9090b0; -fx-letter-spacing: 4px;"
        );
        subtitle.setOpacity(0);

        // === Dots loading ===
        Label dots = new Label("\u2022  \u2022  \u2022");
        dots.setStyle("-fx-font-size: 18px; -fx-text-fill: #b0b0d0;");
        dots.setOpacity(0);

        view.getChildren().addAll(logo, title, subtitle, dots);

        // === Animation timeline ===

        // 1. Logo: scale + fade in with bounce (0ms - 600ms)
        ScaleTransition logoScale = new ScaleTransition(Duration.millis(600), logo);
        logoScale.setFromX(0.3);
        logoScale.setFromY(0.3);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition logoFade = new FadeTransition(Duration.millis(400), logo);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);

        ParallelTransition logoAnim = new ParallelTransition(logoScale, logoFade);

        // 2. Logo glow pulse
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(124, 92, 231, 0.4));
        glow.setRadius(0);

        Timeline glowPulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 0)),
            new KeyFrame(Duration.millis(400), new KeyValue(glow.radiusProperty(), 25)),
            new KeyFrame(Duration.millis(800), new KeyValue(glow.radiusProperty(), 12))
        );
        logo.setEffect(glow);

        // 3. Title: slide up + fade in (400ms delay)
        FadeTransition titleFade = new FadeTransition(Duration.millis(500), title);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);
        titleFade.setDelay(Duration.millis(400));

        TranslateTransition titleSlide = new TranslateTransition(Duration.millis(500), title);
        titleSlide.setFromY(20);
        titleSlide.setToY(0);
        titleSlide.setDelay(Duration.millis(400));
        titleSlide.setInterpolator(Interpolator.EASE_OUT);

        // 4. Subtitle: fade in (700ms delay)
        FadeTransition subFade = new FadeTransition(Duration.millis(400), subtitle);
        subFade.setFromValue(0);
        subFade.setToValue(1);
        subFade.setDelay(Duration.millis(700));

        // 5. Dots: fade in with pulse (900ms delay)
        FadeTransition dotsFade = new FadeTransition(Duration.millis(300), dots);
        dotsFade.setFromValue(0);
        dotsFade.setToValue(0.6);
        dotsFade.setDelay(Duration.millis(900));

        Timeline dotsPulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(dots.opacityProperty(), 0.3)),
            new KeyFrame(Duration.millis(500), new KeyValue(dots.opacityProperty(), 0.8)),
            new KeyFrame(Duration.millis(1000), new KeyValue(dots.opacityProperty(), 0.3))
        );
        dotsPulse.setCycleCount(3);
        dotsPulse.setDelay(Duration.millis(1000));

        // Play all
        ParallelTransition allAnims = new ParallelTransition(
            logoAnim, glowPulse, titleFade, titleSlide, subFade, dotsFade, dotsPulse
        );
        allAnims.play();

        // 6. Fade out and transition (after ~2.5s)
        PauseTransition holdPause = new PauseTransition(Duration.millis(2400));
        holdPause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), view);
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
