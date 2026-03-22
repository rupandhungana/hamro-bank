package com.bankingsystem.model;

import java.time.LocalDateTime;

public class Session {
    private String sessionId;
    private int userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private boolean active;

    public Session() {}

    public Session(String sessionId, int userId, LocalDateTime loginTime, LocalDateTime logoutTime, boolean active) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.active = active;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}