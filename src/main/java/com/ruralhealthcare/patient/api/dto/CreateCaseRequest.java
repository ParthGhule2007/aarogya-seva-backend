package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.IntakeChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class CreateCaseRequest {

    @NotNull(message = "Patient ID is mandatory")
    private UUID patientId;

    @NotNull(message = "Intake channel is mandatory")
    private IntakeChannel intakeChannel;

    @Size(max = 255, message = "Chief complaint cannot exceed 255 characters")
    private String chiefComplaint;

    private String notes;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    @Valid
    private List<SymptomInputDto> initialSymptoms;

    public CreateCaseRequest() {
    }

    public CreateCaseRequest(UUID patientId, IntakeChannel intakeChannel, String chiefComplaint,
                             String notes, Double latitude, Double longitude, List<SymptomInputDto> initialSymptoms) {
        this.patientId = patientId;
        this.intakeChannel = intakeChannel;
        this.chiefComplaint = chiefComplaint;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.initialSymptoms = initialSymptoms;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
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

    public List<SymptomInputDto> getInitialSymptoms() {
        return initialSymptoms;
    }

    public void setInitialSymptoms(List<SymptomInputDto> initialSymptoms) {
        this.initialSymptoms = initialSymptoms;
    }

    public static class Builder {
        private UUID patientId;
        private IntakeChannel intakeChannel;
        private String chiefComplaint;
        private String notes;
        private Double latitude;
        private Double longitude;
        private List<SymptomInputDto> initialSymptoms;

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
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

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder initialSymptoms(List<SymptomInputDto> initialSymptoms) {
            this.initialSymptoms = initialSymptoms;
            return this;
        }

        public CreateCaseRequest build() {
            return new CreateCaseRequest(patientId, intakeChannel, chiefComplaint, notes, latitude, longitude, initialSymptoms);
        }
    }
}
