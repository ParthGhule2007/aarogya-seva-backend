package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public class CaregiverResponse {

    private UUID id;
    private UUID patientId;
    private String name;
    private String relationship;
    private String phoneNumber;
    private String email;
    private NotificationChannel preferredChannel;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public CaregiverResponse() {
    }

    public CaregiverResponse(UUID id, UUID patientId, String name, String relationship,
                             String phoneNumber, String email, NotificationChannel preferredChannel,
                             boolean isActive, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.relationship = relationship;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.preferredChannel = preferredChannel;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public NotificationChannel getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(NotificationChannel preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
}
