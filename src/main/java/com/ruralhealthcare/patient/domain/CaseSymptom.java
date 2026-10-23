package com.ruralhealthcare.patient.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "case_symptoms")
public class CaseSymptom extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private HealthcareCase healthcareCase;

    @Column(name = "symptom_name", nullable = false, length = 150)
    private String symptomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SymptomSeverity severity = SymptomSeverity.MILD;

    @Column(name = "duration_days", nullable = false)
    private int durationDays = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected CaseSymptom() {
    }

    public CaseSymptom(UUID id, HealthcareCase healthcareCase, String symptomName,
                       SymptomSeverity severity, int durationDays, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.healthcareCase = healthcareCase;
        this.symptomName = symptomName;
        this.severity = severity != null ? severity : SymptomSeverity.MILD;
        this.durationDays = durationDays > 0 ? durationDays : 1;
        this.description = description;
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

    public HealthcareCase getHealthcareCase() {
        return healthcareCase;
    }

    public void setHealthcareCase(HealthcareCase healthcareCase) {
        this.healthcareCase = healthcareCase;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CaseSymptom that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private HealthcareCase healthcareCase;
        private String symptomName;
        private SymptomSeverity severity = SymptomSeverity.MILD;
        private int durationDays = 1;
        private String description;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder healthcareCase(HealthcareCase healthcareCase) {
            this.healthcareCase = healthcareCase;
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

        public CaseSymptom build() {
            return new CaseSymptom(id, healthcareCase, symptomName, severity, durationDays, description);
        }
    }
}
