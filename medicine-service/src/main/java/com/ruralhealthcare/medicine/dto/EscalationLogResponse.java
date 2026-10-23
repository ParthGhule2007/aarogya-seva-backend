package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.DeliveryStatus;
import com.ruralhealthcare.medicine.domain.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public class EscalationLogResponse {

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

    public EscalationLogResponse() {
    }

    public EscalationLogResponse(UUID id, UUID patientId, UUID medicationId, String medicationName,
                                 UUID streakStartDoseId, UUID triggeringDoseId, int consecutiveMissedCount,
                                 NotificationChannel channel, String recipient, DeliveryStatus deliveryStatus,
                                 String messageContent, String providerReference,
                                 Instant deliveryAttemptedAt, Instant createdAt) {
        this.id = id;
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
        this.deliveryAttemptedAt = deliveryAttemptedAt;
        this.createdAt = createdAt;
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
}
