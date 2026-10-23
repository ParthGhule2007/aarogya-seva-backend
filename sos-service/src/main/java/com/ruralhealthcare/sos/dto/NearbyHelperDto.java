package com.ruralhealthcare.sos.dto;

import com.ruralhealthcare.sos.domain.UserType;
import java.util.UUID;

public class NearbyHelperDto {

    private UUID userId;
    private UserType userType;
    private Double distanceKm;
    private boolean emergencyAlertOptIn = true;

    public NearbyHelperDto() {
    }

    public NearbyHelperDto(UUID userId, UserType userType, Double distanceKm, boolean emergencyAlertOptIn) {
        this.userId = userId;
        this.userType = userType;
        this.distanceKm = distanceKm;
        this.emergencyAlertOptIn = emergencyAlertOptIn;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isEmergencyAlertOptIn() {
        return emergencyAlertOptIn;
    }

    public void setEmergencyAlertOptIn(boolean emergencyAlertOptIn) {
        this.emergencyAlertOptIn = emergencyAlertOptIn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID userId;
        private UserType userType = UserType.PATIENT;
        private Double distanceKm;
        private boolean emergencyAlertOptIn = true;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }

        public Builder userType(String userType) {
            if (userType != null) {
                try {
                    this.userType = UserType.valueOf(userType.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    this.userType = UserType.PATIENT;
                }
            }
            return this;
        }

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public Builder emergencyAlertOptIn(boolean emergencyAlertOptIn) {
            this.emergencyAlertOptIn = emergencyAlertOptIn;
            return this;
        }

        public NearbyHelperDto build() {
            return new NearbyHelperDto(userId, userType, distanceKm, emergencyAlertOptIn);
        }
    }
}
