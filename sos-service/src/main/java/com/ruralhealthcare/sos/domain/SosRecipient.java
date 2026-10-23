package com.ruralhealthcare.sos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sos_recipients")
public class SosRecipient {

    @Id
    private UUID id;

    @Column(name = "sos_event_id", nullable = false)
    private UUID sosEventId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 30)
    private RecipientType recipientType;

    @Column(name = "distance_km", columnDefinition = "NUMERIC(8, 2)")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 30)
    private RecipientNotificationStatus notificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    public SosRecipient() {
    }

    public SosRecipient(UUID id, UUID sosEventId, UUID recipientId, RecipientType recipientType,
                        Double distanceKm, RecipientNotificationStatus notificationStatus,
                        Instant createdAt, Instant notifiedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.sosEventId = sosEventId;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.distanceKm = distanceKm;
        this.notificationStatus = notificationStatus != null ? notificationStatus : RecipientNotificationStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.notifiedAt = notifiedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.notificationStatus == null) {
            this.notificationStatus = RecipientNotificationStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSosEventId() {
        return sosEventId;
    }

    public void setSosEventId(UUID sosEventId) {
        this.sosEventId = sosEventId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public RecipientNotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(RecipientNotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID sosEventId;
        private UUID recipientId;
        private RecipientType recipientType;
        private Double distanceKm;
        private RecipientNotificationStatus notificationStatus = RecipientNotificationStatus.PENDING;
        private Instant createdAt;
        private Instant notifiedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder sosEventId(UUID sosEventId) {
            this.sosEventId = sosEventId;
            return this;
        }

        public Builder sosEvent(SosEvent sosEvent) {
            if (sosEvent != null) {
                this.sosEventId = sosEvent.getId();
            }
            return this;
        }

        public Builder recipientId(UUID recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder recipientType(RecipientType recipientType) {
            this.recipientType = recipientType;
            return this;
        }

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public Builder notificationStatus(RecipientNotificationStatus notificationStatus) {
            this.notificationStatus = notificationStatus;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder notifiedAt(Instant notifiedAt) {
            this.notifiedAt = notifiedAt;
            return this;
        }

        public SosRecipient build() {
            return new SosRecipient(id, sosEventId, recipientId, recipientType, distanceKm,
                    notificationStatus, createdAt, notifiedAt);
        }
    }
}
