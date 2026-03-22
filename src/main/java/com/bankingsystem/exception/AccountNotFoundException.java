package com.bankingsystem.exception;

/**
 * Thrown when an operation targets an account that cannot be found.
 *
 * <p><b>OOP Principle — SRP:</b><br>
 * Distinguishes "account not found" from other failure categories.
 * Stores the identifier that was searched so diagnostic messages can reference it.
 */
public class AccountNotFoundException extends BankingException {

    private final String identifier;

    public AccountNotFoundException(String identifier) {
        super("No account found for: " + identifier);
        this.identifier = identifier;
    }

    /** @return the account number or user ID that could not be resolved */
    public String getIdentifier() { return identifier; }
}
