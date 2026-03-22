package com.bankingsystem.dao;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for Account persistence.
 *
 * <p><b>OOP Principles applied:</b>
 * <ul>
 *   <li><b>Abstraction</b> — hides all JDBC/SQL details from the service layer</li>
 *   <li><b>DIP</b>         — callers depend on this interface, not on {@link AccountDAO}</li>
 *   <li><b>ISP</b>         — only account-specific operations are in this interface</li>
 * </ul>
 */
public interface AccountRepository {

    /**
     * Creates the account record and returns the generated primary key.
     * @param account fully populated Account (accountId will be ignored / overwritten)
     * @return the generated accountId
     */
    int create(Account account) throws SQLException;

    /**
     * Looks up an account by the owning user's ID.
     * @return Optional containing the Account, or empty if the user has no account
     */
    Optional<Account> findByUserId(int userId) throws SQLException;

    /**
     * Looks up an account by its unique account number.
     * @return Optional containing the Account, or empty if not found
     */
    Optional<Account> findByAccountNumber(String accountNumber) throws SQLException;

    /**
     * Returns all accounts in the system.
     */
    List<Account> findAll() throws SQLException;

    /**
     * Changes the operational status of an account (ACTIVE, FROZEN, CLOSED).
     * @param accountNumber the target account number
     * @param status        the new status to apply
     */
    void updateStatus(String accountNumber, AccountStatus status) throws SQLException;
}
