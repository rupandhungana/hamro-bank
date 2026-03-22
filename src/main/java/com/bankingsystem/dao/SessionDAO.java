package com.bankingsystem.dao;

import com.bankingsystem.model.Session;
import com.bankingsystem.util.DatabaseManager;

import java.sql.*;

public class SessionDAO {

    public void create(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (session_id, user_id, login_time, logout_time, is_active) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.getSessionId());
            ps.setInt(2, session.getUserId());
            ps.setTimestamp(3, Timestamp.valueOf(session.getLoginTime()));
            if (session.getLogoutTime() == null) {
                ps.setNull(4, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(session.getLogoutTime()));
            }
            ps.setBoolean(5, session.isActive());
            ps.executeUpdate();
        }
    }

    public void closeSession(String sessionId) throws SQLException {
        String sql = "UPDATE sessions SET logout_time = CURRENT_TIMESTAMP, is_active = false WHERE session_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        }
    }

    public Session findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE user_id = ? AND is_active = true ORDER BY login_time DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session s = new Session();
                    s.setSessionId(rs.getString("session_id"));
                    s.setUserId(rs.getInt("user_id"));
                    Timestamp login = rs.getTimestamp("login_time");
                    if (login != null) {
                        s.setLoginTime(login.toLocalDateTime());
                    }
                    Timestamp logout = rs.getTimestamp("logout_time");
                    if (logout != null) {
                        s.setLogoutTime(logout.toLocalDateTime());
                    }
                    s.setActive(rs.getBoolean("is_active"));
                    return s;
                }
            }
        }
        return null;
    }
}