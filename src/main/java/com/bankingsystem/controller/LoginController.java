package com.bankingsystem.controller;

import com.bankingsystem.model.Role;
import com.bankingsystem.model.User;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.util.ViewNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String identifier = usernameField.getText().trim();
        String password   = passwordField.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email/phone and password are required.");
            return;
        }

        try {
            User user = authService.login(identifier, password);
            if (user.getRole() == Role.ADMIN) {
                ViewNavigator.showAdmin();
            } else {
                ViewNavigator.showDashboard();
            }
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onOpenRegister() {
        ViewNavigator.showRegister();
    }
}
