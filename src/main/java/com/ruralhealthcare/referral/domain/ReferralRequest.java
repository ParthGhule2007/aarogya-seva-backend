package com.ruralhealthcare.referral.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_requests")
public class ReferralRequest extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "facility_id", nullable = false, updatable = false)
    private UUID facilityId;

    @Column(name = "ai_prediction_id", nullable = false, updatable = false)
    private UUID aiPredictionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "patient_message", columnDefinition = "TEXT")
    private String patientMessage;

    @Column(name = "facility_response", columnDefinition = "TEXT")
    private String facilityResponse;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ReferralRequest() {
    }

    public ReferralRequest(UUID id, UUID caseId, UUID patientId, UUID facilityId, UUID aiPredictionId,
                           ReferralStatus status, String patientMessage, String facilityResponse,
                           Instant requestedAt, Instant respondedAt, Instant acceptedAt, Instant completedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.caseId = caseId;
        this.patientId = patientId;
        this.facilityId = facilityId;
        this.aiPredictionId = aiPredictionId;
        this.status = status != null ? status : ReferralStatus.PENDING;
        this.patientMessage = patientMessage;
        this.facilityResponse = facilityResponse;
        this.requestedAt = requestedAt != null ? requestedAt : Instant.now();
        this.respondedAt = respondedAt;
        this.acceptedAt = acceptedAt;
        this.completedAt = completedAt;
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

    public static class Builder {
        private UUID id;
        private UUID caseId;
        private UUID patientId;
        private UUID facilityId;
        private UUID aiPredictionId;
        private ReferralStatus status = ReferralStatus.PENDING;
        private String patientMessage;
        private String facilityResponse;
        private Instant requestedAt;
        private Instant respondedAt;
        private Instant acceptedAt;
        private Instant completedAt;

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

        public Builder aiPredictionId(UUID aiPredictionId) {
            this.aiPredictionId = aiPredictionId;
            return this;
        }

        public Builder status(ReferralStatus status) {
            this.status = status;
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

        public ReferralRequest build() {
            return new ReferralRequest(id, caseId, patientId, facilityId, aiPredictionId,
                    status, patientMessage, facilityResponse, requestedAt, respondedAt, acceptedAt, completedAt);
        }
    }
}
