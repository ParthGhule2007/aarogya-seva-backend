package com.ruralhealthcare.sos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sos_device_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uq_device_user_token", columnNames = {"user_id", "fcm_token"})
})
public class DeviceToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(name = "emergency_alert_opt_in", nullable = false)
    private boolean emergencyAlertOptIn = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_latitude", columnDefinition = "NUMERIC(10, 7)")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double lastLatitude;

    @Column(name = "last_longitude", columnDefinition = "NUMERIC(10, 7)")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double lastLongitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DeviceToken() {
    }

    public DeviceToken(UUID id, UUID userId, UserType userType, String fcmToken,
                       boolean emergencyAlertOptIn, boolean active, Double lastLatitude,
                       Double lastLongitude, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId;
        this.userType = userType;
        this.fcmToken = fcmToken;
        this.emergencyAlertOptIn = emergencyAlertOptIn;
        this.active = active;
        this.lastLatitude = lastLatitude;
        this.lastLongitude = lastLongitude;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
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

    public Double getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(Double lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public Double getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(Double lastLongitude) {
        this.lastLongitude = lastLongitude;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private UserType userType;
        private String fcmToken;
        private boolean emergencyAlertOptIn = false;
        private boolean active = true;
        private Double lastLatitude;
        private Double lastLongitude;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
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

        public Builder fcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
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

        public Builder lastLatitude(Double lastLatitude) {
            this.lastLatitude = lastLatitude;
            return this;
        }

        public Builder lastLongitude(Double lastLongitude) {
            this.lastLongitude = lastLongitude;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DeviceToken build() {
            return new DeviceToken(id, userId, userType, fcmToken, emergencyAlertOptIn,
                    active, lastLatitude, lastLongitude, createdAt, updatedAt);
        }
    }
}
