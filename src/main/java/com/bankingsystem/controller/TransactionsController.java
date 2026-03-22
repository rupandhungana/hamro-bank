package com.bankingsystem.controller;

import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionStatus;
import com.bankingsystem.model.User;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.BankingService;
import com.bankingsystem.util.SessionContext;
import com.bankingsystem.util.ViewNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionsController {
    @FXML
    private Label sidebarNameLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton allBtn, inBtn, outBtn;
    @FXML
    private Label resultsCountLabel;
    @FXML
    private Label showingLabel;
    @FXML
    private Label totalSumLabel;
    @FXML
    private Label errorLabel;

    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, Transaction> colDate;
    @FXML
    private TableColumn<Transaction, Transaction> colType;
    @FXML
    private TableColumn<Transaction, String> colSender;
    @FXML
    private TableColumn<Transaction, String> colReceiver;
    @FXML
    private TableColumn<Transaction, Transaction> colAmount;
    @FXML
    private TableColumn<Transaction, TransactionStatus> colStatus;

    private final BankingService bankingService = new BankingService();
    private final AuthService authService = new AuthService();
    private final ObservableList<Transaction> allTransactions = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredTransactions;

    @FXML
    private void initialize() {
        User user = SessionContext.getCurrentUser();
        if (user != null) {
            sidebarNameLabel.setText("Welcome, " + user.getFullName());
        }

        setupTable();
        loadTransactions();
        setupFilters();
    }

    private void setupTable() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        colDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getCreatedAt() == null) {
                    setText(null);
                } else {
                    setText(item.getCreatedAt().format(formatter));
                }
            }
        });

        colType.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(12);
                box.setAlignment(Pos.CENTER_LEFT);

                StackPane iconPane = new StackPane();
                iconPane.setPrefSize(28, 28);
                iconPane.setMinSize(28, 28);
                iconPane.setMaxSize(28, 28);

                boolean isOut = isOutgoing(item);
                Label symbol = new Label(isOut ? "-" : "+");
                symbol.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                if (isOut) {
                    iconPane.getStyleClass().add("tx-icon-out");
                    symbol.getStyleClass().add("tx-arrow-out");
                } else {
                    iconPane.getStyleClass().add("tx-icon-in");
                    symbol.getStyleClass().add("tx-arrow-in");
                }

                iconPane.getChildren().add(symbol);

                String type = item.getTransactionType().name().replace('_', ' ');
                String typeLabel = type.charAt(0) + type.substring(1).toLowerCase();
                Label label = new Label(typeLabel);
                label.getStyleClass().add("body-medium");

                box.getChildren().addAll(iconPane, label);
                setGraphic(box);
            }
        });

        colSender.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("senderAccount"));
        colSender.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null || item.isEmpty()) {
                    setText("ADMIN");
                } else {
                    setText(item);
                }
            }
        });
        colReceiver.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("receiverAccount"));

        colAmount.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getAmount() == null) {
                    setText(null);
                    getStyleClass().removeAll("tx-amount-in", "tx-amount-out");
                    return;
                }

                boolean isOut = isOutgoing(item);
                String prefix = isOut ? "- NPR " : "+ NPR ";
                setText(prefix + String.format("%,.2f", item.getAmount()));
                getStyleClass().removeAll("tx-amount-in", "tx-amount-out");
                getStyleClass().add(isOut ? "tx-amount-out" : "tx-amount-in");
            }
        });

        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(TransactionStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String text;
                String style;
                if (item == TransactionStatus.SUCCESS) {
                    text = "Completed";
                    style = "badge-success";
                } else if (item == TransactionStatus.PENDING) {
                    text = "Pending";
                    style = "badge-warning";
                } else {
                    text = "Failed";
                    style = "badge-error";
                }

                Label badge = new Label(text);
                badge.getStyleClass().addAll("badge", style);
                setGraphic(badge);
            }
        });
    }

    private void loadTransactions() {
        try {
            String accNum = SessionContext.getCurrentAccount().getAccountNumber();
            List<Transaction> txs = bankingService.getTransactionsForAccount(accNum);
            allTransactions.setAll(txs);

            filteredTransactions = new FilteredList<>(allTransactions, tx -> true);
            transactionTable.setItems(filteredTransactions);
            updateSummaries();
        } catch (Exception e) {
            errorLabel.setText("Load error: " + e.getMessage());
        }
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        ToggleGroup group = new ToggleGroup();
        allBtn.setToggleGroup(group);
        inBtn.setToggleGroup(group);
        outBtn.setToggleGroup(group);
        allBtn.setSelected(true);

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        boolean filterIn = inBtn.isSelected();
        boolean filterOut = outBtn.isSelected();
        String myAcc = SessionContext.getCurrentAccount().getAccountNumber();

        filteredTransactions.setPredicate(tx -> {
            boolean matchesSearch = search.isEmpty()
                    || (tx.getSenderAccount() != null && tx.getSenderAccount().toLowerCase().contains(search))
                    || (tx.getReceiverAccount() != null && tx.getReceiverAccount().toLowerCase().contains(search))
                    || tx.getTransactionType().name().toLowerCase().contains(search)
                    || (tx.getDescription() != null && tx.getDescription().toLowerCase().contains(search));

            if (!matchesSearch) {
                return false;
            }

            if (filterIn) {
                return !myAcc.equals(tx.getSenderAccount());
            }
            if (filterOut) {
                return myAcc.equals(tx.getSenderAccount());
            }
            return true;
        });

        updateSummaries();
    }

    private void updateSummaries() {
        int count = filteredTransactions.size();
        resultsCountLabel.setText(count + " results");
        showingLabel.setText("Showing " + count + " of " + allTransactions.size() + " transactions");

        String myAcc = SessionContext.getCurrentAccount().getAccountNumber();
        BigDecimal total = filteredTransactions.stream()
                .map(tx -> {
                    BigDecimal amount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
                    boolean outgoing = myAcc.equals(tx.getSenderAccount());
                    return outgoing ? amount.negate() : amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalSumLabel.setText("Total: NPR " + String.format("%.2f", total));
    }

    private boolean isOutgoing(Transaction tx) {
        String myAcc = SessionContext.getCurrentAccount().getAccountNumber();
        return tx.getSenderAccount() != null && tx.getSenderAccount().equals(myAcc);
    }

    @FXML
    private void onBack() {
        ViewNavigator.showDashboard();
    }

    @FXML
    private void onTransfer() {
        ViewNavigator.showTransfer();
    }

    @FXML
    private void onLogout() {
        try {
            authService.logout();
            ViewNavigator.showLogin();
        } catch (Exception e) {
            if (errorLabel != null) {
                errorLabel.setText("Logout error: " + e.getMessage());
            }
        }
    }
}
