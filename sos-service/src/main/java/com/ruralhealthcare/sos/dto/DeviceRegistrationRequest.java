package com.ruralhealthcare.sos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.ruralhealthcare.sos.domain.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceRegistrationRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "fcmToken is required")
    private String fcmToken;

    @NotNull(message = "userType is required")
    private UserType userType;

    private boolean emergencyAlertOptIn = false;

    private Double latitude;

    private Double longitude;

    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(UUID userId, String fcmToken, UserType userType,
                                     boolean emergencyAlertOptIn, Double latitude, Double longitude) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.userType = userType;
        this.emergencyAlertOptIn = emergencyAlertOptIn;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @JsonSetter("userId")
    public void setUserId(Object val) {
        if (val == null) {
            this.userId = null;
            return;
        }
        String str = val.toString().trim();
        try {
            this.userId = UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            this.userId = UUID.nameUUIDFromBytes(("AAROGYA-USER-" + str).getBytes(StandardCharsets.UTF_8));
        }
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    @JsonSetter("userType")
    public void setUserType(Object val) {
        if (val == null) {
            this.userType = UserType.PATIENT;
            return;
        }
        try {
            this.userType = UserType.valueOf(val.toString().trim().toUpperCase());
        } catch (Exception e) {
            this.userType = UserType.PATIENT;
        }
    }

    public boolean isEmergencyAlertOptIn() {
        return emergencyAlertOptIn;
    }

    public Boolean getEmergencyAlertOptIn() {
        return emergencyAlertOptIn;
    }

    public void setEmergencyAlertOptIn(boolean emergencyAlertOptIn) {
        this.emergencyAlertOptIn = emergencyAlertOptIn;
    }

    public void setEmergencyAlertOptIn(Boolean emergencyAlertOptIn) {
        this.emergencyAlertOptIn = emergencyAlertOptIn != null ? emergencyAlertOptIn : false;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID userId;
        private String fcmToken;
        private UserType userType;
        private boolean emergencyAlertOptIn = false;
        private Double latitude;
        private Double longitude;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder fcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
            return this;
        }

        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }

        public Builder emergencyAlertOptIn(boolean emergencyAlertOptIn) {
            this.emergencyAlertOptIn = emergencyAlertOptIn;
            return this;
        }

        public Builder emergencyAlertOptIn(Boolean emergencyAlertOptIn) {
            this.emergencyAlertOptIn = emergencyAlertOptIn != null ? emergencyAlertOptIn : false;
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

        public DeviceRegistrationRequest build() {
            return new DeviceRegistrationRequest(userId, fcmToken, userType, emergencyAlertOptIn, latitude, longitude);
        }
    }
}
