package com.bankingsystem;

import com.bankingsystem.util.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        ViewNavigator.initialize(stage);
        ViewNavigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
