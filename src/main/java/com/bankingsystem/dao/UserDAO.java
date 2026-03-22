package com.bankingsystem.dao;

import com.bankingsystem.model.Role;
import com.bankingsystem.model.User;
import com.bankingsystem.model.UserStatus;
import com.bankingsystem.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link UserRepository}.
 *
 * <p><b>OOP Principles applied:</b>
 * <ul>
 *   <li><b>SRP</b>           — sole responsibility: SQL operations on the {@code users} table</li>
 *   <li><b>Encapsulation</b> — all SQL strings and the {@code map()} helper are private</li>
 *   <li><b>DIP</b>           — implements the {@link UserRepository} interface; callers depend
 *                              on the interface, not this concrete class</li>
 *   <li><b>LSP</b>           — satisfies every contract defined in {@link UserRepository}</li>
 * </ul>
 */
public class UserDAO implements UserRepository {

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public int create(User user) throws SQLException {
        String sql = """
                INSERT INTO users
                    (full_name, phone_number, email, address, date_of_birth,
                     gender, national_id, password_hash, role, status)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhoneNumber());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getAddress());
            ps.setDate  (5, user.getDateOfBirth() != null
                    ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setString(6, user.getGender());
            ps.setString(7, user.getNationalId());
            ps.setString(8, user.getPasswordHash());
            ps.setString(9, user.getRole().name());
            ps.setString(10, user.getStatus().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        return findOneWhere("email = ?", email);
    }

    @Override
    public Optional<User> findByEmailOrPhone(String identifier) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? OR phone_number = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
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
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(map(rs));
        }
        return users;
    }

    // ── Uniqueness checks ─────────────────────────────────────────────────────

    @Override
    public boolean isEmailTaken(String email) throws SQLException {
        return exists("email = ?", email);
    }

    @Override
    public boolean isPhoneTaken(String phone) throws SQLException {
        return exists("phone_number = ?", phone);
    }

    @Override
    public boolean isNationalIdTaken(String nationalId) throws SQLException {
        return exists("national_id = ?", nationalId);
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Override
    public void updateStatus(int userId, UserStatus status) throws SQLException {
        exec("UPDATE users SET status = ? WHERE user_id = ?", status.name(), userId);
    }

    @Override
    public void updateLastLogin(int userId) throws SQLException {
        exec("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Runs a SELECT with a single String parameter and returns the first matching User.
     * Encapsulates the JDBC boilerplate to keep public methods clean (DRY principle).
     */
    private Optional<User> findOneWhere(String condition, String value) throws SQLException {
        String sql = "SELECT * FROM users WHERE " + condition + " LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether any row satisfies the given WHERE condition.
     */
    private boolean exists(String condition, String value) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE " + condition;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Generic UPDATE/INSERT executor.  Keeps public methods free of JDBC boilerplate (DRY).
     */
    private void exec(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    /**
     * Maps a ResultSet row to a fully populated {@link User} object.
     *
     * <p><b>Encapsulation</b>: this mapping logic is private — callers receive
     * clean domain objects, never raw ResultSet data.
     */
    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId      (rs.getInt   ("user_id"));
        user.setFullName    (rs.getString("full_name"));
        user.setPhoneNumber (rs.getString("phone_number"));
        user.setEmail       (rs.getString("email"));
        user.setAddress     (rs.getString("address"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) user.setDateOfBirth(dob.toLocalDate());
        user.setGender      (rs.getString("gender"));
        user.setNationalId  (rs.getString("national_id"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole        (Role.valueOf(rs.getString("role")));
        user.setStatus      (UserStatus.valueOf(rs.getString("status")));
        user.setVerified    (rs.getBoolean("is_verified"));
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) user.setLastLogin(ll.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) user.setCreatedAt(ca.toLocalDateTime());
        return user;
    }
}