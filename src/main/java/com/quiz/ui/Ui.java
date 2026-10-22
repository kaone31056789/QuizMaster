package com.quiz.ui;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Small component kit shared by all screens: icons, buttons,
 * segmented controls, a toggle switch and stat blocks.
 * All colours come from the CSS tokens in styles.css.
 */
public final class Ui {

    private Ui() {}

    // === Icons — 24px grid, stroked paths (JavaFX can't render most colour emoji) ===

    public static final String I_SUN = "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z M12 2v2 M12 20v2 M4.9 4.9l1.4 1.4 M17.7 17.7l1.4 1.4 M2 12h2 M20 12h2 M6.3 17.7l-1.4 1.4 M19.1 4.9l-1.4 1.4";
    public static final String I_MOON = "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z";
    public static final String I_SLIDERS = "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6";
    public static final String I_ARROW_LEFT = "M19 12H5 M12 19l-7-7 7-7";
    public static final String I_ARROW_RIGHT = "M5 12h14 M12 5l7 7-7 7";
    public static final String I_CLOCK = "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z M12 6v6l4 2";
    public static final String I_SKIP = "M5 4l10 8-10 8z M19 5v14";
    public static final String I_HALF = "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z M12 2v20 M12 7h5 M12 12h7 M12 17h5";
    public static final String I_CHECK = "M20 6L9 17l-5-5";
    public static final String I_X = "M18 6L6 18 M6 6l12 12";
    public static final String I_FLAME = "M12 2c1 4 6 6 6 12a6 6 0 0 1-12 0c0-3 1.5-5 3-6.5c0 2.5 1 3.5 2.5 3.5c0-3-1-6 0.5-9z";
    public static final String I_SPARKLE = "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z";
    public static final String I_TROPHY = "M8 21h8 M12 17v4 M7 4h10v5a5 5 0 0 1-10 0z M7 6H4a3 3 0 0 0 3 4 M17 6h3a3 3 0 0 1-3 4";
    public static final String I_KEY = "M7.5 13a4.5 4.5 0 1 0 0 9a4.5 4.5 0 1 0 0-9z M10.7 14.3L21 4 M17 8l3 3 M14.5 10.5l2 2";
    public static final String I_EYE = "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z";
    public static final String I_EYE_OFF = "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z M3 3l18 18";
    public static final String I_ZAP = "M13 2L3 14h9l-1 8 10-12h-9z";
    public static final String I_FOLDER = "M3 6.5A1.5 1.5 0 0 1 4.5 5H9l2 2.5h8.5A1.5 1.5 0 0 1 21 9v9.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z";
    public static final String I_REPLAY = "M3 12a9 9 0 1 0 3-6.7 M3 3v6h6";
    public static final String I_HOME = "M3 11l9-8 9 8 M5 9.5V21h14V9.5";
    public static final String I_ALERT = "M12 3l10 18H2z M12 10v4 M12 17.5v.5";

    /** A stroked icon of the given pixel size. Extra style classes recolour it (icon-accent, icon-muted...). */
    public static Node icon(String path, double size, String... classes) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        svg.getStyleClass().addAll(classes);
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);

        StackPane box = new StackPane(new Group(svg));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    // === Text ===

    public static Label label(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        return l;
    }

    public static Label wrapped(String text, double maxWidth, String... classes) {
        Label l = label(text, classes);
        l.setWrapText(true);
        l.setMaxWidth(maxWidth);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    public static Region rule() {
        Region r = new Region();
        r.getStyleClass().add("rule");
        return r;
    }

    public static Region vrule() {
        Region r = new Region();
        r.getStyleClass().add("rule-v");
        return r;
    }

    public static Region hgrow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region vgrow() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Big serif headline ending in an accent-coloured full stop. */
    public static javafx.scene.text.TextFlow headline(String text, double size) {
        javafx.scene.text.Text main = new javafx.scene.text.Text(text);
        main.getStyleClass().add("display");
        javafx.scene.text.Text stop = new javafx.scene.text.Text(".");
        stop.getStyleClass().add("display-accent");
        String fs = "-fx-font-size: " + size + "px;";
        main.setStyle(fs);
        stop.setStyle(fs);
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(main, stop);
        flow.setLineSpacing(-size * 0.26);
        return flow;
    }

    /** "Q" monogram tile + wordmark used in top bars. */
    public static HBox brand() {
        Label q = label("Q", "monogram-text");
        StackPane tile = new StackPane(q);
        tile.getStyleClass().add("monogram");
        tile.setMinSize(36, 36);
        tile.setMaxSize(36, 36);
        HBox box = new HBox(10, tile, label("Quiz Master", "wordmark"));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // === Buttons ===

    public static Button button(String text, String variant) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", variant);
        return b;
    }

    public static Button button(String text, String variant, String iconPath) {
        Button b = button(text, variant);
        b.setGraphic(icon(iconPath, 18));
        return b;
    }

    public static Button iconButton(String iconPath, String tooltip) {
        Button b = new Button();
        b.getStyleClass().add("icon-btn");
        b.setGraphic(icon(iconPath, 18));
        b.setTooltip(new Tooltip(tooltip));
        b.setAccessibleText(tooltip);
        return b;
    }

    // === Segmented control ===

    /**
     * Pill-shaped segmented control. Returns the container; the group
     * is stored in its properties under "group".
     */
    public static <T> HBox segmented(List<T> values, List<String> labels, T selected,
                                     boolean mono, Consumer<T> onChange) {
        HBox box = new HBox();
        box.getStyleClass().add("segmented");
        ToggleGroup group = new ToggleGroup();

        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            ToggleButton tb = new ToggleButton(labels.get(i));
            tb.getStyleClass().add("seg");
            if (mono) tb.getStyleClass().add("seg-mono");
            tb.setToggleGroup(group);
            tb.setUserData(value);
            tb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tb, Priority.ALWAYS);
            if (value.equals(selected)) tb.setSelected(true);
            box.getChildren().add(tb);
        }

        // Never allow "nothing selected"
        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
                return;
            }
            @SuppressWarnings("unchecked")
            T v = (T) newT.getUserData();
            if (onChange != null) onChange.accept(v);
        });

        box.getProperties().put("group", group);
        return box;
    }

    // === Switch ===

    /** iOS-style toggle switch. */
    public static StackPane toggleSwitch(BooleanProperty on) {
        double w = 46, h = 26, r = 10;

        Region track = new Region();
        track.getStyleClass().add("switch-track");
        track.setMinSize(w, h);
        track.setMaxSize(w, h);

        Circle knob = new Circle(r);
        knob.getStyleClass().add("switch-knob");
        knob.setMouseTransparent(true);
        double travel = (w - h) / 2;

        StackPane sw = new StackPane(track, knob);
        sw.setMinSize(w, h);
        sw.setMaxSize(w, h);
        sw.setFocusTraversable(true);
        sw.setAccessibleText("Toggle");

        Runnable sync = () -> {
            if (on.get()) {
                if (!track.getStyleClass().contains("on")) track.getStyleClass().add("on");
            } else {
                track.getStyleClass().remove("on");
            }
            TranslateTransition tt = new TranslateTransition(Duration.millis(160), knob);
            tt.setToX(on.get() ? travel : -travel);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        };
        knob.setTranslateX(on.get() ? travel : -travel);
        if (on.get()) track.getStyleClass().add("on");

        on.addListener((o, a, b) -> sync.run());
        sw.setOnMouseClicked(e -> { if (!sw.isDisabled()) on.set(!on.get()); });
        sw.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE: case ENTER:
                    if (!sw.isDisabled()) on.set(!on.get());
                    break;
                default:
            }
        });
        sw.disabledProperty().addListener((o, a, b) -> track.setDisable(b));
        return sw;
    }

    public static BooleanProperty bool(boolean initial) {
        return new SimpleBooleanProperty(initial);
    }

    // === Status dot ===

    public static Circle dot(String variant) {
        Circle c = new Circle(4);
        c.getStyleClass().addAll("dot", variant);
        return c;
    }

    public static void setDot(Circle c, String variant) {
        c.getStyleClass().removeAll("dot-ok", "dot-bad");
        if (variant != null) c.getStyleClass().add(variant);
    }

    // === Stat blocks ===

    public static VBox stat(String caption, String value, String valueClass) {
        VBox v = new VBox(4, label(caption, "eyebrow"), label(value, valueClass));
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    /** A row of stats separated by thin vertical rules. */
    public static HBox statRow(double spacing, Node... stats) {
        HBox row = new HBox(spacing);
        row.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < stats.length; i++) {
            if (i > 0) {
                Region r = vrule();
                r.setMinHeight(36);
                row.getChildren().add(r);
            }
            row.getChildren().add(stats[i]);
        }
        return row;
    }

    /** Thousands separator for scoreboard numbers. */
    public static String num(int n) {
        return String.format("%,d", n);
    }
}
