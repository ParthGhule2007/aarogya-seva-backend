package com.ruralhealthcare.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruralhealthcare.ai.domain.UrgencyLevel;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UrgencyAssessment {

    private UrgencyLevel level;
    private String reason;

    public UrgencyAssessment() {
    }

    public UrgencyAssessment(UrgencyLevel level, String reason) {
        this.level = level;
        this.reason = reason;
    }

    public UrgencyLevel getLevel() {
        return level;
    }

    public void setLevel(UrgencyLevel level) {
        this.level = level;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
