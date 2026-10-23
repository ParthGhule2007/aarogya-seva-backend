package com.ruralhealthcare.sos.domain;

/**
 * Emergency alert target group selected by the patient.
 */
public enum TargetType {
    CAREGIVER,
    FACILITY,
    NEARBY_PEOPLE;

    public static TargetType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (TargetType target : values()) {
            if (target.name().equalsIgnoreCase(value.trim())) {
                return target;
            }
        }
        throw new IllegalArgumentException("Unknown TargetType: " + value);
    }
}
