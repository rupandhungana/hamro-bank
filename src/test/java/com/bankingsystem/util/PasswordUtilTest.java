package com.bankingsystem.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    public void testSuccessfulLoginVerification() {
        // Step 1: Simulate the Registration process generating a secure hash
        String userRegisteredPassword = "MySuperSecretPassword123";
        String databaseStoredHash = PasswordUtil.hashPassword(userRegisteredPassword);
        
        // Assert the database never holds the literal password
        assertNotEquals(userRegisteredPassword, databaseStoredHash, "System failed to encrypt credential.");
        
        // Step 2: Simulate the Login process checking the user's typed input
        assertTrue(PasswordUtil.verifyPassword(userRegisteredPassword, databaseStoredHash), "Login verification algorithm mismatch.");
    }

    @Test
    public void testFailedLoginDueToWrongPassword() {
        // Assume user registered with "MySuperSecretPassword123"
        String databaseStoredHash = PasswordUtil.hashPassword("MySuperSecretPassword123");
        
        // Simulate a typo or a hacker attempting a wrong password login
        String maliciousLoginAttempt = "WrongPassword123";
        
        // Verify the login functionality safely catches and actively rejects incorrect matches
        assertFalse(PasswordUtil.verifyPassword(maliciousLoginAttempt, databaseStoredHash), "Security Breach! System authorized incorrect password.");
    }
}
