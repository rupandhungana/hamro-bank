package com.bankingsystem.controller;

import com.bankingsystem.model.AccountType;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.util.ViewNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RegisterController {

    // FXML Component References
    @FXML private TextField      fullNameField;
    @FXML private TextField      phoneField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField      dobField;
    @FXML private TextField      addressField;
    @FXML private TextField      nationalIdField;
    @FXML private TextField      emailField;
    @FXML private PasswordField  passwordField;
    @FXML private ComboBox<AccountType> accountTypeCombo;
    @FXML private Label          errorLabel;

    private final AuthService authService = new AuthService();

    // Initialization

    @FXML
    private void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.getSelectionModel().selectFirst();

        accountTypeCombo.setItems(FXCollections.observableArrayList(AccountType.values()));
        accountTypeCombo.getSelectionModel().select(AccountType.SAVINGS);
    }

    // Action Handlers

    @FXML
    private void onRegister() {
        errorLabel.setText("");

        String fullName   = fullNameField.getText().trim();
        String phone      = phoneField.getText().trim();
        String gender     = genderCombo.getValue();
        String dobText    = dobField.getText().trim();
        String address    = addressField.getText().trim();
        String nationalId = nationalIdField.getText().trim();
        String email      = emailField.getText().trim();
        String password   = passwordField.getText();
        AccountType accountType = accountTypeCombo.getSelectionModel().getSelectedItem();

        // Basic presence checks
        if (fullName.isEmpty() || phone.isEmpty() || dobText.isEmpty()
                || address.isEmpty() || nationalId.isEmpty()
                || email.isEmpty()   || password.isEmpty() || accountType == null) {
            errorLabel.setText("All fields are required.");
            return;
        }

        // Detailed input validation
        if (fullName.length() < 2) {
            errorLabel.setText("Full name must be at least 2 characters.");
            return;
        }
        if (!phone.matches("\\d{7,15}")) {
            errorLabel.setText("Phone number must be 7–15 digits.");
            return;
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobText);
            if (dob.isAfter(LocalDate.now().minusYears(10))) {
                errorLabel.setText("Date of birth must be at least 10 years ago.");
                return;
            }
        } catch (DateTimeParseException e) {
            errorLabel.setText("Date of birth must be in YYYY-MM-DD format.");
            return;
        }

        if (nationalId.length() < 4) {
            errorLabel.setText("National ID must be at least 4 characters.");
            return;
        }

        // Call authentication service to register the user
        try {
            authService.register(
                    fullName, phone, email, address, dob,
                    gender.toUpperCase(), nationalId.toUpperCase(),
                    password, accountType
            );
            ViewNavigator.showLogin();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onBackToLogin() {
        ViewNavigator.showLogin();
    }

    @FXML
    private void onClearForm() {
        fullNameField.clear();
        phoneField.clear();
        dobField.clear();
        addressField.clear();
        nationalIdField.clear();
        emailField.clear();
        passwordField.clear();
        genderCombo.getSelectionModel().selectFirst();
        accountTypeCombo.getSelectionModel().select(AccountType.SAVINGS);
        errorLabel.setText("");
    }
}
