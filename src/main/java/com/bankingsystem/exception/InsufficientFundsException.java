package com.bankingsystem.exception;

/**
 * Thrown when a bank account has insufficient funds for a requested transfer or withdrawal.
 *
 * <p><b>OOP Principle — SRP:</b><br>
 * Carries the available balance and requested amount so the UI can display specific,
 * helpful messages (e.g., "Your balance is NPR 500 but you requested NPR 800").
 */
public class InsufficientFundsException extends BankingException {

    private final java.math.BigDecimal available;
    private final java.math.BigDecimal requested;

    public InsufficientFundsException(java.math.BigDecimal available,
                                      java.math.BigDecimal requested) {
        super(String.format(
                "Insufficient funds: available NPR %.2f, requested NPR %.2f",
                available, requested));
        this.available = available;
        this.requested = requested;
    }

    /** @return the account balance at the time the exception was thrown */
    public java.math.BigDecimal getAvailable() { return available; }

    /** @return the amount that was requested */
    public java.math.BigDecimal getRequested() { return requested; }
}
