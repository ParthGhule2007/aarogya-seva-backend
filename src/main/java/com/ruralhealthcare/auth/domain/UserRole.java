package com.ruralhealthcare.auth.domain;

public enum UserRole {
    PATIENT,
    CAREGIVER,
    ASHA,
    DOCTOR,
    FACILITY_ADMIN,
    ADMIN;

    public static UserRole fromString(String role) {
        if (role == null || role.isBlank()) {
            return PATIENT;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PATIENT;
        }
    }
}
