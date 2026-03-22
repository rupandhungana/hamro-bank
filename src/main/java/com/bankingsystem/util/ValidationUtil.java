package com.bankingsystem.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class ValidationUtil {
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern CITIZENSHIP = Pattern.compile("^[A-Z0-9-]{5,20}$");
    private static final Pattern ACCOUNT = Pattern.compile("^AC\\d{10,}$");

    private ValidationUtil() {}

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }

    public static boolean isValidCitizenshipId(String id) {
        return id != null && CITIZENSHIP.matcher(id).matches();
    }

    public static boolean isValidAccountNumber(String account) {
        return account != null && ACCOUNT.matcher(account).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
