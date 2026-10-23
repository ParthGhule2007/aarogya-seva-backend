package com.ruralhealthcare.referral.api.dto;

import com.ruralhealthcare.referral.domain.ReferralStatus;

import java.time.Instant;
import java.util.UUID;

public class ReferralResponseDto {

    private UUID id;
    private UUID caseId;
    private UUID patientId;
    private UUID facilityId;
    private String facilityName;
    private String facilityType;
    private UUID aiPredictionId;
    private ReferralStatus status;
    private String statusDisplayName;
    private String patientMessage;
    private String facilityResponse;
    private Instant requestedAt;
    private Instant respondedAt;
    private Instant acceptedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public ReferralResponseDto() {
    }

    public ReferralResponseDto(UUID id, UUID caseId, UUID patientId, UUID facilityId, String facilityName,
                               String facilityType, UUID aiPredictionId, ReferralStatus status,
                               String statusDisplayName, String patientMessage, String facilityResponse,
                               Instant requestedAt, Instant respondedAt, Instant acceptedAt, Instant completedAt,
                               Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.caseId = caseId;
        this.patientId = patientId;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.aiPredictionId = aiPredictionId;
        this.status = status;
        this.statusDisplayName = statusDisplayName;
        this.patientMessage = patientMessage;
        this.facilityResponse = facilityResponse;
        this.requestedAt = requestedAt;
        this.respondedAt = respondedAt;
        this.acceptedAt = acceptedAt;
        this.completedAt = completedAt;
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

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public UUID getAiPredictionId() {
        return aiPredictionId;
    }

    public void setAiPredictionId(UUID aiPredictionId) {
        this.aiPredictionId = aiPredictionId;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public String getStatusDisplayName() {
        return statusDisplayName;
    }

    public void setStatusDisplayName(String statusDisplayName) {
        this.statusDisplayName = statusDisplayName;
    }

    public String getPatientMessage() {
        return patientMessage;
    }

    public void setPatientMessage(String patientMessage) {
        this.patientMessage = patientMessage;
    }

    public String getFacilityResponse() {
        return facilityResponse;
    }

    public void setFacilityResponse(String facilityResponse) {
        this.facilityResponse = facilityResponse;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
        private UUID patientId;
        private UUID facilityId;
        private String facilityName;
        private String facilityType;
        private UUID aiPredictionId;
        private ReferralStatus status;
        private String statusDisplayName;
        private String patientMessage;
        private String facilityResponse;
        private Instant requestedAt;
        private Instant respondedAt;
        private Instant acceptedAt;
        private Instant completedAt;
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

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
            return this;
        }

        public Builder facilityName(String facilityName) {
            this.facilityName = facilityName;
            return this;
        }

        public Builder facilityType(String facilityType) {
            this.facilityType = facilityType;
            return this;
        }

        public Builder aiPredictionId(UUID aiPredictionId) {
            this.aiPredictionId = aiPredictionId;
            return this;
        }

        public Builder status(ReferralStatus status) {
            this.status = status;
            return this;
        }

        public Builder statusDisplayName(String statusDisplayName) {
            this.statusDisplayName = statusDisplayName;
            return this;
        }

        public Builder patientMessage(String patientMessage) {
            this.patientMessage = patientMessage;
            return this;
        }

        public Builder facilityResponse(String facilityResponse) {
            this.facilityResponse = facilityResponse;
            return this;
        }

        public Builder requestedAt(Instant requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public Builder respondedAt(Instant respondedAt) {
            this.respondedAt = respondedAt;
            return this;
        }

        public Builder acceptedAt(Instant acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
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

        public ReferralResponseDto build() {
            return new ReferralResponseDto(id, caseId, patientId, facilityId, facilityName,
                    facilityType, aiPredictionId, status, statusDisplayName, patientMessage, facilityResponse,
                    requestedAt, respondedAt, acceptedAt, completedAt, createdAt, updatedAt);
        }
    }
}
