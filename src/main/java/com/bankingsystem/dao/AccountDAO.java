package com.bankingsystem.dao;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link AccountRepository}.
 *
 * <p><b>OOP Principles applied:</b>
 * <ul>
 *   <li><b>SRP</b>           — only responsible for SQL operations on the {@code accounts} table</li>
 *   <li><b>Encapsulation</b> — SQL strings and the {@code map()} helper are {@code private}</li>
 *   <li><b>DIP</b>           — implements {@link AccountRepository} so the service layer is
 *                              decoupled from this concrete class</li>
 *   <li><b>LSP</b>           — correctly fulfils every contract from the interface</li>
 * </ul>
 */
public class AccountDAO implements AccountRepository {

    @Override
    public int create(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, account.getUserId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getAccountType().name());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public Optional<Account> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) accounts.add(map(rs));
        }
        return accounts;
    }

    @Override
    public void updateStatus(String accountNumber, AccountStatus status) throws SQLException {
        String sql = "UPDATE accounts SET status = ? WHERE account_number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to an {@link Account} domain object.
     * Kept {@code private} to enforce encapsulation — callers always receive
     * clean model objects, never raw database cursors.
     */
    private Account map(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setAccountId    (rs.getInt("account_id"));
        account.setUserId       (rs.getInt("user_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setAccountType  (AccountType.valueOf(rs.getString("account_type")));
        account.setBalance      (rs.getBigDecimal("balance"));
        account.setStatus       (AccountStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) account.setCreatedAt(ts.toLocalDateTime());
        return account;
    }
}