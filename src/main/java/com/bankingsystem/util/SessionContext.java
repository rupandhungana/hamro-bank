package com.bankingsystem.util;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.Session;
import com.bankingsystem.model.User;

public class SessionContext {
    private static User currentUser;
    private static Account currentAccount;
    private static Session currentSession;

    private SessionContext() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Account getCurrentAccount() {
        return currentAccount;
    }

    public static void setCurrentAccount(Account account) {
        currentAccount = account;
    }

    public static Session getCurrentSession() {
        return currentSession;
    }

    public static void setCurrentSession(Session session) {
        currentSession = session;
    }

    public static void clear() {
        currentUser = null;
        currentAccount = null;
        currentSession = null;
    }
}