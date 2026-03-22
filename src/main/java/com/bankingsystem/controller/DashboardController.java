package com.bankingsystem.controller;

import com.bankingsystem.model.*;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.BankingService;
import com.bankingsystem.util.SessionContext;
import com.bankingsystem.util.ViewNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** 
 * Dashboard Controller orchestrates the main view shown to regular customers after login. 
 * Note: Avoid mixing heavy UI logic with controller flow. We delegate row creation to Builders. 
 */
public class DashboardController {

    // Sidebar
    @FXML
    private Label sidebarNameLabel;

    // Greeting row
    @FXML
    private Label greetingLabel;
    @FXML
    private Label dateLabel;

    // Balance card
    @FXML
    private Label accountTypeLabel;
    @FXML
    private Label nameLabel; // account holder
    @FXML
    private Label accountLabel; // account number
    @FXML
    private Label balanceLabel;

    // Stat cards
    @FXML
    private Label totalInLabel;
    @FXML
    private Label totalOutLabel;

    // Recent transactions list
    @FXML
    private VBox recentTxList;

    @FXML
    private Button hideBtn;
    @FXML
    private Label errorLabel;

    private final BankingService bankingService = new BankingService();
    private final AuthService authService = new AuthService();

    private boolean balanceHidden = true;
    private BigDecimal currentBalance = BigDecimal.ZERO;

    // Initialization and lifecycle methods

    @FXML
    private void initialize() {
        try {
            User user = SessionContext.getCurrentUser();
            Account account = SessionContext.getCurrentAccount();

            // Greeting & date
            if (user != null) {
                String fullName = user.getFullName() != null ? user.getFullName() : "User";
                String firstName = fullName.split("\\s+")[0];
                greetingLabel.setText("Hello, " + firstName);
                sidebarNameLabel.setText("Welcome, " + fullName);
                nameLabel.setText(fullName);
            } else {
                greetingLabel.setText("Hello");
                sidebarNameLabel.setText("Welcome");
            }

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);
            dateLabel.setText(LocalDate.now().format(dateFmt));

            // Balance card
            if (account != null) {
                currentBalance = account.getBalance() != null
                        ? account.getBalance()
                        : BigDecimal.ZERO;
                accountLabel.setText(account.getAccountNumber());
                if (account.getAccountType() != null) {
                    accountTypeLabel.setText(
                            capitalize(account.getAccountType().name()) + " Account");
                }
            } else {
                accountLabel.setText("—");
                accountTypeLabel.setText("No account");
            }

            refreshBalance();
            loadStats();
            loadRecentTransactions();

        } catch (Exception e) {
            // Show error in UI rather than crashing the FXML load
            if (errorLabel != null)
                errorLabel.setText("Dashboard error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Balance visibility toggle mechanism

    @FXML
    private void onToggleBalance() {
        balanceHidden = !balanceHidden;
        if (hideBtn != null) {
            hideBtn.setText(balanceHidden ? "Show" : "Hide");
        }
        refreshBalance();
    }

    private void refreshBalance() {
        balanceLabel.setText(balanceHidden
                ? "NPR XXXX"
                : "NPR " + currentBalance.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    // Statistics processing

    private void loadStats() {
        Account account = SessionContext.getCurrentAccount();
        if (account == null)
            return;
        try {
            List<Transaction> txList = bankingService.getTransactionsForAccount(account.getAccountNumber());

            BigDecimal totalIn = BigDecimal.ZERO;
            BigDecimal totalOut = BigDecimal.ZERO;
            String myAcc = account.getAccountNumber();

            for (Transaction tx : txList) {
                if (tx.getStatus() != TransactionStatus.SUCCESS)
                    continue;
                if (myAcc.equals(tx.getReceiverAccount()))
                    totalIn = totalIn.add(tx.getAmount());
                if (myAcc.equals(tx.getSenderAccount()))
                    totalOut = totalOut.add(tx.getAmount());
            }

            totalInLabel.setText("NPR " + totalIn.setScale(2, RoundingMode.HALF_UP).toPlainString());
            totalOutLabel.setText("NPR " + totalOut.setScale(2, RoundingMode.HALF_UP).toPlainString());
        } catch (Exception ignored) {
            // stats are supplementary — fail silently
        }
    }

    // Recent transaction list rendering

    private void loadRecentTransactions() {
        recentTxList.getChildren().clear();
        Account account = SessionContext.getCurrentAccount();
        if (account == null) {
            recentTxList.getChildren().add(emptyLabel("No account found."));
            return;
        }
        try {
            List<Transaction> txList = bankingService.getTransactionsForAccount(account.getAccountNumber());

            if (txList.isEmpty()) {
                recentTxList.getChildren().add(emptyLabel("No transactions yet."));
                return;
            }

            int limit = Math.min(txList.size(), 5);
            for (int i = 0; i < limit; i++) {
                Transaction tx = txList.get(i);
                boolean isIncoming = isIncomingTx(tx, account.getAccountNumber());
                
                recentTxList.getChildren().add(new TransactionRowBuilder(tx, isIncoming).build());
                
                if (i < limit - 1) {
                    Separator sep = new Separator();
                    sep.setPadding(new Insets(0, 20, 0, 72));
                    recentTxList.getChildren().add(sep);
                }
            }
        } catch (Exception e) {
            Label err = new Label("Failed to load transactions.");
            err.getStyleClass().add("error");
            err.setPadding(new Insets(12, 20, 12, 20));
            recentTxList.getChildren().add(err);
        }
    }

    /** Returns true when the given account is the RECEIVER of this transaction. */
    private boolean isIncomingTx(Transaction tx, String myAcc) {
        if (tx.getTransactionType() == TransactionType.ADMIN_DEPOSIT
                || tx.getSenderAccount() == null)
            return true;
        return myAcc.equals(tx.getReceiverAccount())
                && !myAcc.equals(tx.getSenderAccount());
    }

    /**
     * Component builder encapsulating the UI construction logic for a single transaction row.
     * Applies OOP Builder pattern to separate complex view logic from the main controller.
     */
    private class TransactionRowBuilder {
        private final Transaction tx;
        private final boolean isIncoming;

        public TransactionRowBuilder(Transaction tx, boolean isIncoming) {
            this.tx = tx;
            this.isIncoming = isIncoming;
        }

        public HBox build() {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 20, 14, 20));

            row.getChildren().addAll(
                buildIcon(),
                buildInfo(),
                buildAmountAndStatus()
            );

            return row;
        }

        private StackPane buildIcon() {
            StackPane iconCircle = new StackPane();
            iconCircle.setPrefSize(36, 36);
            iconCircle.setMinSize(36, 36);
            iconCircle.setMaxSize(36, 36);
            iconCircle.getStyleClass().add(isIncoming ? "tx-icon-in" : "tx-icon-out");

            Label arrowLbl = new Label(isIncoming ? "+" : "-");
            arrowLbl.getStyleClass().add(isIncoming ? "tx-arrow-in" : "tx-arrow-out");
            arrowLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            iconCircle.getChildren().add(arrowLbl);
            return iconCircle;
        }

        private VBox buildInfo() {
            String title;
            TransactionType type = tx.getTransactionType();
            
            if (type == TransactionType.ADMIN_DEPOSIT || tx.getSenderAccount() == null) {
                title = "Admin Deposit";
            } else if (isIncoming) {
                title = "From " + tx.getSenderAccount();
            } else {
                title = "To " + tx.getReceiverAccount();
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
            String datePart = tx.getCreatedAt() != null ? tx.getCreatedAt().format(fmt) : "";
            String typePart = capitalize(type.name().replace('_', ' '));

            Label titleLbl = new Label(title);
            titleLbl.getStyleClass().add("body-medium");

            Label subLbl = new Label(datePart + " - " + typePart);
            subLbl.getStyleClass().add("body-small");

            VBox info = new VBox(3, titleLbl, subLbl);
            HBox.setHgrow(info, Priority.ALWAYS);
            return info;
        }

        private VBox buildAmountAndStatus() {
            String prefix = isIncoming ? "+ NPR " : "- NPR ";
            Label amountLbl = new Label(prefix + tx.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            amountLbl.getStyleClass().add(isIncoming ? "tx-amount-in" : "tx-amount-out");

            Label statusLbl = new Label(capitalize(tx.getStatus().name()));
            if (tx.getStatus() == TransactionStatus.SUCCESS) {
                statusLbl.getStyleClass().addAll("badge", "badge-success");
            } else {
                statusLbl.getStyleClass().addAll("badge", "badge-error");
            }

            VBox amountBox = new VBox(4, amountLbl, statusLbl);
            amountBox.setAlignment(Pos.CENTER_RIGHT);
            return amountBox;
        }
    }

    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("body-small");
        lbl.setPadding(new Insets(20));
        return lbl;
    }

    // State refresh mechanisms

    @FXML
    private void onRefresh() {
        try {
            User u = SessionContext.getCurrentUser();
            if (u != null) {
                Account acc = bankingService.getAccountByUserId(u.getUserId()).orElse(null);
                if (acc != null) {
                    SessionContext.setCurrentAccount(acc);
                    currentBalance = acc.getBalance() != null
                            ? acc.getBalance()
                            : BigDecimal.ZERO;
                    accountLabel.setText(acc.getAccountNumber());
                }
            }
            refreshBalance();
            loadStats();
            loadRecentTransactions();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }

    // View Navigation handlers

    @FXML
    private void onTransfer() {
        ViewNavigator.showTransfer();
    }

    @FXML
    private void onTransferClick() {
        ViewNavigator.showTransfer();
    }

    @FXML
    private void onTransactions() {
        ViewNavigator.showTransactions();
    }

    @FXML
    private void onTransactionsClick() {
        ViewNavigator.showTransactions();
    }

    @FXML
    private void onLogout() {
        try {
            authService.logout();
            ViewNavigator.showLogin();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }

    // Helper utilities

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        String lower = s.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
