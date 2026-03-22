package com.bankingsystem.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    private PasswordUtil() {}

    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String plain, String hash) {
        return BCrypt.checkpw(plain, hash);
    }
}