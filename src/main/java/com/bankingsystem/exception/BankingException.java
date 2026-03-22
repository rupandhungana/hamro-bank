package com.bankingsystem.exception;

/**
 * Base exception for all application-level banking errors.
 *
 * <p><b>OOP Principle — Inheritance &amp; SRP:</b><br>
 * By having a common base, callers can catch {@code BankingException} to handle all
 * domain errors generically, or catch specific subclasses for fine-grained handling.
 * This is the root of the custom exception hierarchy.
 */
public class BankingException extends RuntimeException {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
