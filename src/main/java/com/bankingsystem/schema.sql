-- =========================================
-- BankFlow — Database Schema v3 (Added Transaction Notes)
-- =========================================
CREATE DATABASE IF NOT EXISTS banking_system;
USE banking_system;

-- =========================================
-- USERS TABLE
-- =========================================
CREATE TABLE users (
    user_id      INT AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(100)  NOT NULL,
    phone_number VARCHAR(15)   NOT NULL UNIQUE,
    email        VARCHAR(100)  NOT NULL UNIQUE,
    address      VARCHAR(255)  NOT NULL,
    date_of_birth DATE         NOT NULL,
    gender       ENUM('MALE','FEMALE','OTHER') NOT NULL,
    national_id  VARCHAR(30)   NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role         ENUM('USER','ADMIN')          NOT NULL DEFAULT 'USER',
    status       ENUM('ACTIVE','BANNED')       NOT NULL DEFAULT 'ACTIVE',
    is_verified  BOOLEAN                       NOT NULL DEFAULT FALSE,
    last_login   TIMESTAMP NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================================
-- ACCOUNTS TABLE
-- =========================================
CREATE TABLE accounts (
    account_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    account_number VARCHAR(20)  NOT NULL UNIQUE,
    account_type   ENUM('SAVINGS','CURRENT') NOT NULL,
    balance        DECIMAL(12,2)             NOT NULL DEFAULT 0.00,
    status         ENUM('ACTIVE','FROZEN')   NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================================
-- TRANSACTIONS TABLE
-- =========================================
CREATE TABLE transactions (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    sender_account   VARCHAR(20),
    receiver_account VARCHAR(20)  NOT NULL,
    amount           DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    description      VARCHAR(255),
    transaction_type ENUM('TRANSFER','ADMIN_DEPOSIT') NOT NULL,
    status           ENUM('SUCCESS','FAILED')         NOT NULL DEFAULT 'SUCCESS',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sender_account
        FOREIGN KEY (sender_account)   REFERENCES accounts(account_number) ON DELETE SET NULL,
    CONSTRAINT fk_receiver_account
        FOREIGN KEY (receiver_account) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- =========================================
-- SESSIONS TABLE
-- =========================================
CREATE TABLE sessions (
    session_id  VARCHAR(100) PRIMARY KEY,
    user_id     INT NOT NULL,
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================================
-- SEED DATA (Admin account)
-- =========================================
-- Password hash for "admin" using BCrypt
INSERT INTO users (full_name, phone_number, email, address, date_of_birth, gender,
                   national_id, password_hash, role, status, is_verified)
VALUES ('System Admin', '9800000000', 'admin@bank.com', 'Bank HQ', '1990-01-01',
        'MALE', 'NID-ADMIN-001',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMIN', 'ACTIVE', TRUE);

-- =========================================
-- INDEXES
-- =========================================
CREATE INDEX idx_phone       ON users(phone_number);
CREATE INDEX idx_email       ON users(email);
CREATE INDEX idx_acc_number  ON accounts(account_number);
CREATE INDEX idx_txn_date    ON transactions(created_at);
