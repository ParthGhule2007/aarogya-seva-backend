package com.ruralhealthcare.sos.dto;

import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.TargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SosResponse {

    private UUID sosId;
    private SosEventStatus status;
    private Instant triggeredAt;
    private String message;
    private List<TargetType> targets;

    public SosResponse() {
    }

    public SosResponse(UUID sosId, SosEventStatus status, Instant triggeredAt, String message, List<TargetType> targets) {
        this.sosId = sosId;
        this.status = status;
        this.triggeredAt = triggeredAt;
        this.message = message;
        this.targets = targets;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TargetType> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetType> targets) {
        this.targets = targets;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID sosId;
        private SosEventStatus status;
        private Instant triggeredAt;
        private String message;
        private List<TargetType> targets;

        public Builder sosId(UUID sosId) {
            this.sosId = sosId;
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

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder targets(List<TargetType> targets) {
            this.targets = targets;
            return this;
        }

        public SosResponse build() {
            return new SosResponse(sosId, status, triggeredAt, message, targets);
        }
    }
}
