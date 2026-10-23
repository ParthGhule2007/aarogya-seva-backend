package com.ruralhealthcare.referral.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateReferralRequest {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    private UUID patientId;

    @NotNull(message = "Facility ID is required")
    private UUID facilityId;

    @Size(max = 2000, message = "Patient message must not exceed 2000 characters")
    private String patientMessage;

    public CreateReferralRequest() {
    }

    public CreateReferralRequest(UUID caseId, UUID facilityId, String patientMessage) {
        this(caseId, null, facilityId, patientMessage);
    }

    public CreateReferralRequest(UUID caseId, UUID patientId, UUID facilityId, String patientMessage) {
        this.caseId = caseId;
        this.patientId = patientId;
        this.facilityId = facilityId;
        this.patientMessage = patientMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getPatientMessage() {
        return patientMessage;
    }

    public void setPatientMessage(String patientMessage) {
        this.patientMessage = patientMessage;
    }

    public static class Builder {
        private UUID caseId;
        private UUID patientId;
        private UUID facilityId;
        private String patientMessage;

        public Builder caseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
            return this;
        }

        public Builder patientMessage(String patientMessage) {
            this.patientMessage = patientMessage;
            return this;
        }

        public CreateReferralRequest build() {
            return new CreateReferralRequest(caseId, patientId, facilityId, patientMessage);
        }
    }
}
