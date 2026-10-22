package com.quiz.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class AnimationHelper {

    /**
     * Smooth fade-in from transparent to fully visible.
     */
    public static void applyFadeIn(Node node, int durationMs) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    public static void applyScreenShake(Node node) {
        TranslateTransition t1 = new TranslateTransition(Duration.millis(50), node);
        t1.setByX(15);
        t1.setCycleCount(2);
        t1.setAutoReverse(true);
        
        TranslateTransition t2 = new TranslateTransition(Duration.millis(50), node);
        t2.setByX(-15);
        t2.setCycleCount(2);
        t2.setAutoReverse(true);
        
        t1.setOnFinished(e -> t2.play());
        t1.play();
    }

    public static void spawnComboGraphic(javafx.scene.layout.Pane parent, int streak) {
        javafx.scene.control.Label comboLabel = new javafx.scene.control.Label(streak + "× combo");
        comboLabel.getStyleClass().add("combo-text");
        comboLabel.setMouseTransparent(true);

        parent.getChildren().add(comboLabel);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(400), comboLabel);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1.3); st.setToY(1.3);
        st.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition ft = new FadeTransition(Duration.millis(400), comboLabel);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        ft.setDelay(Duration.millis(600)); // fade out after a bit
        
        st.play();
        ft.play();
        
        ft.setOnFinished(e -> parent.getChildren().remove(comboLabel));
    }

    /**
     * Slide-up entrance: node rises from below while fading in.
     */
    public static void applySlideUp(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateY(40);

        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromY(40);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Bounce-in scale effect for dramatic score reveals.
     */
    public static void applyBounceIn(Node node, double durationMs) {
        node.setOpacity(0);
        node.setScaleX(0.3);
        node.setScaleY(0.3);

        FadeTransition fade = new FadeTransition(Duration.millis(durationMs * 0.4), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(durationMs * 0.6), node);
        scaleUp.setFromX(0.3);
        scaleUp.setFromY(0.3);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleBounce = new ScaleTransition(Duration.millis(durationMs * 0.4), node);
        scaleBounce.setFromX(1.1);
        scaleBounce.setFromY(1.1);
        scaleBounce.setToX(1.0);
        scaleBounce.setToY(1.0);
        scaleBounce.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition entry = new ParallelTransition(fade, scaleUp);
        new SequentialTransition(entry, scaleBounce).play();
    }

    /**
     * Pulsing glow effect for urgency (e.g. timer running low).
     */
    public static Timeline applyPulse(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(400), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        // Return a timeline that can be stopped externally
        Timeline controller = new Timeline(new KeyFrame(Duration.INDEFINITE));
        controller.setOnFinished(e -> pulse.stop());
        return controller;
    }

    /**
     * Dramatic answer reveal: brief flash/highlight effect.
     */
    public static void applyRevealFlash(Node node, int flashCount) {
        FadeTransition flash = new FadeTransition(Duration.millis(150), node);
        flash.setFromValue(1.0);
        flash.setToValue(0.4);
        flash.setCycleCount(flashCount * 2);
        flash.setAutoReverse(true);
        flash.setInterpolator(Interpolator.EASE_BOTH);
        flash.play();
    }

    /**
     * Hover effect: subtle scale-up with easing.
     */
    public static void applyHoverEffect(Node node) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), node);
        scaleUp.setToX(1.03);
        scaleUp.setToY(1.03);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_OUT);

        node.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.play();
        });

        node.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.play();
        });
    }

    /**
     * Staggered fade-in for a list of nodes (each delayed slightly).
     */
    public static void applyStaggeredFadeIn(java.util.List<Node> nodes, double delayPerItemMs) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            n.setOpacity(0);
            n.setTranslateY(20);

            FadeTransition fade = new FadeTransition(Duration.millis(300), n);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * delayPerItemMs));
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), n);
            slide.setFromY(20);
            slide.setToY(0);
            slide.setDelay(Duration.millis(i * delayPerItemMs));
            slide.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, slide).play();
        }
    }
}
