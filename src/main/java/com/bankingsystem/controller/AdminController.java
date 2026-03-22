package com.bankingsystem.controller;

import com.bankingsystem.model.*;
import com.bankingsystem.service.BankingService;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.util.SessionContext;
import com.bankingsystem.util.ValidationUtil;
import com.bankingsystem.util.ViewNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    // Navigation Controls
    @FXML
    private Button navDashboard;
    @FXML
    private Button navUsers;
    @FXML
    private Button navDeposits;
    @FXML
    private Button navTx;

    // Page Layout Components
    @FXML
    private ScrollPane pageDashboard;
    @FXML
    private VBox pageUsers;
    @FXML
    private ScrollPane pageDeposits;
    @FXML
    private VBox pageTransactions;

    // Dashboard Statistics and Charts
    @FXML
    private Label dateLabel;
    @FXML
    private Label statTotalUsers;
    @FXML
    private Label statTotalTx;
    @FXML
    private Label statTotalDeposited;
    @FXML
    private Label statActiveUsers;
    @FXML
    private Label statBannedUsers;
    @FXML
    private Label recentTxCount;
    @FXML
    private ListView<Transaction> recentTxList;
    @FXML
    private ListView<User> userStatusList;
    @FXML
    private Label activeBarCount;
    @FXML
    private Label bannedBarCount;
    @FXML
    private Rectangle activeBar;
    @FXML
    private Rectangle bannedBar;

    // Users Table and Filters
    @FXML
    private TableView<UserRow> userTable;
    @FXML
    private TableColumn<UserRow, UserRow> colName;
    @FXML
    private TableColumn<UserRow, String> colAccount;
    @FXML
    private TableColumn<UserRow, UserRow> colBalance;
    @FXML
    private TableColumn<UserRow, String> colStatus;
    @FXML
    private TableColumn<UserRow, String> colAcctStat;
    @FXML
    private TableColumn<UserRow, UserRow> colActions;
    @FXML
    private TextField userSearchField;
    @FXML
    private Label userCountLabel;
    @FXML
    private Label usersErrorLabel;
    @FXML
    private Button filterAll;
    @FXML
    private Button filterActive;
    @FXML
    private Button filterBanned;
    @FXML
    private Button filterFrozen;

    // Deposits Processing View
    @FXML
    private TextField depositAccountField;
    @FXML
    private TextField depositAmountField;
    @FXML
    private TextField depositNoteField;
    @FXML
    private Label depositErrorLabel;
    @FXML
    private Label depositSuccessLabel;
    @FXML
    private ListView<UserAccount> accountLookupList;

    // System-wide Transaction Logs
    @FXML
    private TableView<TransactionRow> txTable;
    @FXML
    private TableColumn<TransactionRow, String> colTxDate;
    @FXML
    private TableColumn<TransactionRow, TransactionRow> colTxType;
    @FXML
    private TableColumn<TransactionRow, String> colTxSender;
    @FXML
    private TableColumn<TransactionRow, String> colTxReceiver;
    @FXML
    private TableColumn<TransactionRow, String> colTxAmount;
    @FXML
    private TableColumn<TransactionRow, TransactionRow> colTxStatus;
    @FXML
    private TextField txSearchField;
    @FXML
    private Label txCountLabel;
    @FXML
    private Button txFilterAll;
    @FXML
    private Button txFilterIncoming;
    @FXML
    private Button txFilterOutgoing;

    // Shared Modal Components
    @FXML private StackPane modalOverlay;
    @FXML private VBox modalContainer;
    @FXML private StackPane modalIconContainer;
    @FXML private Label modalIcon;
    @FXML private Label modalTitle;
    @FXML private Label modalBody;
    @FXML private Button modalCancelBtn;
    @FXML private Button modalConfirmBtn;
    @FXML private Button modalCloseBtn;

    // Service Dependencies
    // Dependencies against service abstractions (DIP)
    private final BankingService bankingService = new BankingService();
    private final AuthService authService = new AuthService();

    // Data Transfer Objects and Transformers
    public static class TransactionRow {
        public final Transaction tx;
        public final String date;
        public final String type;
        public final String sender;
        public final String receiver;
        public final String amountText;
        public final boolean isIncoming;

        public TransactionRow(Transaction t, String senderName, String receiverName) {
            this.tx = t;
            this.date = t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    : "";
            this.type = (t.getTransactionType() != null)
                    ? capitalize(t.getTransactionType().name().replace('_', ' '))
                    : "Unknown";

            // Format: Name (AccountNo) or just AccountNo
            if (t.getSenderAccount() == null || t.getSenderAccount().isEmpty()) {
                this.sender = (t.getTransactionType() == TransactionType.ADMIN_DEPOSIT) ? "ADMIN" : "-";
            } else {
                this.sender = (senderName != null && !senderName.isEmpty()) ? senderName + " (" + t.getSenderAccount() + ")"
                        : t.getSenderAccount();
            }

            if (t.getReceiverAccount() == null || t.getReceiverAccount().isEmpty()) {
                this.receiver = "-";
            } else {
                this.receiver = (receiverName != null && !receiverName.isEmpty())
                        ? receiverName + " (" + t.getReceiverAccount() + ")"
                        : t.getReceiverAccount();
            }

            this.isIncoming = t.getTransactionType() == TransactionType.ADMIN_DEPOSIT
                    || t.getTransactionType() == TransactionType.DEPOSIT;
            this.amountText = (isIncoming ? "+" : "-") + " NPR " + String.format("%,.2f", t.getAmount());
        }

        public String getDate() { return date; }
        public String getType() { return type; }
        public String getSender() { return sender; }
        public String getReceiver() { return receiver; }
        public String getAmountText() { return amountText; }
        public Transaction getTx() { return tx; }
        public boolean isIncoming() { return isIncoming; }

        private String capitalize(String s) {
            if (s == null || s.isEmpty())
                return s;
            String l = s.toLowerCase();
            return Character.toUpperCase(l.charAt(0)) + l.substring(1);
        }
    }

    private ObservableList<UserRow> allUserRows = FXCollections.observableArrayList();
    private ObservableList<TransactionRow> allTxRows = FXCollections.observableArrayList();
    private String currentUserFilter = "ALL";
    private String currentTxFilter = "ALL";

    // ══════════════════════════════════════════════════════════════════
    // Inner record helpers
    // ══════════════════════════════════════════════════════════════════
    public static class UserRow {
        public final User user;
        public final Account account;

        UserRow(User u, Account a) {
            user = u;
            account = a;
        }
    }

    public static class UserAccount {
        public final User user;
        public final Account account;

        UserAccount(User u, Account a) {
            user = u;
            account = a;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // initialize
    // ══════════════════════════════════════════════════════════════════
    @FXML
    private void initialize() {
        try {
            // 1. Setup UI components (factories, formatters)
            dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
            setupUserTable();
            setupTxTable();
            setupRecentTxList();
            setupUserStatusList();
            setupAccountLookupList();
            setupDepositFormatters();

            // 2. Load data
            loadAll();
        } catch (Exception e) {
            System.err.println("AdminController initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ══════════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════════
    private static final String ACTIVE_STYLE = "nav-btn nav-btn-active";
    private static final String INACTIVE_STYLE = "nav-btn";

    @FXML
    private void onNavDashboard() {
        switchPage(0);
    }

    @FXML
    private void onNavUsers() {
        switchPage(1);
    }

    @FXML
    private void onNavDeposits() {
        switchPage(2);
    }

    @FXML
    private void onNavTransactions() {
        switchPage(3);
    }

    private void switchPage(int idx) {
        pageDashboard.setVisible(idx == 0);
        pageDashboard.setManaged(idx == 0);
        pageUsers.setVisible(idx == 1);
        pageUsers.setManaged(idx == 1);
        pageDeposits.setVisible(idx == 2);
        pageDeposits.setManaged(idx == 2);
        pageTransactions.setVisible(idx == 3);
        pageTransactions.setManaged(idx == 3);

        setNavActive(navDashboard, idx == 0);
        setNavActive(navUsers, idx == 1);
        setNavActive(navDeposits, idx == 2);
        setNavActive(navTx, idx == 3);
    }

    private void setNavActive(Button btn, boolean active) {
        if (btn != null)
            btn.getStyleClass().setAll(active ? ACTIVE_STYLE.split(" ") : INACTIVE_STYLE.split(" "));
    }

    @FXML
    private void onRefresh() {
        System.out.println("[Admin] manual refresh triggered");
        loadAll();
    }

    // ══════════════════════════════════════════════════════════════════
    // Data Loading
    // ══════════════════════════════════════════════════════════════════
    private void loadAll() {
        try {
            List<User> usersList = bankingService.getAllUsers();
            List<Transaction> txsList = bankingService.getAllTransactions();

            // Build UserRow list (User + their Account)
            allUserRows.clear();
            for (User u : usersList) {
                Account acc = bankingService.getAccountByUserId(u.getUserId()).orElse(null);
                allUserRows.add(new UserRow(u, acc));
            }

            // Create a lookup map for AccountNumber -> User Name
            java.util.Map<String, String> accNameMap = new java.util.HashMap<>();
            for (UserRow ur : allUserRows) {
                if (ur.account != null) {
                    accNameMap.put(ur.account.getAccountNumber(), ur.user.getFullName());
                }
            }

            // Build TransactionRows
            allTxRows.clear();
            for (Transaction t : txsList) {
                String sName = accNameMap.get(t.getSenderAccount());
                String rName = accNameMap.get(t.getReceiverAccount());
                allTxRows.add(new TransactionRow(t, sName, rName));
            }

            // Stat cards
            int total = usersList.size();
            int active = (int) usersList.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
            int banned = (int) usersList.stream().filter(u -> u.getStatus() == UserStatus.BANNED).count();
            BigDecimal deposited = txsList.stream()
                    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                    .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (statTotalUsers != null)
                statTotalUsers.setText(String.valueOf(total));
            if (statTotalTx != null)
                statTotalTx.setText(String.valueOf(txsList.size()));
            if (statTotalDeposited != null)
                statTotalDeposited.setText("NPR " + String.format("%,.2f", deposited));
            if (statActiveUsers != null)
                statActiveUsers.setText(String.valueOf(active));
            if (statBannedUsers != null)
                statBannedUsers.setText(String.valueOf(banned));

            // Status bars
            if (activeBarCount != null)
                activeBarCount.setText(String.valueOf(active));
            if (bannedBarCount != null)
                bannedBarCount.setText(String.valueOf(banned));
            double maxW = 200.0;
            if (activeBar != null)
                activeBar.setWidth(total == 0 ? 0 : maxW * active / total);
            if (bannedBar != null)
                bannedBar.setWidth(total == 0 ? 0 : maxW * banned / total);

            // Recent 15 transactions
            List<Transaction> recent = txsList.stream().limit(15).collect(Collectors.toList());
            recentTxList.setItems(FXCollections.observableArrayList(recent));
            if (recentTxCount != null)
                recentTxCount.setText(txsList.size() + " total");

            // User status list (first 5 - ensure it shows new users)
            userStatusList.setItems(
                    FXCollections.observableArrayList(usersList.stream().limit(5).collect(Collectors.toList())));

            // Account lookup (for cash deposits)
            List<UserAccount> uas = allUserRows.stream()
                    .filter(r -> r.account != null)
                    .map(r -> new UserAccount(r.user, r.account))
                    .collect(Collectors.toList());
            accountLookupList.setItems(FXCollections.observableArrayList(uas));

            // Refresh tables
            applyUserFilter();
            applyTxFilter();

            System.out.println("[Admin] loadAll completed successfully");
        } catch (Exception e) {
            System.err.println("Admin loadAll error: " + e.toString());
            // Show first error in a global toast or error label if not hidden
            if (usersErrorLabel != null) {
                usersErrorLabel.setText("Database error: " + e.toString());
                usersErrorLabel.setVisible(true);
            }
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Recent Transactions ListView cell
    // ══════════════════════════════════════════════════════════════════
    private void setupRecentTxList() {
        recentTxList.setCellFactory(lv -> new ListCell<Transaction>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                TransactionType type = t.getTransactionType();
                boolean isIn = type == TransactionType.ADMIN_DEPOSIT || type == TransactionType.DEPOSIT;
                String color = isIn ? "#16A34A" : "#DC2626";
                String sign = isIn ? "+" : "-";
                String amtStr = sign + "NPR "
                        + String.format("%,.2f", t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO);

                // Icon circle
                StackPane icon = new StackPane();
                icon.setStyle((isIn ? "-fx-background-color:#DCFCE7;" : "-fx-background-color:#FEE2E2;") +
                        "-fx-background-radius:18;-fx-min-width:32;-fx-min-height:32;-fx-max-width:32;-fx-max-height:32;");
                Label iconLbl = new Label(isIn ? "+" : "-");
                iconLbl.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:700;-fx-font-size:14px;");
                icon.getChildren().add(iconLbl);

                // Description
                String sender = t.getSenderAccount() != null ? t.getSenderAccount() 
                               : (t.getTransactionType() == TransactionType.ADMIN_DEPOSIT ? "ADMIN" : "Bank");
                String receiver = t.getReceiverAccount() != null ? t.getReceiverAccount() : "—";
                VBox desc = new VBox(2);
                Label title = new Label(sender + " → " + receiver);
                title.setStyle("-fx-font-size:13px;-fx-font-weight:500;-fx-text-fill:#111827;");
                Label date = new Label(t.getCreatedAt() != null
                        ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        : "");
                date.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
                desc.getChildren().addAll(title, date);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label amt = new Label(amtStr);
                amt.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:600;-fx-font-size:13px;");

                HBox row = new HBox(10, icon, desc, spacer, amt);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 4, 8, 4));
                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // User Status ListView cell (Dashboard sidebar)
    // ══════════════════════════════════════════════════════════════════
    private void setupUserStatusList() {
        userStatusList.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // Avatar circle
                StackPane avatar = new StackPane();
                String ch = u.getFullName() != null && !u.getFullName().isEmpty()
                        ? String.valueOf(u.getFullName().charAt(0)).toUpperCase()
                        : "?";
                avatar.setStyle("-fx-background-color:#1E3A8A;-fx-background-radius:16;" +
                        "-fx-min-width:32;-fx-min-height:32;-fx-max-width:32;-fx-max-height:32;");
                Label init = new Label(ch);
                init.setStyle("-fx-text-fill:#FFFFFF;-fx-font-weight:700;-fx-font-size:13px;");
                avatar.getChildren().add(init);

                VBox info = new VBox(1);
                Label name = new Label(u.getFullName() != null ? u.getFullName() : "");
                name.setStyle("-fx-font-size:13px;-fx-font-weight:500;-fx-text-fill:#111827;");
                Label handle = new Label("@" + (u.getEmail() != null ? u.getEmail().split("@")[0] : ""));
                handle.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
                info.getChildren().addAll(name, handle);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                boolean isBanned = u.getStatus() == UserStatus.BANNED;
                Label badge = new Label(isBanned ? "Banned" : "Active");
                badge.setStyle((isBanned
                        ? "-fx-text-fill:#DC2626;-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;"
                        : "-fx-text-fill:#16A34A;-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;")
                        + "-fx-background-radius:6;-fx-border-radius:6;-fx-border-width:1;-fx-padding:2 8;-fx-font-size:11px;-fx-font-weight:500;");

                HBox row = new HBox(10, avatar, info, sp, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 4, 6, 4));
                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // Users Table Setup
    // ══════════════════════════════════════════════════════════════════
    private void setupUserTable() {
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colName.setCellFactory(col -> new TableCell<UserRow, UserRow>() {
            @Override
            protected void updateItem(UserRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null || row.user == null) {
                    setGraphic(null);
                    return;
                }

                String name = row.user.getFullName() != null ? row.user.getFullName() : "Unknown";
                String ch = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();

                StackPane avatar = new StackPane();
                avatar.setStyle("-fx-background-color:#1E3A8A;-fx-background-radius:16;" +
                        "-fx-min-width:32;-fx-min-height:32;-fx-max-width:32;-fx-max-height:32;");
                Label init = new Label(ch);
                init.setStyle("-fx-text-fill:#FFFFFF;-fx-font-weight:700;-fx-font-size:13px;");
                avatar.getChildren().add(init);

                VBox info = new VBox(1);
                Label nameL = new Label(name);
                nameL.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#111827;");

                String email = row.user.getEmail() != null ? "@" + row.user.getEmail().split("@")[0] : "";
                Label emailL = new Label(email);
                emailL.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");

                info.getChildren().addAll(nameL, emailL);
                HBox hb = new HBox(10, avatar, info);
                hb.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hb);
                setText(null);
            }
        });

        colAccount.setCellValueFactory(c -> {
            Account a = c.getValue().account;
            if (a == null)
                return new SimpleStringProperty("—");
            String num = a.getAccountNumber();
            if (num != null && num.length() >= 16)
                return new SimpleStringProperty(num.substring(0, 4) + " " + num.substring(4, 8) + " "
                        + num.substring(8, 12) + " " + num.substring(12, 16));
            return new SimpleStringProperty(num != null ? num : "—");
        });

        colBalance.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colBalance.setCellFactory(col -> new TableCell<UserRow, UserRow>() {
            @Override
            protected void updateItem(UserRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null || row.account == null) {
                    setText("—");
                    setStyle("");
                    return;
                }
                BigDecimal bal = row.account.getBalance();
                setText("NPR " + String.format("%,.2f", bal != null ? bal : BigDecimal.ZERO));
                setStyle("-fx-font-weight:600;-fx-text-fill:#111827;");
            }
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().user.getStatus() != null ? c.getValue().user.getStatus().name() : ""));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    return;
                }
                Label lbl = new Label(s);
                boolean banned = "BANNED".equals(s);
                lbl.setStyle((banned
                        ? "-fx-text-fill:#DC2626;-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;"
                        : "-fx-text-fill:#16A34A;-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;")
                        + "-fx-background-radius:6;-fx-border-radius:6;-fx-border-width:1;-fx-padding:2 8;-fx-font-size:11px;-fx-font-weight:500;");
                setGraphic(lbl);
                setText(null);
            }
        });

        colAcctStat.setCellValueFactory(c -> {
            Account a = c.getValue().account;
            return new SimpleStringProperty(a != null && a.getStatus() != null ? a.getStatus().name() : "—");
        });
        colAcctStat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    return;
                }
                Label lbl = new Label(s);
                String style;
                if ("FROZEN".equals(s))
                    style = "-fx-text-fill:#D97706;-fx-background-color:#FFFBEB;-fx-border-color:#FDE68A;";
                else if ("ACTIVE".equals(s))
                    style = "-fx-text-fill:#28a745;-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;";
                else
                    style = "-fx-text-fill:#6B7280;-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;";
                lbl.setStyle(style
                        + "-fx-background-radius:6;-fx-border-radius:6;-fx-border-width:1;-fx-padding:2 8;-fx-font-size:11px;-fx-font-weight:500;");
                setGraphic(lbl);
                setText(null);
            }
        });

        colActions.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<UserRow, UserRow>() {
            final Button banBtn = new Button();
            final Button freezeBtn = new Button();

            @Override
            protected void updateItem(UserRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }

                boolean isBanned = row.user.getStatus() == UserStatus.BANNED;
                boolean isFrozen = row.account != null && row.account.getStatus() == AccountStatus.FROZEN;

                banBtn.setText(isBanned ? "Unban" : "Ban");
                banBtn.setStyle(isBanned
                        ? "-fx-background-color:transparent;-fx-border-color:#28a745;-fx-border-width:1;-fx-text-fill:#28a745;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-border-radius:6;-fx-padding:3 10;-fx-cursor:hand;"
                        : "-fx-background-color:transparent;-fx-border-color:#dc3545;-fx-border-width:1;-fx-text-fill:#dc3545;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-border-radius:6;-fx-padding:3 10;-fx-cursor:hand;");

                banBtn.setOnAction(e -> {
                    UserStatus newStatus = isBanned ? UserStatus.ACTIVE : UserStatus.BANNED;
                    try {
                        bankingService.updateUserStatus(row.user.getUserId(), newStatus);
                        loadAll();
                    } catch (Exception ex) {
                        if (usersErrorLabel != null) {
                            usersErrorLabel.setText("Update failed: " + ex.toString());
                            usersErrorLabel.setVisible(true);
                        }
                    }
                });

                freezeBtn.setText(isFrozen ? "Unfreeze" : "Freeze");
                freezeBtn.setStyle(isFrozen
                        ? "-fx-background-color:transparent;-fx-border-color:#BBF7D0;-fx-border-width:1;-fx-text-fill:#16A34A;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-border-radius:6;-fx-padding:3 10;-fx-cursor:hand;"
                        : "-fx-background-color:transparent;-fx-border-color:#FDE68A;-fx-border-width:1;-fx-text-fill:#D97706;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-border-radius:6;-fx-padding:3 10;-fx-cursor:hand;");

                freezeBtn.setOnAction(e -> {
                    if (row.account == null)
                        return;
                    AccountStatus newStatus = isFrozen ? AccountStatus.ACTIVE : AccountStatus.FROZEN;
                    try {
                        bankingService.updateAccountStatus(row.account.getAccountNumber(), newStatus);
                        loadAll();
                    } catch (Exception ex) {
                        if (usersErrorLabel != null) {
                            usersErrorLabel.setText(ex.getMessage());
                            usersErrorLabel.setVisible(true);
                        }
                    }
                });

                HBox box = new HBox(8, banBtn, freezeBtn);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // Transactions Table Setup
    // ══════════════════════════════════════════════════════════════════
    private void setupTxTable() {
        colTxDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTxType.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        colTxType.setCellFactory(tc -> new TableCell<TransactionRow, TransactionRow>() {
            @Override
            protected void updateItem(TransactionRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setGraphic(null);
                    return;
                }
                boolean inc = r.isIncoming;

                StackPane icon = new StackPane();
                icon.setStyle((inc ? "-fx-background-color:#DCFCE7;" : "-fx-background-color:#FEE2E2;") +
                        "-fx-background-radius:12;-fx-min-width:24;-fx-min-height:24;-fx-max-width:24;-fx-max-height:24;");

                Label ic = new Label(inc ? "+" : "-");
                ic.setStyle(
                        "-fx-text-fill:" + (inc ? "#28a745" : "#dc3545") + ";-fx-font-weight:800; -fx-font-size:12px;");
                icon.getChildren().add(ic);

                Label lbl = new Label(r.type);
                lbl.setStyle("-fx-text-fill:#374151; -fx-font-size:13px;");

                HBox hb = new HBox(10, icon, lbl);
                hb.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hb);
                setText(null);
            }
        });
        colTxSender.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colTxReceiver.setCellValueFactory(new PropertyValueFactory<>("receiver"));
        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amountText"));
        colTxStatus
                .setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));

        colTxAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String amt, boolean empty) {
                super.updateItem(amt, empty);
                if (empty || amt == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(amt);
                boolean inc = amt.startsWith("+");
                setStyle("-fx-text-fill: " + (inc ? "#28a745" : "#dc3545")
                        + "; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
        });

        colTxStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(TransactionRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setGraphic(null);
                    return;
                }
                String s = r.tx.getStatus().name();
                Label lbl = new Label(s.charAt(0) + s.substring(1).toLowerCase());
                String style;
                switch (s) {
                    case "SUCCESS" ->
                        style = "-fx-text-fill:#16A34A;-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;";
                    case "PENDING" ->
                        style = "-fx-text-fill:#D97706;-fx-background-color:#FFFBEB;-fx-border-color:#FDE68A;";
                    case "FAILED" ->
                        style = "-fx-text-fill:#DC2626;-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;";
                    default -> style = "-fx-text-fill:#6B7280;-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;";
                }
                lbl.setStyle(style
                        + "-fx-background-radius:6;-fx-border-radius:6;-fx-border-width:1;-fx-padding:2 10;-fx-font-size:11px;-fx-font-weight:600;");
                setGraphic(lbl);
            }
        });

        txTable.setPlaceholder(new Label("No transactions found"));
    }

    // ══════════════════════════════════════════════════════════════════
    // Account Lookup List (Deposits page)
    // ══════════════════════════════════════════════════════════════════
    private void setupAccountLookupList() {
        accountLookupList.setCellFactory(lv -> new ListCell<UserAccount>() {
            @Override
            protected void updateItem(UserAccount ua, boolean empty) {
                super.updateItem(ua, empty);
                if (empty || ua == null) {
                    setGraphic(null);
                    return;
                }
                String ch = ua.user.getFullName() != null && !ua.user.getFullName().isEmpty()
                        ? String.valueOf(ua.user.getFullName().charAt(0)).toUpperCase()
                        : "?";
                StackPane avatar = new StackPane();
                avatar.setStyle("-fx-background-color:#1E3A8A;-fx-background-radius:16;" +
                        "-fx-min-width:32;-fx-min-height:32;-fx-max-width:32;-fx-max-height:32;");
                Label init = new Label(ch);
                init.setStyle("-fx-text-fill:#FFFFFF;-fx-font-weight:700;-fx-font-size:13px;");
                avatar.getChildren().add(init);

                VBox info = new VBox(1);
                Label name = new Label(ua.user.getFullName() != null ? ua.user.getFullName() : "");
                name.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#111827;");
                String num = ua.account.getAccountNumber();
                if (num != null && num.length() >= 16)
                    num = num.substring(0, 4) + " " + num.substring(4, 8) + " " + num.substring(8, 12) + " "
                            + num.substring(12, 16);
                Label acct = new Label(num != null ? num : "");
                acct.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
                info.getChildren().addAll(name, acct);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                BigDecimal bal = ua.account.getBalance();
                boolean lowBal = bal != null && bal.compareTo(new BigDecimal("100")) < 0;
                Label balLbl = new Label("NPR " + String.format("%,.2f", bal != null ? bal : BigDecimal.ZERO));
                balLbl.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:"
                        + (lowBal ? "#D97706" : "#28a745") + ";");

                HBox row = new HBox(10, avatar, info, sp, balLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 4, 8, 4));
                row.setStyle("-fx-cursor:hand;");

                String finalNum = ua.account.getAccountNumber();
                row.setOnMouseClicked(e -> {
                    depositAccountField.setText(finalNum != null ? finalNum : "");
                    depositErrorLabel.setText("");
                    depositSuccessLabel.setText("");
                });

                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // User Filters (Users page)
    // ══════════════════════════════════════════════════════════════════
    @FXML
    private void onFilterAll() {
        currentUserFilter = "ALL";
        applyUserFilter();
        updateFilterButtons(filterAll, filterActive, filterBanned, filterFrozen);
    }

    @FXML
    private void onFilterActive() {
        currentUserFilter = "ACTIVE";
        applyUserFilter();
        updateFilterButtons(filterActive, filterAll, filterBanned, filterFrozen);
    }

    @FXML
    private void onFilterBanned() {
        currentUserFilter = "BANNED";
        applyUserFilter();
        updateFilterButtons(filterBanned, filterAll, filterActive, filterFrozen);
    }

    @FXML
    private void onFilterFrozen() {
        currentUserFilter = "FROZEN";
        applyUserFilter();
        updateFilterButtons(filterFrozen, filterAll, filterActive, filterBanned);
    }

    private void updateFilterButtons(Button active, Button... others) {
        active.setStyle(
                "-fx-background-color:#FFFFFF;-fx-text-fill:#111827;-fx-font-size:13px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:5 14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),4,0.3,0,1);");
        for (Button o : others)
            o.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#6B7280;-fx-font-size:13px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:5 14;-fx-cursor:hand;");
    }

    private void applyUserFilter() {
        String query = userSearchField != null ? userSearchField.getText().toLowerCase().trim() : "";
        List<UserRow> filtered = allUserRows.stream().filter(r -> {
            boolean matchFilter = switch (currentUserFilter) {
                case "ACTIVE" -> r.user.getStatus() == UserStatus.ACTIVE;
                case "BANNED" -> r.user.getStatus() == UserStatus.BANNED;
                case "FROZEN" -> r.account != null && r.account.getStatus() == AccountStatus.FROZEN;
                default -> true;
            };
            boolean matchSearch = query.isEmpty() ||
                    (r.user.getFullName() != null && r.user.getFullName().toLowerCase().contains(query)) ||
                    (r.user.getEmail() != null && r.user.getEmail().toLowerCase().contains(query)) ||
                    (r.account != null && r.account.getAccountNumber() != null
                            && r.account.getAccountNumber().contains(query));
            return matchFilter && matchSearch;
        }).collect(Collectors.toList());

        userTable.setItems(FXCollections.observableArrayList(filtered));
        if (userCountLabel != null)
            userCountLabel.setText(filtered.size() + " users");
    }

    @FXML
    private void onSearchUsers() {
        applyUserFilter();
    }

    // ══════════════════════════════════════════════════════════════════
    // Transaction Filters
    // ══════════════════════════════════════════════════════════════════
    @FXML
    private void onTxFilterAll() {
        currentTxFilter = "ALL";
        applyTxFilter();
        updateFilterButtons(txFilterAll, txFilterIncoming, txFilterOutgoing);
    }

    @FXML
    private void onTxFilterIncoming() {
        currentTxFilter = "INCOMING";
        applyTxFilter();
        updateFilterButtons(txFilterIncoming, txFilterAll, txFilterOutgoing);
    }

    @FXML
    private void onTxFilterOutgoing() {
        currentTxFilter = "OUTGOING";
        applyTxFilter();
        updateFilterButtons(txFilterOutgoing, txFilterAll, txFilterIncoming);
    }

    private void applyTxFilter() {
        String query = txSearchField != null ? txSearchField.getText().toLowerCase().trim() : "";
        List<TransactionRow> filtered = allTxRows.stream().filter(r -> {
            boolean matchFilter = switch (currentTxFilter) {
                case "INCOMING" -> r.tx.getTransactionType() == TransactionType.DEPOSIT
                        || r.tx.getTransactionType() == TransactionType.ADMIN_DEPOSIT;
                case "OUTGOING" -> r.tx.getTransactionType() == TransactionType.TRANSFER;
                default -> true;
            };
            boolean matchSearch = query.isEmpty() ||
                    r.sender.toLowerCase().contains(query) ||
                    r.receiver.toLowerCase().contains(query) ||
                    r.type.toLowerCase().contains(query);
            return matchFilter && matchSearch;
        }).collect(Collectors.toList());

        txTable.setItems(FXCollections.observableArrayList(filtered));
        if (txCountLabel != null)
            txCountLabel.setText(filtered.size() + " records");
    }

    @FXML
    private void onSearchTransactions() {
        applyTxFilter();
    }

    // ══════════════════════════════════════════════════════════════════
    // Deposits Page
    // ══════════════════════════════════════════════════════════════════
    private void setupDepositFormatters() {
        depositAmountField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.isEmpty())
                return change;
            return text.matches("\\d*(\\.\\d{0,2})?") ? change : null;
        }));
    }

    @FXML
    private void onDeposit() {
        String account = depositAccountField.getText().trim();
        String amountText = depositAmountField.getText().trim();
        if (account.isEmpty() || amountText.isEmpty()) {
            showDepositError("Account number and amount are required.");
            return;
        }
        
        // Show confirmation modal
        modalTitle.setText("Confirm Deposit");
        modalBody.setText("Are you sure you want to deposit NPR " + amountText + " to account " + account + "?");
        modalIcon.setText("!");
        modalIconContainer.getStyleClass().removeAll("modal-icon-success");
        modalIconContainer.getStyleClass().add("modal-icon-wait");
        
        modalCancelBtn.setVisible(true);
        modalCancelBtn.setManaged(true);
        modalConfirmBtn.setVisible(true);
        modalConfirmBtn.setManaged(true);
        modalCloseBtn.setVisible(false);
        modalCloseBtn.setManaged(false);
        
        modalConfirmBtn.setOnAction(e -> performDeposit(account, amountText, depositNoteField.getText().trim()));
        modalOverlay.setVisible(true);
    }

    private void performDeposit(String account, String amountText, String note) {
        modalConfirmBtn.setDisable(true);
        modalCancelBtn.setDisable(true);
        modalTitle.setText("Processing...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                BigDecimal amount = new BigDecimal(amountText);
                if (!ValidationUtil.isValidAmount(amount)) {
                    throw new IllegalArgumentException("Amount must be greater than zero.");
                }
                String adminName = SessionContext.getCurrentUser() != null ? SessionContext.getCurrentUser().getFullName() : "Admin";
                String description = note.isEmpty() ? "Admin Cash Deposit (" + adminName + ")" : note;
                bankingService.adminDeposit(account, amount, description);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            modalConfirmBtn.setDisable(false);
            modalCancelBtn.setDisable(false);
            loadAll();
            
            // Show success in modal
            modalTitle.setText("Deposit Successful");
            modalBody.setText("NPR " + amountText + " has been successfully credited to " + account + ".");
            modalIcon.setText("✓");
            modalIconContainer.getStyleClass().add("modal-icon-success");
            
            modalCancelBtn.setVisible(false);
            modalCancelBtn.setManaged(false);
            modalConfirmBtn.setVisible(false);
            modalConfirmBtn.setManaged(false);
            modalCloseBtn.setVisible(true);
            modalCloseBtn.setManaged(true);
            
            depositAccountField.clear();
            depositAmountField.clear();
            depositNoteField.clear();
        });

        task.setOnFailed(e -> {
            modalConfirmBtn.setDisable(false);
            modalCancelBtn.setDisable(false);
            modalOverlay.setVisible(false); // Hide modal to show error label on main page
            Throwable ex = task.getException();
            showDepositError(ex != null ? ex.toString() : "Unknown process error.");
        });

        new Thread(task).start();
    }

    @FXML
    private void onCloseModal() {
        modalOverlay.setVisible(false);
    }

    private void showDepositError(String message) {
        if (depositErrorLabel != null) {
            depositErrorLabel.setText(message);
            depositErrorLabel.setVisible(true);
            depositErrorLabel.setManaged(true);
        }
    }

    @FXML
    private void onClearDeposit() {
        depositAccountField.clear();
        depositAmountField.clear();
        depositNoteField.clear();
        depositErrorLabel.setText("");
        depositSuccessLabel.setText("");
    }

    // ══════════════════════════════════════════════════════════════════
    // Logout
    // ══════════════════════════════════════════════════════════════════
    @FXML
    private void onLogout() {
        try {
            authService.logout();
            ViewNavigator.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
