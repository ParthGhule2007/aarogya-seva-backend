package com.ruralhealthcare.sos.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sos_notifications")
public class SosNotification {

    @Id
    private UUID id;

    @Column(name = "sos_event_id", nullable = false)
    private UUID sosEventId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel = "FCM";

    @Column(name = "fcm_message_id", length = 255)
    private String fcmMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SosNotification() {
    }

    public SosNotification(UUID id, UUID sosEventId, UUID recipientId, String channel,
                           String fcmMessageId, NotificationStatus status, String failureReason,
                           Instant sentAt, Instant deliveredAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.sosEventId = sosEventId;
        this.recipientId = recipientId;
        this.channel = channel != null ? channel : "FCM";
        this.fcmMessageId = fcmMessageId;
        this.status = status != null ? status : NotificationStatus.PENDING;
        this.failureReason = failureReason;
        this.sentAt = sentAt;
        this.deliveredAt = deliveredAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.channel == null) {
            this.channel = "FCM";
        }
        if (this.status == null) {
            this.status = NotificationStatus.PENDING;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getFcmMessageId() {
        return fcmMessageId;
    }

    public void setFcmMessageId(String fcmMessageId) {
        this.fcmMessageId = fcmMessageId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID sosEventId;
        private UUID recipientId;
        private String channel = "FCM";
        private String fcmMessageId;
        private NotificationStatus status = NotificationStatus.PENDING;
        private String failureReason;
        private Instant sentAt;
        private Instant deliveredAt;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder sosEventId(UUID sosEventId) {
            this.sosEventId = sosEventId;
            return this;
        }

        public Builder sosEvent(SosEvent event) {
            if (event != null) {
                this.sosEventId = event.getId();
            }
            return this;
        }

        public Builder recipientId(UUID recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder recipient(SosRecipient recipient) {
            if (recipient != null) {
                this.recipientId = recipient.getId();
            }
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder fcmMessageId(String fcmMessageId) {
            this.fcmMessageId = fcmMessageId;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder sentAt(Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder deliveredAt(Instant deliveredAt) {
            this.deliveredAt = deliveredAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SosNotification build() {
            return new SosNotification(id, sosEventId, recipientId, channel, fcmMessageId,
                    status, failureReason, sentAt, deliveredAt, createdAt);
        }
    }
}
