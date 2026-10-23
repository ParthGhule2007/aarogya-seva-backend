package com.ruralhealthcare.sos.domain;

/**
 * Type of registered user registering FCM device tokens.
 */
public enum UserType {
    PATIENT,
    CAREGIVER,
    ASHA,
    FACILITY,
    DOCTOR;

    public static UserType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (UserType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UserType: " + value);
    }
}
