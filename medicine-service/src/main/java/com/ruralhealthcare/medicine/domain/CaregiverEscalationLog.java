package com.ruralhealthcare.medicine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caregiver_escalation_logs", schema = "medicine")
public class CaregiverEscalationLog {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "medication_id", nullable = false)
    private UUID medicationId;

    @Column(name = "medication_name", nullable = false, length = 255)
    private String medicationName;

    @Column(name = "streak_start_dose_id", nullable = false)
    private UUID streakStartDoseId;

    @Column(name = "triggering_dose_id", nullable = false)
    private UUID triggeringDoseId;

    @Column(name = "consecutive_missed_count", nullable = false)
    private int consecutiveMissedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 150)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    private DeliveryStatus deliveryStatus;

    @Column(name = "message_content", nullable = false, length = 1000)
    private String messageContent;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "delivery_attempted_at", nullable = false)
    private Instant deliveryAttemptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CaregiverEscalationLog() {
    }

    public CaregiverEscalationLog(UUID id, UUID patientId, UUID medicationId, String medicationName,
                                  UUID streakStartDoseId, UUID triggeringDoseId, int consecutiveMissedCount,
                                  NotificationChannel channel, String recipient, DeliveryStatus deliveryStatus,
                                  String messageContent, String providerReference,
                                  Instant deliveryAttemptedAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.patientId = patientId;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.streakStartDoseId = streakStartDoseId;
        this.triggeringDoseId = triggeringDoseId;
        this.consecutiveMissedCount = consecutiveMissedCount;
        this.channel = channel;
        this.recipient = recipient;
        this.deliveryStatus = deliveryStatus;
        this.messageContent = messageContent;
        this.providerReference = providerReference;
        this.deliveryAttemptedAt = deliveryAttemptedAt != null ? deliveryAttemptedAt : Instant.now();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (deliveryAttemptedAt == null) {
            deliveryAttemptedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(UUID medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public UUID getStreakStartDoseId() {
        return streakStartDoseId;
    }

    public void setStreakStartDoseId(UUID streakStartDoseId) {
        this.streakStartDoseId = streakStartDoseId;
    }

    public UUID getTriggeringDoseId() {
        return triggeringDoseId;
    }

    public void setTriggeringDoseId(UUID triggeringDoseId) {
        this.triggeringDoseId = triggeringDoseId;
    }

    public int getConsecutiveMissedCount() {
        return consecutiveMissedCount;
    }

    public void setConsecutiveMissedCount(int consecutiveMissedCount) {
        this.consecutiveMissedCount = consecutiveMissedCount;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public Instant getDeliveryAttemptedAt() {
        return deliveryAttemptedAt;
    }

    public void setDeliveryAttemptedAt(Instant deliveryAttemptedAt) {
        this.deliveryAttemptedAt = deliveryAttemptedAt;
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
        private UUID patientId;
        private UUID medicationId;
        private String medicationName;
        private UUID streakStartDoseId;
        private UUID triggeringDoseId;
        private int consecutiveMissedCount;
        private NotificationChannel channel;
        private String recipient;
        private DeliveryStatus deliveryStatus;
        private String messageContent;
        private String providerReference;
        private Instant deliveryAttemptedAt;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder medicationId(UUID medicationId) {
            this.medicationId = medicationId;
            return this;
        }

        public Builder medicationName(String medicationName) {
            this.medicationName = medicationName;
            return this;
        }

        public Builder streakStartDoseId(UUID streakStartDoseId) {
            this.streakStartDoseId = streakStartDoseId;
            return this;
        }

        public Builder triggeringDoseId(UUID triggeringDoseId) {
            this.triggeringDoseId = triggeringDoseId;
            return this;
        }

        public Builder consecutiveMissedCount(int consecutiveMissedCount) {
            this.consecutiveMissedCount = consecutiveMissedCount;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder deliveryStatus(DeliveryStatus deliveryStatus) {
            this.deliveryStatus = deliveryStatus;
            return this;
        }

        public Builder messageContent(String messageContent) {
            this.messageContent = messageContent;
            return this;
        }

        public Builder providerReference(String providerReference) {
            this.providerReference = providerReference;
            return this;
        }

        public Builder deliveryAttemptedAt(Instant deliveryAttemptedAt) {
            this.deliveryAttemptedAt = deliveryAttemptedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CaregiverEscalationLog build() {
            return new CaregiverEscalationLog(id, patientId, medicationId, medicationName, streakStartDoseId,
                    triggeringDoseId, consecutiveMissedCount, channel, recipient, deliveryStatus,
                    messageContent, providerReference, deliveryAttemptedAt, createdAt);
        }
    }
}
