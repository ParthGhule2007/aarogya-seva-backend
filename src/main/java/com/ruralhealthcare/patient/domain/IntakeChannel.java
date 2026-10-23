package com.ruralhealthcare.patient.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum IntakeChannel {
    FORM("Digital Web/Mobile Form"),
    CHAT("Conversational Health Assistant"),
    VOICE("Voice-based Intake Interface"),
    COMMUNITY_HEALTH_WORKER("Assisted Intake by CHW / ASHA Worker"),
    PATIENT_PORTAL("Patient Portal / Mobile Self-Service"),
    ASHA_WORKER("Assisted Intake by ASHA Worker"),
    FIELD_CLINIC("Rural Outreach / Field Clinic");

    private final String description;

    IntakeChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static IntakeChannel fromString(String value) {
        if (value == null || value.isBlank()) {
            return FORM;
        }
        for (IntakeChannel channel : IntakeChannel.values()) {
            if (channel.name().equalsIgnoreCase(value.trim())) {
                return channel;
            }
        }
        return FORM;
    }
}

