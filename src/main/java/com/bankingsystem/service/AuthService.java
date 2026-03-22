package com.bankingsystem.service;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.AccountRepository;
import com.bankingsystem.dao.SessionDAO;
import com.bankingsystem.dao.UserDAO;
import com.bankingsystem.dao.UserRepository;
import com.bankingsystem.exception.AuthenticationException;
import com.bankingsystem.model.*;
import com.bankingsystem.util.PasswordUtil;
import com.bankingsystem.util.SessionContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service class handling all authentication operations: registration and
 * login/logout.
 *
 * <p>
 * <b>OOP Principles applied:</b>
 * <ul>
 * <li><b>SRP</b> — this class has ONE responsibility: authentication.
 * Database persistence is delegated to repositories;
 * password hashing is delegated to {@link PasswordUtil}.</li>
 * <li><b>DIP</b> — depends on {@link UserRepository} and
 * {@link AccountRepository}
 * interfaces, not on concrete DAO classes. This makes the service
 * independently testable by injecting mock repositories.</li>
 * <li><b>Encapsulation</b> — all repository references are
 * {@code private final};
 * account-number generation is a private implementation detail.</li>
 * <li><b>Abstraction</b> — clients call clean {@code register()} /
 * {@code login()} methods
 * without knowing any SQL or hashing details.</li>
 * </ul>
 */
public class AuthService {

    /** Dependencies — declared against interfaces (DIP). */
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionDAO sessionDAO;

    /**
     * Default constructor wires the production JDBC implementations.
     *
     * <p>
     * In a Spring application this constructor would be replaced by
     * 
     * @Autowired constructor injection; in tests, the overloaded constructor
     *            below can be used to inject mocks.
     */
    public AuthService() {
        this.userRepository = new UserDAO();
        this.accountRepository = new AccountDAO();
        this.sessionDAO = new SessionDAO();
    }

    /**
     * Constructor for dependency injection (used in tests or future DI frameworks).
     *
     * @param userRepository    the user repository implementation to use
     * @param accountRepository the account repository implementation to use
     * @param sessionDAO        the session DAO to use
     */
    public AuthService(UserRepository userRepository,
            AccountRepository accountRepository,
            SessionDAO sessionDAO) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.sessionDAO = sessionDAO;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new user and creates their initial bank account.
     *
     * <p>
     * <b>Abstraction:</b> the caller passes plain-text fields; this method
     * handles normalisation, hashing, and all DB writes transparently.
     *
     * @return the persisted {@link User} with its generated userId
     * @throws IllegalArgumentException if uniqueness constraints are violated
     * @throws SQLException             if a database error occurs
     */
    public User register(String fullName,
            String phoneNumber,
            String email,
            String address,
            LocalDate dateOfBirth,
            String gender,
            String nationalId,
            String password,
            AccountType accountType) throws SQLException {

        // ── Uniqueness validation (fail-fast) ─────────────────────────────
        if (userRepository.isEmailTaken(email))
            throw new IllegalArgumentException("Email is already registered.");
        if (userRepository.isPhoneTaken(phoneNumber))
            throw new IllegalArgumentException("Phone number is already registered.");
        if (userRepository.isNationalIdTaken(nationalId))
            throw new IllegalArgumentException("National ID is already registered.");

        // ── Build User domain object ──────────────────────────────────────
        User user = new User();
        user.setFullName(fullName.trim());
        user.setPhoneNumber(phoneNumber.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setAddress(address.trim());
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender.toUpperCase());
        user.setNationalId(nationalId.trim().toUpperCase());
        user.setPasswordHash(PasswordUtil.hashPassword(password)); // no plaintext stored
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        int userId = userRepository.create(user);
        user.setUserId(userId);

        // ── Create the linked bank account ────────────────────────────────
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.create(account);

        return user;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates by email OR phone number and password.
     *
     * <p>
     * On success, updates the last_login timestamp, opens a new session record,
     * and stores the logged-in user in {@link SessionContext} for the lifecycle of
     * the
     * current JavaFX application session.
     *
     * @param identifier email or phone number
     * @param password   plain-text password
     * @return the authenticated {@link User}
     * @throws AuthenticationException if credentials are invalid or the account is
     *                                 suspended
     * @throws SQLException            if a database error occurs
     */
    public User login(String identifier, String password) throws SQLException {
        // Clear any previous session state to avoid contamination
        SessionContext.clear();

        if (identifier == null || identifier.isBlank())
            throw new AuthenticationException("Email or phone number is required.");

        // Try lowercase first (email), then original casing (phone numbers)
        User user = userRepository.findByEmailOrPhone(identifier.trim().toLowerCase())
                .or(() -> {
                    try {
                        return userRepository.findByEmailOrPhone(identifier.trim());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new AuthenticationException("No account found with that email or phone number."));

        if (user.getStatus() == UserStatus.BANNED)
            throw new AuthenticationException("Your account has been suspended. Please contact support.");

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash()))
            throw new AuthenticationException("Incorrect password. Please try again.");

        // ── Post-login bookkeeping ────────────────────────────────────────
        userRepository.updateLastLogin(user.getUserId());

        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(user.getUserId());
        session.setLoginTime(LocalDateTime.now());
        session.setActive(true);
        sessionDAO.create(session);

        // Populate application-wide session context
        SessionContext.setCurrentUser(user);
        SessionContext.setCurrentSession(session);
        accountRepository.findByUserId(user.getUserId())
                .ifPresent(SessionContext::setCurrentAccount);

        return user;
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Closes the active session and clears the session context.
     *
     * @throws SQLException if the session record update fails
     */
    public void logout() throws SQLException {
        Session session = SessionContext.getCurrentSession();
        if (session != null) {
            sessionDAO.closeSession(session.getSessionId());
        }
        SessionContext.clear();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Generates a unique account number.
     *
     * <p>
     * <b>Encapsulation:</b> generation strategy is a private implementation detail
     * —
     * the calling code does not need to know how account numbers are produced.
     *
     * @return a unique account number string
     */
    private String generateAccountNumber() {
        return "AC" + System.currentTimeMillis();
    }
}