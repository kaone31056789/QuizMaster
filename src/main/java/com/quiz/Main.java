package com.quiz;

import com.quiz.controller.AppController;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppController.setPrimaryStage(primaryStage);
        AppController.showInitialScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
