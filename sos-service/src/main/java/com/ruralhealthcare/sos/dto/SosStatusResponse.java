package com.ruralhealthcare.sos.dto;

import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.TargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SosStatusResponse {

    private UUID sosId;
    private UUID patientId;
    private Double latitude;
    private Double longitude;
    private UUID facilityId;
    private String message;
    private SosEventStatus status;
    private Instant triggeredAt;
    private Instant resolvedAt;
    private List<TargetType> targets;
    private int totalRecipients;
    private int successfulNotifications;
    private int failedNotifications;

    public SosStatusResponse() {
    }

    public SosStatusResponse(UUID sosId, SosEventStatus status, Instant triggeredAt, Instant resolvedAt,
                             List<TargetType> targets, int totalRecipients, int successfulNotifications, int failedNotifications) {
        this.sosId = sosId;
        this.status = status;
        this.triggeredAt = triggeredAt;
        this.resolvedAt = resolvedAt;
        this.targets = targets;
        this.totalRecipients = totalRecipients;
        this.successfulNotifications = successfulNotifications;
        this.failedNotifications = failedNotifications;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
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

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getSosId() {
        return sosId;
    }

    public void setSosId(UUID sosId) {
        this.sosId = sosId;
    }

    public SosEventStatus getStatus() {
        return status;
    }

    public void setStatus(SosEventStatus status) {
        this.status = status;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<TargetType> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetType> targets) {
        this.targets = targets;
    }

    public int getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(int totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public int getSuccessfulNotifications() {
        return successfulNotifications;
    }

    public void setSuccessfulNotifications(int successfulNotifications) {
        this.successfulNotifications = successfulNotifications;
    }

    public int getRecipientsNotified() {
        return successfulNotifications;
    }

    public void setRecipientsNotified(int recipientsNotified) {
        this.successfulNotifications = recipientsNotified;
    }

    public int getFailedNotifications() {
        return failedNotifications;
    }

    public void setFailedNotifications(int failedNotifications) {
        this.failedNotifications = failedNotifications;
    }

    public int getRecipientsFailed() {
        return failedNotifications;
    }

    public void setRecipientsFailed(int recipientsFailed) {
        this.failedNotifications = recipientsFailed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID sosId;
        private UUID patientId;
        private Double latitude;
        private Double longitude;
        private UUID facilityId;
        private String message;
        private SosEventStatus status;
        private Instant triggeredAt;
        private Instant resolvedAt;
        private List<TargetType> targets;
        private int totalRecipients;
        private int successfulNotifications;
        private int failedNotifications;

        public Builder sosId(UUID sosId) {
            this.sosId = sosId;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
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

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder status(SosEventStatus status) {
            this.status = status;
            return this;
        }

        public Builder triggeredAt(Instant triggeredAt) {
            this.triggeredAt = triggeredAt;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder targets(List<TargetType> targets) {
            this.targets = targets;
            return this;
        }

        public Builder totalRecipients(int totalRecipients) {
            this.totalRecipients = totalRecipients;
            return this;
        }

        public Builder totalRecipients(long totalRecipients) {
            this.totalRecipients = (int) totalRecipients;
            return this;
        }

        public Builder successfulNotifications(int successfulNotifications) {
            this.successfulNotifications = successfulNotifications;
            return this;
        }

        public Builder successfulNotifications(long successfulNotifications) {
            this.successfulNotifications = (int) successfulNotifications;
            return this;
        }

        public Builder recipientsNotified(int recipientsNotified) {
            this.successfulNotifications = recipientsNotified;
            return this;
        }

        public Builder recipientsNotified(long recipientsNotified) {
            this.successfulNotifications = (int) recipientsNotified;
            return this;
        }

        public Builder failedNotifications(int failedNotifications) {
            this.failedNotifications = failedNotifications;
            return this;
        }

        public Builder failedNotifications(long failedNotifications) {
            this.failedNotifications = (int) failedNotifications;
            return this;
        }

        public Builder recipientsFailed(int recipientsFailed) {
            this.failedNotifications = recipientsFailed;
            return this;
        }

        public Builder recipientsFailed(long recipientsFailed) {
            this.failedNotifications = (int) recipientsFailed;
            return this;
        }

        public SosStatusResponse build() {
            SosStatusResponse res = new SosStatusResponse(sosId, status, triggeredAt, resolvedAt, targets,
                    totalRecipients, successfulNotifications, failedNotifications);
            res.setPatientId(patientId);
            res.setLatitude(latitude);
            res.setLongitude(longitude);
            res.setFacilityId(facilityId);
            res.setMessage(message);
            return res;
        }
    }
}
