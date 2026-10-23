package com.ruralhealthcare.sos.security;

import java.util.UUID;

public class UserPrincipal {

    private final UUID userId;
    private final String role;
    private final boolean authenticated;

    public UserPrincipal(UUID userId, String role) {
        this(userId, role, userId != null);
    }

    public UserPrincipal(UUID userId, String role, boolean authenticated) {
        this.userId = userId;
        this.role = role != null ? role.toUpperCase() : "PATIENT";
        this.authenticated = authenticated;
    }

    public static UserPrincipal anonymous() {
        return new UserPrincipal(null, "ANONYMOUS", false);
    }

    public static UserPrincipal of(UUID userId, String role) {
        return new UserPrincipal(userId, role, true);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isPatient() {
        return "PATIENT".equalsIgnoreCase(role);
    }

    public boolean isCaregiver() {
        return "CAREGIVER".equalsIgnoreCase(role);
    }

    public boolean isAshaOrHealthWorker() {
        return "ASHA".equalsIgnoreCase(role) || "HEALTH_WORKER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
}
