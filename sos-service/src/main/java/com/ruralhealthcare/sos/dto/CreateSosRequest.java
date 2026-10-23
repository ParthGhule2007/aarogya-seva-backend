package com.ruralhealthcare.sos.dto;

import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.exception.SosValidationException;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSosRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
    private Double longitude;

    @NotEmpty(message = "targets is required and must contain at least one target")
    private List<TargetType> targets;

    private UUID facilityId;

    @Size(max = 500, message = "message cannot exceed 500 characters")
    private String message;

    public CreateSosRequest() {
    }

    public CreateSosRequest(UUID patientId, Double latitude, Double longitude,
                            List<TargetType> targets, UUID facilityId, String message) {
        this.patientId = patientId;
        this.latitude = latitude;
        this.longitude = longitude;
        setTargets(targets);
        this.facilityId = facilityId;
        this.message = message;
    }

    public void normalizeAndValidate() {
        if (patientId == null) {
            throw new SosValidationException("patientId is required");
        }
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new SosValidationException("latitude must be between -90 and 90");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new SosValidationException("longitude must be between -180 and 180");
        }
        if (targets == null || targets.isEmpty()) {
            throw new SosValidationException("targets is required and must contain at least one target");
        }

        // Deduplicate and normalize targets
        this.targets = targets.stream()
                .filter(t -> t != null)
                .distinct()
                .collect(Collectors.toList());

        if (this.targets.isEmpty()) {
            throw new SosValidationException("targets is required and must contain at least one target");
        }

        // Rule 9: If FACILITY exists in targets, facilityId is REQUIRED
        if (this.targets.contains(TargetType.FACILITY) && facilityId == null) {
            throw new SosValidationException("facilityId is required when FACILITY target is selected");
        }

        // Rule 10: If FACILITY does not exist, facilityId must not affect processing
        if (!this.targets.contains(TargetType.FACILITY)) {
            this.facilityId = null;
        }

        // Limit message length
        if (message != null && message.length() > 500) {
            throw new SosValidationException("message cannot exceed 500 characters");
        }
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

    public List<TargetType> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetType> targets) {
        if (targets != null) {
            this.targets = targets.stream()
                    .filter(t -> t != null)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            this.targets = new ArrayList<>();
        }
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID patientId;
        private Double latitude;
        private Double longitude;
        private List<TargetType> targets = new ArrayList<>();
        private UUID facilityId;
        private String message;

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

        public Builder targets(List<TargetType> targets) {
            this.targets = targets;
            return this;
        }

        public Builder addTarget(TargetType target) {
            if (this.targets == null) {
                this.targets = new ArrayList<>();
            }
            if (target != null && !this.targets.contains(target)) {
                this.targets.add(target);
            }
            return this;
        }

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public CreateSosRequest build() {
            return new CreateSosRequest(patientId, latitude, longitude, targets, facilityId, message);
        }
    }
}
