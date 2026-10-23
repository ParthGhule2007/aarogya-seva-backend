package com.ruralhealthcare.symptom.api.dto;

import com.ruralhealthcare.symptom.domain.SymptomSeverity;

import java.time.Instant;
import java.util.UUID;

public class SymptomDto {

    private UUID id;
    private UUID caseId;
    private String symptomName;
    private SymptomSeverity severity;
    private String severityDescription;
    private int durationDays;
    private String description;
    private String bodySite;
    private Instant createdAt;
    private Instant updatedAt;

    public SymptomDto() {
    }

    public SymptomDto(UUID id, UUID caseId, String symptomName, SymptomSeverity severity,
                      String severityDescription, int durationDays, String description,
                      String bodySite, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.caseId = caseId;
        this.symptomName = symptomName;
        this.severity = severity;
        this.severityDescription = severityDescription;
        this.durationDays = durationDays;
        this.description = description;
        this.bodySite = bodySite;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getSeverityDescription() {
        return severityDescription;
    }

    public void setSeverityDescription(String severityDescription) {
        this.severityDescription = severityDescription;
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

    public String getBodySite() {
        return bodySite;
    }

    public void setBodySite(String bodySite) {
        this.bodySite = bodySite;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Builder {
        private UUID id;
        private UUID caseId;
        private String symptomName;
        private SymptomSeverity severity;
        private String severityDescription;
        private int durationDays;
        private String description;
        private String bodySite;
        private Instant createdAt;
        private Instant updatedAt;

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

        public Builder severityDescription(String severityDescription) {
            this.severityDescription = severityDescription;
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

        public Builder bodySite(String bodySite) {
            this.bodySite = bodySite;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SymptomDto build() {
            return new SymptomDto(id, caseId, symptomName, severity, severityDescription,
                    durationDays, description, bodySite, createdAt, updatedAt);
        }
    }
}
