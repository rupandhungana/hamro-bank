package com.bankingsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewNavigator {
    private static Stage stage;

    private ViewNavigator() {
    }

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("HamroBank");
        stage.setMaximized(true);
    }

    public static void showLogin() {
        loadScene("/com/bankingsystem/view/Login.fxml");
    }

    public static void showRegister() {
        loadScene("/com/bankingsystem/view/Register.fxml");
    }

    public static void showDashboard() {
        loadScene("/com/bankingsystem/view/Dashboard.fxml");
    }

    public static void showTransfer() {
        loadScene("/com/bankingsystem/view/Transfer.fxml");
    }

    public static void showTransactions() {
        loadScene("/com/bankingsystem/view/Transactions.fxml");
    }

    public static void showAdmin() {
        loadScene("/com/bankingsystem/view/Admin.fxml");
    }

    private static void loadScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            String cssPath = "/com/bankingsystem/style/app.css";
            var cssUrl = ViewNavigator.class.getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}