package com.ruralhealthcare.symptom.domain;

public enum SymptomSeverity {
    MILD("Mild symptoms, manageable without immediate intervention"),
    MODERATE("Noticeable discomfort impacting daily activities"),
    SEVERE("Significant pain or impairment requiring clinical evaluation"),
    CRITICAL("Life-threatening condition requiring emergency response");

    private final String description;

    SymptomSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
