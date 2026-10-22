package com.quiz;

import com.quiz.controller.AppController;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;

public class Main extends Application {

    /** Bundled fonts (OFL / IBM Plex licence — see resources/fonts) */
    private static final String[] FONTS = {
            "DMSerifDisplay-Regular.ttf",
            "DMSerifDisplay-Italic.ttf",
            "SpaceMono-Regular.ttf",
            "SpaceMono-Bold.ttf",
            "IBMPlexSans-Regular.ttf",
            "IBMPlexSans-SemiBold.ttf",
            "IBMPlexSans-Bold.ttf"
    };

    @Override
    public void start(Stage primaryStage) {
        loadFonts();
        AppController.setHostServices(getHostServices());
        AppController.setPrimaryStage(primaryStage);
        AppController.showInitialScreen();
    }

    private static void loadFonts() {
        for (String file : FONTS) {
            try (InputStream in = Main.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null || Font.loadFont(in, 12) == null) {
                    System.err.println("Could not load font: " + file);
                }
            } catch (Exception e) {
                System.err.println("Could not load font " + file + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
