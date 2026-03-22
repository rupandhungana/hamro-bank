package com.bankingsystem.service;

import com.bankingsystem.dao.*;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.InsufficientFundsException;
import com.bankingsystem.model.*;
import com.bankingsystem.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Core banking service — handles all financial operations.
 *
 * <p>
 * <b>OOP Principles applied:</b>
 * <ul>
 * <li><b>SRP</b> — this class has ONE responsibility: executing banking
 * transactions. Database access is delegated to repositories;
 * no UI code is present here.</li>
 * <li><b>DIP</b> — depends on {@link UserRepository} and
 * {@link AccountRepository}
 * interfaces, enabling the service to be tested in isolation
 * with mock repositories.</li>
 * <li><b>Encapsulation</b> — all repository fields are {@code private final};
 * low-level SQL for multi-step transfers is wrapped in a
 * private helper to keep public methods readable.</li>
 * <li><b>Abstraction</b> — callers invoke {@code transfer()} or
 * {@code adminDeposit()}
 * without needing to know about locking, rollback, or JDBC.</li>
 * <li><b>OCP</b> — new transaction types (e.g., inter-bank) can be added as new
 * methods without modifying the existing ones.</li>
 * </ul>
 */
public class BankingService {

    /** Dependencies declared against abstractions (DIP). */
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionDAO transactionDAO;

    /** Default constructor: wires production JDBC implementations. */
    public BankingService() {
        this.userRepository = new UserDAO();
        this.accountRepository = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    /**
     * Injection constructor for testing and future DI framework support.
     *
     * @param userRepository    user data source
     * @param accountRepository account data source
     * @param transactionDAO    transaction persistence
     */
    public BankingService(UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionDAO transactionDAO) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionDAO = transactionDAO;
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    /**
     * Transfers {@code amount} from one account to another atomically.
     *
     * <p>
     * Uses a database transaction with {@code FOR UPDATE} row-locking to prevent
     * race conditions when multiple transfers occur simultaneously.
     *
     * @param senderAccount   account number of the sender
     * @param receiverAccount account number of the receiver
     * @param amount          positive transfer amount
     * @param description     optional memo / reference text
     * @throws InsufficientFundsException if the sender's balance is too low
     * @throws AccountNotFoundException   if either account does not exist
     * @throws IllegalArgumentException   if amount is zero/negative or accounts are
     *                                    the same
     * @throws SQLException               if a DB error occurs (transaction is
     *                                    rolled back)
     */
    public void transfer(String senderAccount,
            String receiverAccount,
            BigDecimal amount,
            String description) throws SQLException {

        validateTransferInputs(senderAccount, receiverAccount, amount);

        String lockSql = "SELECT balance, status FROM accounts WHERE account_number = ? FOR UPDATE";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        String insertSql = "INSERT INTO transactions "
                + "(sender_account, receiver_account, amount, description, transaction_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock and read sender
                LockedAccount sender = lockAccount(conn, lockSql, senderAccount);
                if (sender.status() == AccountStatus.FROZEN)
                    throw new IllegalArgumentException("Sender account is frozen.");
                if (sender.balance().compareTo(amount) < 0)
                    throw new InsufficientFundsException(sender.balance(), amount);

                // Lock and read receiver
                LockedAccount receiver = lockAccount(conn, lockSql, receiverAccount);
                if (receiver.status() == AccountStatus.FROZEN)
                    throw new IllegalArgumentException("Receiver account is frozen.");

                // Debit sender, credit receiver
                updateBalance(conn, updateSql, sender.balance().subtract(amount), senderAccount);
                updateBalance(conn, updateSql, receiver.balance().add(amount), receiverAccount);

                // Record the transaction
                insertTransaction(conn, insertSql,
                        senderAccount, receiverAccount, amount, description,
                        TransactionType.TRANSFER, TransactionStatus.SUCCESS);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Admin Deposit

    /**
     * Credits {@code amount} into a user's account (admin-only operation).
     *
     * @param receiverAccount target account number
     * @param amount          positive deposit amount
     * @param description     admin note / reason
     * @throws AccountNotFoundException if the target account does not exist
     * @throws IllegalArgumentException if the amount is not positive or the account
     *                                  is frozen
     * @throws SQLException             if a DB error occurs
     */
    public void adminDeposit(String receiverAccount,
            BigDecimal amount,
            String description) throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");

        String lockSql = "SELECT balance, status FROM accounts WHERE account_number = ? FOR UPDATE";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        String insertSql = "INSERT INTO transactions "
                + "(sender_account, receiver_account, amount, description, transaction_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                LockedAccount receiver = lockAccount(conn, lockSql, receiverAccount);
                if (receiver.status() == AccountStatus.FROZEN)
                    throw new IllegalArgumentException("Target account is frozen - cannot deposit.");

                updateBalance(conn, updateSql, receiver.balance().add(amount), receiverAccount);

                insertTransaction(conn, insertSql,
                        null, receiverAccount, amount, description,
                        TransactionType.ADMIN_DEPOSIT, TransactionStatus.SUCCESS);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Queries

    /**
     * Returns all transactions for a specific account (sorted by most recent
     * first).
     */
    public List<Transaction> getTransactionsForAccount(String accountNumber) throws SQLException {
        return transactionDAO.findByAccount(accountNumber);
    }

    /** Returns every transaction in the system (admin view). */
    public List<Transaction> getAllTransactions() throws SQLException {
        return transactionDAO.findAll();
    }

    /** Returns all registered users. */
    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAll();
    }

    /** Finds the account belonging to a given user. */
    public java.util.Optional<Account> getAccountByUserId(int userId) throws SQLException {
        return accountRepository.findByUserId(userId);
    }

    /** Looks up an account by its account number. */
    public java.util.Optional<Account> getAccountByNumber(String accountNumber) throws SQLException {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    /** Updates the status of a user (e.g., ACTIVE → BANNED). */
    public void updateUserStatus(int userId, UserStatus status) throws SQLException {
        userRepository.updateStatus(userId, status);
    }

    /** Updates the status of an account (e.g., ACTIVE → FROZEN). */
    public void updateAccountStatus(String accountNumber, AccountStatus status) throws SQLException {
        accountRepository.updateStatus(accountNumber, status);
    }

    // Private helpers

    /**
     * A private record to carry the two values needed from a locked account row.
     */
    private record LockedAccount(BigDecimal balance, AccountStatus status) {
    }

    /**
     * Reads and locks a single account row within an existing transaction.
     *
     * @throws AccountNotFoundException if the account number is not in the database
     */
    private LockedAccount lockAccount(Connection conn,
            String lockSql,
            String accountNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new AccountNotFoundException(accountNumber);
                return new LockedAccount(
                        rs.getBigDecimal("balance"),
                        AccountStatus.valueOf(rs.getString("status")));
            }
        }
    }

    /** Updates an account's balance within an active DB transaction. */
    private void updateBalance(Connection conn,
            String updateSql,
            BigDecimal newBalance,
            String accountNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        }
    }

    /** Inserts a transaction record within an active DB transaction. */
    private void insertTransaction(Connection conn,
            String insertSql,
            String sender,
            String receiver,
            BigDecimal amount,
            String description,
            TransactionType type,
            TransactionStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            if (sender == null)
                ps.setNull(1, Types.VARCHAR);
            else
                ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setBigDecimal(3, amount);
            ps.setString(4, description);
            ps.setString(5, type.name());
            ps.setString(6, status.name());
            ps.executeUpdate();
        }
    }

    /** Validates inputs that are common to transfer operations. */
    private void validateTransferInputs(String senderAccount,
            String receiverAccount,
            BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        if (senderAccount == null || senderAccount.isBlank())
            throw new IllegalArgumentException("Sender account is required.");
        if (receiverAccount == null || receiverAccount.isBlank())
            throw new IllegalArgumentException("Receiver account is required.");
        if (senderAccount.equals(receiverAccount))
            throw new IllegalArgumentException("Sender and receiver accounts must be different.");
    }
}