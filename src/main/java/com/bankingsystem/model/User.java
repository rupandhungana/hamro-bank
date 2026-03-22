package com.bankingsystem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain object that maps to the `users` table (schema v2). */
public class User {

    private int           userId;
    private String        fullName;
    private String        phoneNumber;
    private String        email;
    private String        address;
    private LocalDate     dateOfBirth;
    private String        gender;       // MALE | FEMALE | OTHER
    private String        nationalId;
    private String        passwordHash;
    private Role          role;
    private UserStatus    status;
    private boolean       verified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public User() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getUserId()                        { return userId; }
    public void setUserId(int userId)             { this.userId = userId; }

    public String getFullName()                   { return fullName; }
    public void setFullName(String fullName)      { this.fullName = fullName; }

    public String getPhoneNumber()                { return phoneNumber; }
    public void setPhoneNumber(String v)          { this.phoneNumber = v; }

    public String getEmail()                      { return email; }
    public void setEmail(String email)            { this.email = email; }

    public String getAddress()                    { return address; }
    public void setAddress(String address)        { this.address = address; }

    public LocalDate getDateOfBirth()             { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v)       { this.dateOfBirth = v; }

    public String getGender()                     { return gender; }
    public void setGender(String gender)          { this.gender = gender; }

    public String getNationalId()                 { return nationalId; }
    public void setNationalId(String nationalId)  { this.nationalId = nationalId; }

    public String getPasswordHash()               { return passwordHash; }
    public void setPasswordHash(String v)         { this.passwordHash = v; }

    public Role getRole()                         { return role; }
    public void setRole(Role role)                { this.role = role; }

    public UserStatus getStatus()                 { return status; }
    public void setStatus(UserStatus status)      { this.status = status; }

    public boolean isVerified()                   { return verified; }
    public void setVerified(boolean verified)     { this.verified = verified; }

    public LocalDateTime getLastLogin()           { return lastLogin; }
    public void setLastLogin(LocalDateTime v)     { this.lastLogin = v; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime v)     { this.createdAt = v; }

    // ── Helpers used by sidebar / greeting ───────────────────────────────────
    /** Returns the first word of fullName, e.g. "Alex" from "Alex Johnson". */
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) return "User";
        return fullName.split("\\s+")[0];
    }

    @Override
    public String toString() {
        return "User{id=" + userId + ", email='" + email + "', role=" + role + "}";
    }
}