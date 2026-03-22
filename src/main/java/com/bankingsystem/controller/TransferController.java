package com.bankingsystem.controller;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.User;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.BankingService;
import com.bankingsystem.util.SessionContext;
import com.bankingsystem.util.ValidationUtil;
import com.bankingsystem.util.ViewNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

import java.math.BigDecimal;
import java.sql.SQLException;

public class TransferController {
    @FXML private Label sidebarNameLabel;
    @FXML private Label balanceLabel;
    @FXML private TextField receiverAccountField;
    @FXML private TextField amountField;
    @FXML private TextField noteField;
    @FXML private Label errorLabel;

    // Modal Dialog Components
    @FXML private StackPane modalOverlay;
    @FXML private VBox modalContainer;
    @FXML private StackPane modalIconContainer;
    @FXML private Label modalIcon;
    @FXML private Label modalTitle;
    @FXML private Label modalBody;
    @FXML private Button modalCancelBtn;
    @FXML private Button modalConfirmBtn;
    @FXML private Button modalCloseBtn;

    private final BankingService bankingService = new BankingService();
    private final AuthService authService = new AuthService();

    private PendingTransfer pendingTransfer;

    @FXML
    private void initialize() {
        User user = SessionContext.getCurrentUser();
        Account account = SessionContext.getCurrentAccount();

        if (user != null) {
            sidebarNameLabel.setText("Welcome, " + user.getFullName());
        }
        if (account != null) {
            balanceLabel.setText("NPR " + String.format("%.2f", account.getBalance()));
        }

        receiverAccountField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.length() > 24) return null;
            if (!text.matches("[A-Za-z0-9 ]*")) return null;
            return change;
        }));

        amountField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) return change;
            if (!text.matches("\\d*(\\.\\d{0,2})?")) return null;
            return change;
        }));

        hideConfirmModal();
        hideSuccessModal();
    }

    @FXML
    private void onConfirm() {
        errorLabel.setText("");

        String receiver = receiverAccountField.getText().trim();
        String amountText = amountField.getText().trim();
        String note = noteField.getText().trim();

        if (receiver.isEmpty() || amountText.isEmpty()) {
            errorLabel.setText("Account and amount are required");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);
            Account senderAccount = SessionContext.getCurrentAccount();

            if (senderAccount == null) {
                errorLabel.setText("Session error: No account found");
                return;
            }
            if (senderAccount.getAccountNumber().equals(receiver)) {
                errorLabel.setText("Cannot transfer to self");
                return;
            }
            if (!ValidationUtil.isValidAmount(amount)) {
                errorLabel.setText("Amount must be positive");
                return;
            }
            if (senderAccount.getBalance().compareTo(amount) < 0) {
                errorLabel.setText("Insufficient balance");
                return;
            }

            pendingTransfer = new PendingTransfer(
                    senderAccount.getAccountNumber(),
                    receiver,
                    amount,
                    note,
                    senderAccount.getBalance().subtract(amount)
            );
            
            // Show confirmation modal
            modalTitle.setText("Confirm Transfer");
            modalBody.setText("Send NPR " + String.format("%.2f", amount) + " to account " + receiver + "?");
            modalIcon.setText("!");
            modalIconContainer.getStyleClass().removeAll("modal-icon-success");
            modalIconContainer.getStyleClass().add("modal-icon-wait");
            
            modalCancelBtn.setVisible(true);
            modalCancelBtn.setManaged(true);
            modalConfirmBtn.setVisible(true);
            modalConfirmBtn.setManaged(true);
            modalCloseBtn.setVisible(false);
            modalCloseBtn.setManaged(false);
            modalConfirmBtn.setOnAction(e -> onConfirmModalTransfer());
            modalOverlay.setVisible(true);
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid amount format");
        }
    }

    @FXML
    private void onCancel() {
        receiverAccountField.clear();
        amountField.clear();
        noteField.clear();
        errorLabel.setText("");
    }

    @FXML
    private void onConfirmModalTransfer() {
        if (pendingTransfer == null) {
            onCloseModal();
            return;
        }

        try {
            bankingService.transfer(
                    pendingTransfer.senderAccount,
                    pendingTransfer.receiverAccount,
                    pendingTransfer.amount,
                    pendingTransfer.note
            );

            Account updated = bankingService.getAccountByNumber(pendingTransfer.senderAccount)
                    .orElseThrow(() -> new IllegalStateException("Account not found"));
            SessionContext.setCurrentAccount(updated);
            balanceLabel.setText("NPR " + String.format("%.2f", updated.getBalance()));

            receiverAccountField.clear();
            amountField.clear();
            noteField.clear();
            errorLabel.setText("");

            // Show success in modal
            modalTitle.setText("Transfer Success");
            modalBody.setText("NPR " + String.format("%.2f", pendingTransfer.amount) + " has been sent successfully to " + pendingTransfer.receiverAccount + ".");
            modalIcon.setText("✓");
            modalIconContainer.getStyleClass().add("modal-icon-success");
            
            modalCancelBtn.setVisible(false);
            modalCancelBtn.setManaged(false);
            modalConfirmBtn.setVisible(false);
            modalConfirmBtn.setManaged(false);
            modalCloseBtn.setVisible(true);
            modalCloseBtn.setManaged(true);

        } catch (Exception e) {
            onCloseModal();
            errorLabel.setText("Transfer failed: " + e.getMessage());
        }
    }

    @FXML
    private void onCloseModal() {
        modalOverlay.setVisible(false);
        if (modalCloseBtn.isVisible()) {
            onBack();
        }
    }

    @FXML private void onBack() { ViewNavigator.showDashboard(); }
    @FXML private void onTransactions() { ViewNavigator.showTransactions(); }

    @FXML
    private void onLogout() {
        try {
            authService.logout();
            ViewNavigator.showLogin();
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Logout error: " + e.getMessage());
        }
    }

    private void hideConfirmModal() { }
    private void hideSuccessModal() { }

    private static final class PendingTransfer {
        private final String senderAccount;
        private final String receiverAccount;
        private final BigDecimal amount;
        private final String note;
        private final BigDecimal remainingBalance;

        private PendingTransfer(String senderAccount,
                                String receiverAccount,
                                BigDecimal amount,
                                String note,
                                BigDecimal remainingBalance) {
            this.senderAccount = senderAccount;
            this.receiverAccount = receiverAccount;
            this.amount = amount;
            this.note = note;
            this.remainingBalance = remainingBalance;
        }
    }
}
