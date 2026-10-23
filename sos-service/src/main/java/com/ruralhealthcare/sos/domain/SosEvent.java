package com.ruralhealthcare.sos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "sos_events")
public class SosEvent {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "latitude", nullable = false, columnDefinition = "NUMERIC(10, 7)")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "NUMERIC(10, 7)")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double longitude;

    @Column(name = "targets", nullable = false, length = 100)
    private String targets; // Stored as comma-separated values

    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SosEventStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public SosEvent() {
    }

    public SosEvent(UUID id, UUID patientId, Double latitude, Double longitude, List<TargetType> targetList,
                    UUID facilityId, String message, SosEventStatus status, String idempotencyKey,
                    Instant triggeredAt, Instant createdAt, Instant updatedAt, Instant resolvedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.patientId = patientId;
        this.latitude = latitude;
        this.longitude = longitude;
        setTargetList(targetList);
        this.facilityId = facilityId;
        this.message = message;
        this.status = status != null ? status : SosEventStatus.PROCESSING;
        this.idempotencyKey = idempotencyKey;
        this.triggeredAt = triggeredAt != null ? triggeredAt : Instant.now();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.resolvedAt = resolvedAt;
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
        if (this.triggeredAt == null) {
            this.triggeredAt = Instant.now();
        }
        if (this.status == null) {
            this.status = SosEventStatus.PROCESSING;
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

    public String getTargets() {
        return targets;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public List<TargetType> getTargetList() {
        if (this.targets == null || this.targets.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(this.targets.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TargetType::valueOf)
                .collect(Collectors.toList());
    }

    public List<TargetType> getTargetTypes() {
        return getTargetList();
    }

    public void setTargetList(List<TargetType> targetList) {
        if (targetList == null || targetList.isEmpty()) {
            this.targets = "";
        } else {
            this.targets = targetList.stream()
                    .map(TargetType::name)
                    .collect(Collectors.joining(","));
        }
    }

    public void setTargetTypes(List<TargetType> targetTypes) {
        setTargetList(targetTypes);
    }

    public boolean hasTarget(TargetType targetType) {
        return getTargetList().contains(targetType);
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

    public SosEventStatus getStatus() {
        return status;
    }

    public void setStatus(SosEventStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
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

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID patientId;
        private Double latitude;
        private Double longitude;
        private List<TargetType> targetList = new ArrayList<>();
        private UUID facilityId;
        private String message;
        private SosEventStatus status = SosEventStatus.PROCESSING;
        private String idempotencyKey;
        private Instant triggeredAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant resolvedAt;

        public Builder id(UUID id) {
            this.id = id;
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

        public Builder targetList(List<TargetType> targetList) {
            this.targetList = targetList;
            return this;
        }

        public Builder targetTypes(List<TargetType> targetTypes) {
            this.targetList = targetTypes;
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

        public Builder status(SosEventStatus status) {
            this.status = status;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder triggeredAt(Instant triggeredAt) {
            this.triggeredAt = triggeredAt;
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

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public SosEvent build() {
            return new SosEvent(id, patientId, latitude, longitude, targetList, facilityId,
                    message, status, idempotencyKey, triggeredAt, createdAt, updatedAt, resolvedAt);
        }
    }
}
