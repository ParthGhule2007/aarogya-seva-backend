package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.SymptomSeverity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SymptomInputDto {

    @NotBlank(message = "Symptom name is mandatory")
    @Size(min = 2, max = 150, message = "Symptom name must be between 2 and 150 characters")
    private String symptomName;

    private SymptomSeverity severity = SymptomSeverity.MILD;

    @Min(value = 1, message = "Duration must be at least 1 day")
    private int durationDays = 1;

    private String description;

    public SymptomInputDto() {
    }

    public SymptomInputDto(String symptomName, SymptomSeverity severity, int durationDays, String description) {
        this.symptomName = symptomName;
        this.severity = severity != null ? severity : SymptomSeverity.MILD;
        this.durationDays = durationDays > 0 ? durationDays : 1;
        this.description = description;
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

    public static class Builder {
        private String symptomName;
        private SymptomSeverity severity = SymptomSeverity.MILD;
        private int durationDays = 1;
        private String description;

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

        public SymptomInputDto build() {
            return new SymptomInputDto(symptomName, severity, durationDays, description);
        }
    }
}
