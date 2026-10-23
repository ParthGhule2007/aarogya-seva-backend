package com.ruralhealthcare.auth.security;

import com.ruralhealthcare.auth.domain.UserRole;

import java.util.UUID;

public class UserPrincipal {

    private final UUID userId;
    private final String username;
    private final UserRole role;
    private final String fullName;
    private final String phoneNumber;
    private final String associatedEntityId;

    public UserPrincipal(UUID userId, String username, UserRole role, String fullName, String phoneNumber, String associatedEntityId) {
        this.userId = userId;
        this.username = username;
        this.role = role != null ? role : UserRole.PATIENT;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.associatedEntityId = associatedEntityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAssociatedEntityId() {
        return associatedEntityId;
    }

    public boolean isPatient() {
        return role == UserRole.PATIENT;
    }

    public boolean isCaregiver() {
        return role == UserRole.CAREGIVER;
    }

    public boolean isAsha() {
        return role == UserRole.ASHA;
    }

    public boolean isDoctor() {
        return role == UserRole.DOCTOR;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
