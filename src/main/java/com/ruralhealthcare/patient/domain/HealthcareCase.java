package com.ruralhealthcare.patient.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import com.ruralhealthcare.common.domain.GeoLocation;
import com.ruralhealthcare.common.exception.ValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "healthcare_cases")
public class HealthcareCase extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CaseStatus status = CaseStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private CasePriority priority = CasePriority.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_channel", nullable = false, length = 30)
    private IntakeChannel intakeChannel;

    @Column(name = "chief_complaint", length = 255)
    private String chiefComplaint;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Embedded
    private GeoLocation geoLocation;

    @OneToMany(mappedBy = "healthcareCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CaseSymptom> symptoms = new HashSet<>();

    protected HealthcareCase() {
    }

    public HealthcareCase(UUID id, String caseNumber, Patient patient, CaseStatus status,
                          CasePriority priority, IntakeChannel intakeChannel, String chiefComplaint,
                          String notes, GeoLocation geoLocation, Set<CaseSymptom> symptoms) {
        this.id = id != null ? id : UUID.randomUUID();
        this.caseNumber = caseNumber;
        this.patient = patient;
        this.status = status != null ? status : CaseStatus.CREATED;
        this.priority = priority != null ? priority : CasePriority.LOW;
        this.intakeChannel = intakeChannel;
        this.chiefComplaint = chiefComplaint;
        this.notes = notes;
        this.geoLocation = geoLocation;
        this.symptoms = symptoms != null ? symptoms : new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addSymptom(CaseSymptom symptom) {
        symptoms.add(symptom);
        symptom.setHealthcareCase(this);
        if (this.status == CaseStatus.CREATED) {
            this.status = CaseStatus.SYMPTOMS_RECORDED;
        }
    }

    public void removeSymptom(CaseSymptom symptom) {
        symptoms.remove(symptom);
        symptom.setHealthcareCase(null);
    }

    public void transitionStatus(CaseStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new ValidationException(String.format(
                    "Invalid case status transition from [%s] to [%s]. Valid target states: %s",
                    this.status, newStatus, this.status.getValidNextStates()
            ));
        }
        this.status = newStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public void setPriority(CasePriority priority) {
        this.priority = priority;
    }

    public IntakeChannel getIntakeChannel() {
        return intakeChannel;
    }

    public void setIntakeChannel(IntakeChannel intakeChannel) {
        this.intakeChannel = intakeChannel;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public Set<CaseSymptom> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(Set<CaseSymptom> symptoms) {
        this.symptoms = symptoms;
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String caseNumber;
        private Patient patient;
        private CaseStatus status = CaseStatus.CREATED;
        private CasePriority priority = CasePriority.LOW;
        private IntakeChannel intakeChannel;
        private String chiefComplaint;
        private String notes;
        private GeoLocation geoLocation;
        private Set<CaseSymptom> symptoms = new HashSet<>();

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder caseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
            return this;
        }

        public Builder patient(Patient patient) {
            this.patient = patient;
            return this;
        }

        public Builder status(CaseStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(CasePriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder intakeChannel(IntakeChannel intakeChannel) {
            this.intakeChannel = intakeChannel;
            return this;
        }

        public Builder chiefComplaint(String chiefComplaint) {
            this.chiefComplaint = chiefComplaint;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder geoLocation(GeoLocation geoLocation) {
            this.geoLocation = geoLocation;
            return this;
        }

        public Builder symptoms(Set<CaseSymptom> symptoms) {
            this.symptoms = symptoms;
            return this;
        }

        public HealthcareCase build() {
            return new HealthcareCase(id, caseNumber, patient, status, priority, intakeChannel,
                    chiefComplaint, notes, geoLocation, symptoms);
        }
    }
}
