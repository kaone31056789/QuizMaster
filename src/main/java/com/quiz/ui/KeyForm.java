package com.quiz.ui;

import com.quiz.controller.AppController;
import com.quiz.util.AIQuestionGenerator;
import com.quiz.util.ConfigManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Ollama API key + model inputs shared by SetupView and SettingsView:
 * masked key with show/hide, "Test key" connection check, and a note
 * showing exactly where the key is stored on disk.
 */
public class KeyForm {

    private final VBox view;
    private final PasswordField keyHidden = new PasswordField();
    private final TextField keyShown = new TextField();
    private final TextField modelField = new TextField();
    private final Circle statusDot;
    private final Label statusText;
    private boolean revealed = false;

    public KeyForm(String initialKey) {
        // === Key field (masked / revealed share one value) ===
        keyHidden.setPromptText("Paste your key from ollama.com");
        keyShown.setPromptText("Paste your key from ollama.com");
        keyHidden.getStyleClass().add("input-mono");
        keyShown.getStyleClass().add("input-mono");
        keyShown.textProperty().bindBidirectional(keyHidden.textProperty());
        keyHidden.setText(initialKey);
        keyShown.setVisible(false);
        keyShown.setManaged(false);

        StackPane keyStack = new StackPane(keyHidden, keyShown);
        HBox.setHgrow(keyStack, Priority.ALWAYS);

        Button eyeBtn = Ui.iconButton(Ui.I_EYE, "Show key");
        eyeBtn.setOnAction(e -> {
            revealed = !revealed;
            keyShown.setVisible(revealed);
            keyShown.setManaged(revealed);
            keyHidden.setVisible(!revealed);
            keyHidden.setManaged(!revealed);
            eyeBtn.setGraphic(Ui.icon(revealed ? Ui.I_EYE_OFF : Ui.I_EYE, 18));
            eyeBtn.getTooltip().setText(revealed ? "Hide key" : "Show key");
        });

        HBox keyRow = new HBox(8, keyStack, eyeBtn);
        keyRow.setAlignment(Pos.CENTER_LEFT);

        // === Status line ===
        statusDot = Ui.dot(null);
        statusText = Ui.label("", "muted");
        statusText.setWrapText(true);
        HBox status = new HBox(8, statusDot, statusText);
        status.setAlignment(Pos.CENTER_LEFT);
        refreshStatus();
        keyHidden.textProperty().addListener((o, a, b) -> refreshStatus());

        Button getKey = Ui.button("Get a free key →", "btn-link");
        getKey.setOnAction(e -> AppController.openUrl("https://ollama.com/settings/keys"));

        Button testBtn = Ui.button("Test key", "btn-outline", Ui.I_ZAP);
        testBtn.setOnAction(e -> runTest(testBtn));

        HBox keyActions = new HBox(14, testBtn, getKey);
        keyActions.setAlignment(Pos.CENTER_LEFT);

        // === Model ===
        modelField.setText(ConfigManager.getModel());
        modelField.setPromptText(AIQuestionGenerator.DEFAULT_MODEL);
        modelField.getStyleClass().add("input-mono");
        Label modelHint = Ui.wrapped("Tried first. If it fails, QuizMaster falls back to "
                + "ministral-3, gemini-3-flash, qwen3.5 and gemma4 automatically.", 420, "muted");

        // === Where the key lives ===
        Label whereTitle = Ui.label("WHERE YOUR KEY IS STORED", "eyebrow");
        Label wherePath = Ui.wrapped(ConfigManager.getConfigFile(), 420, "mono");
        Label whereNote = Ui.wrapped("Saved only on this computer, in plain text, outside the project folder "
                + "— so it never ends up in Git. You can also set the OLLAMA_API_KEY "
                + "environment variable instead.", 420, "muted");
        Button openFolder = Ui.button("Open folder", "btn-link");
        openFolder.setOnAction(e -> AppController.openConfigFolder());

        VBox where = new VBox(6, whereTitle, wherePath, whereNote, openFolder);
        where.getStyleClass().add("panel-flat");

        view = new VBox(10,
                Ui.label("OLLAMA API KEY", "eyebrow"), keyRow, status, keyActions,
                gap(), Ui.label("PREFERRED MODEL", "eyebrow"), modelField, modelHint,
                gap(), where);

        if (ConfigManager.isApiKeyFromEnv()) {
            Label envNote = Ui.wrapped("The OLLAMA_API_KEY environment variable is set, "
                    + "so it overrides the key saved here.", 420, "muted");
            envNote.getStyleClass().add("status-warn");
            view.getChildren().add(3, envNote);
        }
    }

    private static javafx.scene.layout.Region gap() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setMinHeight(6);
        return r;
    }

    private void refreshStatus() {
        String key = getKey();
        if (key.isEmpty()) {
            setStatus(null, "No key — you'll play with built-in questions");
        } else if (key.equals(ConfigManager.getSavedApiKey())) {
            setStatus("dot-ok", "Key saved · ••••" + tail(key));
        } else {
            setStatus(null, "Unsaved changes");
        }
    }

    private void setStatus(String dotVariant, String text) {
        Ui.setDot(statusDot, dotVariant);
        statusText.setText(text);
    }

    private static String tail(String key) {
        return key.length() <= 4 ? key : key.substring(key.length() - 4);
    }

    private void runTest(Button testBtn) {
        String key = getKey();
        if (key.isEmpty()) {
            setStatus("dot-bad", "Paste a key first");
            return;
        }
        testBtn.setDisable(true);
        setStatus(null, "Testing connection…");

        Thread t = new Thread(() -> {
            AIQuestionGenerator gen = new AIQuestionGenerator("https://ollama.com", getModel(), key);
            String err = gen.testConnection();
            Platform.runLater(() -> {
                testBtn.setDisable(false);
                if (err == null) {
                    // A key that works is saved straight away — no need to press Save
                    ConfigManager.setApiKey(key);
                    ConfigManager.setModel(getModel());
                    setStatus("dot-ok", "Connected & saved · " + gen.getUsedModel() + " responded");
                } else {
                    setStatus("dot-bad", "Failed · " + err);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    /** True when the field holds a key different from the one on disk. */
    public boolean hasUnsavedKey() {
        return !getKey().isEmpty() && !getKey().equals(ConfigManager.getSavedApiKey());
    }

    public String getKey() {
        return keyHidden.getText() == null ? "" : keyHidden.getText().trim();
    }

    public String getModel() {
        String m = modelField.getText() == null ? "" : modelField.getText().trim();
        return m.isEmpty() ? AIQuestionGenerator.DEFAULT_MODEL : m;
    }

    /** Persist key + model and refresh the status line. */
    public void save() {
        String key = getKey();
        if (key.isEmpty()) {
            ConfigManager.clearApiKey();
        } else {
            ConfigManager.setApiKey(key);
        }
        ConfigManager.setModel(getModel());
        refreshStatus();
    }

    public void clear() {
        keyHidden.clear();
        ConfigManager.clearApiKey();
        refreshStatus();
    }

    public void focusKey() {
        Platform.runLater(keyHidden::requestFocus);
    }

    public VBox getView() {
        return view;
    }
}
