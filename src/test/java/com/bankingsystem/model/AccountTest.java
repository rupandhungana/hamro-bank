package com.bankingsystem.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    public void testAccountCreationAndRegistrationLogic() {
        // Simulating the creation of a new user's bank account during registration
        Account newAccount = new Account();
        newAccount.setAccountId(1);
        newAccount.setUserId(100);
        newAccount.setAccountNumber("AC123456"); // Simulated Random Account Number
        newAccount.setAccountType(AccountType.SAVINGS);
        
        // Simulating an initial deposit or default balance constraint
        newAccount.setBalance(new BigDecimal("1500.00"));
        newAccount.setStatus(AccountStatus.ACTIVE);
        newAccount.setCreatedAt(LocalDateTime.now());

        // Verifying the Account functionality initialized exactly as intended
        assertEquals("AC123456", newAccount.getAccountNumber(), "Account generation sequence failed.");
        assertEquals(new BigDecimal("1500.00"), newAccount.getBalance(), "Starting balance calculation mismatch.");
        assertEquals(AccountStatus.ACTIVE, newAccount.getStatus(), "Account was not activated upon generation.");
    }
}
