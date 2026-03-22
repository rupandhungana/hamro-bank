package com.bankingsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain object representing a bank account in the system.
 *
 * <p><b>OOP Principles applied:</b>
 * <ul>
 *   <li><b>Encapsulation</b>  — all fields are {@code private}; state is only accessible
 *       through public getters/setters, preventing direct external mutation.</li>
 *   <li><b>Abstraction</b>    — the account's balance, status, and type are exposed
 *       through a clean interface; internal implementation details are hidden.</li>
 *   <li><b>Immutability-friendly</b> — the full-argument constructor allows creating
 *       complete, valid account objects in a single call (Builder-ready design).</li>
 * </ul>
 *
 * <p>Financial amounts use {@link BigDecimal} — never {@code double} — to avoid
 * floating-point rounding errors in monetary calculations (Java best practice).
 */
public class Account {

    // ── Encapsulated fields (all private) ─────────────────────────────────────
    private int           accountId;
    private int           userId;
    private String        accountNumber;
    private AccountType   accountType;
    private BigDecimal    balance;
    private AccountStatus status;
    private LocalDateTime createdAt;

    /** Default no-arg constructor (required by JPA-style mapping). */
    public Account() {}

    /**
     * Full-argument constructor for creating a complete Account object at once.
     *
     * <p>This constructor demonstrates <b>encapsulation</b>: the object is initialised
     * in a valid state in a single step, reducing the risk of partially-constructed objects.
     */
    public Account(int accountId, int userId, String accountNumber,
                   AccountType accountType, BigDecimal balance,
                   AccountStatus status, LocalDateTime createdAt) {
        this.accountId     = accountId;
        this.userId        = userId;
        this.accountNumber = accountNumber;
        this.accountType   = accountType;
        this.balance       = balance;
        this.status        = status;
        this.createdAt     = createdAt;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getAccountId()               { return accountId; }
    public void setAccountId(int id)        { this.accountId = id; }

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getAccountNumber()                      { return accountNumber; }
    public void setAccountNumber(String accountNumber)    { this.accountNumber = accountNumber; }

    public AccountType getAccountType()                   { return accountType; }
    public void setAccountType(AccountType accountType)   { this.accountType = accountType; }

    public BigDecimal getBalance()                        { return balance; }
    public void setBalance(BigDecimal balance)            { this.balance = balance; }

    public AccountStatus getStatus()                      { return status; }
    public void setStatus(AccountStatus status)           { this.status = status; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    // ── Object contract ───────────────────────────────────────────────────────

    /**
     * Two accounts are equal if they share the same unique account number.
     * This follows the <b>value equality</b> principle — identity is defined by
     * the natural key, not the object reference.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return Objects.equals(accountNumber, other.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    /**
     * Human-readable representation — useful for logging and debugging.
     * Note: balance is included; never log this in production without masking.
     */
    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", userId=" + userId +
                ", accountNumber='" + accountNumber + '\'' +
                ", type=" + accountType +
                ", balance=" + balance +
                ", status=" + status +
                '}';
    }
}