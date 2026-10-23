package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CaregiverRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "relationship is required")
    @Size(max = 50, message = "relationship cannot exceed 50 characters")
    private String relationship;

    @Size(max = 30, message = "phoneNumber cannot exceed 30 characters")
    private String phoneNumber;

    @Size(max = 150, message = "email cannot exceed 150 characters")
    private String email;

    @NotNull(message = "preferredChannel is required")
    private NotificationChannel preferredChannel = NotificationChannel.WHATSAPP;

    public CaregiverRequest() {
    }

    public CaregiverRequest(UUID patientId, String name, String relationship, String phoneNumber, String email, NotificationChannel preferredChannel) {
        this.patientId = patientId;
        this.name = name;
        this.relationship = relationship;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.preferredChannel = preferredChannel != null ? preferredChannel : NotificationChannel.WHATSAPP;
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
}
