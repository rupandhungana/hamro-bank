package com.bankingsystem.dao;

import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionStatus;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public int create(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (sender_account, receiver_account, amount, description, transaction_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tx.getSenderAccount());
            ps.setString(2, tx.getReceiverAccount());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getDescription());
            ps.setString(5, tx.getTransactionType().name());
            ps.setString(6, tx.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Transaction> findByAccount(String accountNumber) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender_account = ? OR receiver_account = ? "
                + "ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setTransactionId(rs.getInt("transaction_id"));
        tx.setSenderAccount(rs.getString("sender_account"));
        tx.setReceiverAccount(rs.getString("receiver_account"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setDescription(rs.getString("description"));
        tx.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        tx.setStatus(TransactionStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            tx.setCreatedAt(ts.toLocalDateTime());
        }
        return tx;
    }
}