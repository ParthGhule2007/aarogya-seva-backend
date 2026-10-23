package com.ruralhealthcare.ai.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RiskLevel {
    LOW("Low Clinical Risk"),
    MODERATE("Moderate Clinical Risk"),
    HIGH("High Clinical Risk"),
    CRITICAL("Critical Emergency Risk");

    private final String description;

    RiskLevel(String description) {
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
    public static RiskLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (RiskLevel level : values()) {
            if (level.name().equalsIgnoreCase(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown RiskLevel: " + value);
    }
}
