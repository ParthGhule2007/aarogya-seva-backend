package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.SymptomSeverity;

import java.time.Instant;
import java.util.UUID;

public class CaseSymptomDto {

    private UUID id;
    private UUID caseId;
    private String symptomName;
    private SymptomSeverity severity;
    private int durationDays;
    private String description;
    private Instant createdAt;

    public CaseSymptomDto() {
    }

    public CaseSymptomDto(UUID id, UUID caseId, String symptomName, SymptomSeverity severity,
                          int durationDays, String description, Instant createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.symptomName = symptomName;
        this.severity = severity;
        this.durationDays = durationDays;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    public SymptomSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(SymptomSeverity severity) {
        this.severity = severity;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private UUID id;
        private UUID caseId;
        private String symptomName;
        private SymptomSeverity severity;
        private int durationDays;
        private String description;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder caseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder symptomName(String symptomName) {
            this.symptomName = symptomName;
            return this;
        }

        public Builder severity(SymptomSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder durationDays(int durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CaseSymptomDto build() {
            return new CaseSymptomDto(id, caseId, symptomName, severity, durationDays, description, createdAt);
        }
    }
}
