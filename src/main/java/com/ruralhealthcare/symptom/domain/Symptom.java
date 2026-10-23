package com.ruralhealthcare.symptom.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "symptoms")
public class Symptom extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "symptom_name", nullable = false, length = 150)
    private String symptomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SymptomSeverity severity = SymptomSeverity.MILD;

    @Column(name = "duration_days", nullable = false)
    private int durationDays = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "body_site", length = 100)
    private String bodySite;

    protected Symptom() {
    }

    public Symptom(UUID id, UUID caseId, String symptomName, SymptomSeverity severity,
                   int durationDays, String description, String bodySite) {
        this.id = id != null ? id : UUID.randomUUID();
        this.caseId = caseId;
        this.symptomName = symptomName;
        this.severity = severity != null ? severity : SymptomSeverity.MILD;
        this.durationDays = durationDays > 0 ? durationDays : 1;
        this.description = description;
        this.bodySite = bodySite;
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

    public String getBodySite() {
        return bodySite;
    }

    public void setBodySite(String bodySite) {
        this.bodySite = bodySite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symptom symptom = (Symptom) o;
        return Objects.equals(id, symptom.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private UUID id;
        private UUID caseId;
        private String symptomName;
        private SymptomSeverity severity = SymptomSeverity.MILD;
        private int durationDays = 1;
        private String description;
        private String bodySite;

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

        public Builder bodySite(String bodySite) {
            this.bodySite = bodySite;
            return this;
        }

        public Symptom build() {
            return new Symptom(id, caseId, symptomName, severity, durationDays, description, bodySite);
        }
    }
}
