package com.ruralhealthcare.sos.dto;

import com.ruralhealthcare.sos.domain.UserType;
import java.time.Instant;
import java.util.UUID;

public class DeviceRegistrationResponse {

    private UUID deviceId;
    private UUID userId;
    private UserType userType;
    private String message;
    private boolean emergencyAlertOptIn;
    private boolean active;
    private Instant updatedAt;
    private Instant registeredAt;

    public DeviceRegistrationResponse() {
    }

    public DeviceRegistrationResponse(UUID deviceId, UUID userId, UserType userType, String message,
                                      boolean emergencyAlertOptIn, boolean active, Instant updatedAt, Instant registeredAt) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.userType = userType;
        this.message = message;
        this.emergencyAlertOptIn = emergencyAlertOptIn;
        this.active = active;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.registeredAt = registeredAt != null ? registeredAt : this.updatedAt;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getTokenId() {
        return deviceId;
    }

    public void setTokenId(UUID tokenId) {
        this.deviceId = tokenId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isEmergencyAlertOptIn() {
        return emergencyAlertOptIn;
    }

    public void setEmergencyAlertOptIn(boolean emergencyAlertOptIn) {
        this.emergencyAlertOptIn = emergencyAlertOptIn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getRegisteredAt() {
        return registeredAt != null ? registeredAt : updatedAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID deviceId;
        private UUID userId;
        private UserType userType;
        private String message;
        private boolean emergencyAlertOptIn;
        private boolean active = true;
        private Instant updatedAt;
        private Instant registeredAt;

        public Builder deviceId(UUID deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder tokenId(UUID tokenId) {
            this.deviceId = tokenId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder emergencyAlertOptIn(boolean emergencyAlertOptIn) {
            this.emergencyAlertOptIn = emergencyAlertOptIn;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public DeviceRegistrationResponse build() {
            return new DeviceRegistrationResponse(deviceId, userId, userType, message, emergencyAlertOptIn, active, updatedAt, registeredAt);
        }
    }
}
