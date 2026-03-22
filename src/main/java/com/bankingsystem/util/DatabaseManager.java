package com.bankingsystem.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/banking_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "rupan2888";

    private static final String URL = System.getenv().getOrDefault("BANK_DB_URL", DEFAULT_URL);
    private static final String USER = System.getenv().getOrDefault("BANK_DB_USER", DEFAULT_USER);
    private static final String PASSWORD = System.getenv().getOrDefault("BANK_DB_PASSWORD", DEFAULT_PASSWORD);

    private DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL driver not found", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (message.contains("public key retrieval")) {
                throw new SQLException(
                        "Database connection failed due to MySQL public key retrieval settings. " +
                        "Use a URL with allowPublicKeyRetrieval=true.",
                        e
                );
            }
            throw e;
        }
    }
}
