package com.ruralhealthcare.facility.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FacilityType {
    SUB_CENTRE("Sub Centre"),
    PHC("Primary Health Centre"),
    CHC("Community Health Centre"),
    SUB_DISTRICT_HOSPITAL("Sub-District Hospital"),
    DISTRICT_HOSPITAL("District Hospital"),
    TERTIARY_HOSPITAL("Tertiary Hospital"),
    MOBILE_UNIT("Mobile Health Unit");

    private final String displayName;

    FacilityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static FacilityType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        if ("PRIMARY_HEALTH_CENTRE".equals(normalized)) return PHC;
        if ("COMMUNITY_HEALTH_CENTRE".equals(normalized)) return CHC;
        if ("MOBILE_HEALTH_UNIT".equals(normalized)) return MOBILE_UNIT;

        for (FacilityType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FacilityType: " + value);
    }
}
