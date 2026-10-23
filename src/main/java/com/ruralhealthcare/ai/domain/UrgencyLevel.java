package com.ruralhealthcare.ai.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UrgencyLevel {
    ROUTINE("Routine - Scheduled Evaluation"),
    PRIORITY("Priority - Within 24 to 48 Hours"),
    URGENT("Urgent - Immediate Clinical Attention Required"),
    EMERGENCY("Emergency - Immediate Stabilization and Care Required");

    private final String description;

    UrgencyLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static UrgencyLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (UrgencyLevel level : values()) {
            if (level.name().equalsIgnoreCase(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown UrgencyLevel: " + value);
    }
}
