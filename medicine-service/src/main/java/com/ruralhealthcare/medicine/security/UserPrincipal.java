package com.ruralhealthcare.medicine.security;

import java.util.UUID;

public class UserPrincipal {

    private final UUID userId;
    private final String role;
    private final boolean authenticated;

    public UserPrincipal(UUID userId, String role, boolean authenticated) {
        this.userId = userId;
        this.role = role != null ? role.toUpperCase() : "ANONYMOUS";
        this.authenticated = authenticated;
    }

    public static UserPrincipal anonymous() {
        return new UserPrincipal(null, "ANONYMOUS", false);
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
}
