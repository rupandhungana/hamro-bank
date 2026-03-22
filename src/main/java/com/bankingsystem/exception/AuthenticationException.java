package com.bankingsystem.exception;

/**
 * Thrown when login credentials are invalid (wrong password, unknown user, banned account).
 *
 * <p><b>OOP Principle — SRP:</b><br>
 * Each exception class has one clear responsibility: communicating a specific category
 * of failure. Controllers can catch this specifically to show an appropriate UI message.
 */
public class AuthenticationException extends BankingException {

    public AuthenticationException(String message) {
        super(message);
    }
}
