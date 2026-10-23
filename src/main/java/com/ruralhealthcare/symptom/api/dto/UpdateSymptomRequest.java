package com.ruralhealthcare.symptom.api.dto;

import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateSymptomRequest {

    @Size(min = 2, max = 150, message = "Symptom name must be between 2 and 150 characters")
    private String symptomName;

    private SymptomSeverity severity;

    @Min(value = 1, message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration cannot exceed 365 days")
    private Integer durationDays;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 100, message = "Body site cannot exceed 100 characters")
    private String bodySite;

    public UpdateSymptomRequest() {
    }

    public UpdateSymptomRequest(String symptomName, SymptomSeverity severity,
                                Integer durationDays, String description, String bodySite) {
        this.symptomName = symptomName;
        this.severity = severity;
        this.durationDays = durationDays;
        this.description = description;
        this.bodySite = bodySite;
    }

    public static Builder builder() {
        return new Builder();
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

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
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

    public static class Builder {
        private String symptomName;
        private SymptomSeverity severity;
        private Integer durationDays;
        private String description;
        private String bodySite;

        public Builder symptomName(String symptomName) {
            this.symptomName = symptomName;
            return this;
        }

        public Builder severity(SymptomSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder durationDays(Integer durationDays) {
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

        public UpdateSymptomRequest build() {
            return new UpdateSymptomRequest(symptomName, severity, durationDays, description, bodySite);
        }
    }
}
