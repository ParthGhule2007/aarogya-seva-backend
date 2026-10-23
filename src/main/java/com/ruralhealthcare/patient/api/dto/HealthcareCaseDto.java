package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.IntakeChannel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class HealthcareCaseDto {

    private UUID id;
    private String caseNumber;
    private UUID patientId;
    private String patientName;
    private String patientContactNumber;
    private CaseStatus status;
    private CasePriority priority;
    private IntakeChannel intakeChannel;
    private String intakeChannelDescription;
    private String chiefComplaint;
    private String notes;
    private Double latitude;
    private Double longitude;
    private List<CaseSymptomDto> symptoms;
    private Instant createdAt;
    private Instant updatedAt;

    public HealthcareCaseDto() {
    }

    public HealthcareCaseDto(UUID id, String caseNumber, UUID patientId, String patientName,
                             String patientContactNumber, CaseStatus status, CasePriority priority,
                             IntakeChannel intakeChannel, String intakeChannelDescription,
                             String chiefComplaint, String notes, Double latitude, Double longitude,
                             List<CaseSymptomDto> symptoms, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientContactNumber = patientContactNumber;
        this.status = status;
        this.priority = priority;
        this.intakeChannel = intakeChannel;
        this.intakeChannelDescription = intakeChannelDescription;
        this.chiefComplaint = chiefComplaint;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.symptoms = symptoms;
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientContactNumber() {
        return patientContactNumber;
    }

    public void setPatientContactNumber(String patientContactNumber) {
        this.patientContactNumber = patientContactNumber;
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

    public String getIntakeChannelDescription() {
        return intakeChannelDescription;
    }

    public void setIntakeChannelDescription(String intakeChannelDescription) {
        this.intakeChannelDescription = intakeChannelDescription;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<CaseSymptomDto> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<CaseSymptomDto> symptoms) {
        this.symptoms = symptoms;
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
        private String caseNumber;
        private UUID patientId;
        private String patientName;
        private String patientContactNumber;
        private CaseStatus status;
        private CasePriority priority;
        private IntakeChannel intakeChannel;
        private String intakeChannelDescription;
        private String chiefComplaint;
        private String notes;
        private Double latitude;
        private Double longitude;
        private List<CaseSymptomDto> symptoms;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder caseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder patientName(String patientName) {
            this.patientName = patientName;
            return this;
        }

        public Builder patientContactNumber(String patientContactNumber) {
            this.patientContactNumber = patientContactNumber;
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

        public Builder intakeChannelDescription(String intakeChannelDescription) {
            this.intakeChannelDescription = intakeChannelDescription;
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

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder symptoms(List<CaseSymptomDto> symptoms) {
            this.symptoms = symptoms;
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

        public HealthcareCaseDto build() {
            return new HealthcareCaseDto(id, caseNumber, patientId, patientName, patientContactNumber,
                    status, priority, intakeChannel, intakeChannelDescription, chiefComplaint,
                    notes, latitude, longitude, symptoms, createdAt, updatedAt);
        }
    }
}
