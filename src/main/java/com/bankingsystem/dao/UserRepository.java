package com.bankingsystem.dao;

import com.bankingsystem.model.User;
import com.bankingsystem.model.UserStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for User persistence (DIP — Dependency Inversion Principle).
 *
 * <p>High-level services depend on this interface, NOT on the concrete {@link UserDAO}.
 * This decouples business logic from the database implementation, enabling easy
 * testing with mock repositories and future swapping of persistence strategies.
 *
 * <p><b>OOP Principles applied:</b>
 * <ul>
 *   <li><b>Abstraction</b>  — hides all SQL details behind clean method contracts</li>
 *   <li><b>DIP</b>          — services depend on this abstraction, not on MySql specifics</li>
 *   <li><b>ISP</b>          — only user-relevant operations are captured here</li>
 * </ul>
 */
public interface UserRepository {

    /**
     * Persists a new User and returns the generated primary key.
     * @param user a fully populated User object (without userId set)
     * @return the generated userId
     * @throws SQLException if the record cannot be inserted
     */
    int create(User user) throws SQLException;

    /**
     * Finds a user by their unique email address.
     * @param email the email to search for
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email) throws SQLException;

    /**
     * Finds a user by email OR phone number — supports flexible login identifiers.
     * @param identifier email or phone number
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByEmailOrPhone(String identifier) throws SQLException;

    /**
     * Finds a user by their primary key.
     * @param userId the user's database ID
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findById(int userId) throws SQLException;

    /**
     * Returns all users ordered by registration date (most recent first).
     */
    List<User> findAll() throws SQLException;

    /** @return true if the email is already registered */
    boolean isEmailTaken(String email) throws SQLException;

    /** @return true if the phone number is already registered */
    boolean isPhoneTaken(String phone) throws SQLException;

    /** @return true if the national ID is already registered */
    boolean isNationalIdTaken(String nationalId) throws SQLException;

    /**
     * Updates the status of the user (ACTIVE, BANNED, etc.).
     * @param userId the target user's ID
     * @param status the new status
     */
    void updateStatus(int userId, UserStatus status) throws SQLException;

    /**
     * Stamps the user's last_login timestamp with the current time.
     * @param userId the target user's ID
     */
    void updateLastLogin(int userId) throws SQLException;
}
