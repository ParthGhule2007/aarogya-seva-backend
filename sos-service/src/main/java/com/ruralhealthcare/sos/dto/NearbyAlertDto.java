package com.ruralhealthcare.sos.dto;

import java.time.Instant;
import java.util.UUID;

public class NearbyAlertDto {

    private UUID sosId;
    private UUID patientId;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String message;
    private Instant triggeredAt;
    private String status;

    public NearbyAlertDto() {
    }

    public NearbyAlertDto(UUID sosId, UUID patientId, Double latitude, Double longitude,
                          Double distanceKm, String message, Instant triggeredAt, String status) {
        this.sosId = sosId;
        this.patientId = patientId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.message = message;
        this.triggeredAt = triggeredAt;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getSosId() {
        return sosId;
    }

    public void setSosId(UUID sosId) {
        this.sosId = sosId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class Builder {
        private UUID sosId;
        private UUID patientId;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;
        private String message;
        private Instant triggeredAt;
        private String status;

        public Builder sosId(UUID sosId) {
            this.sosId = sosId;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
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

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder triggeredAt(Instant triggeredAt) {
            this.triggeredAt = triggeredAt;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public NearbyAlertDto build() {
            return new NearbyAlertDto(sosId, patientId, latitude, longitude, distanceKm, message, triggeredAt, status);
        }
    }
}
