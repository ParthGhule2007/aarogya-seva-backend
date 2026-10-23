package com.ruralhealthcare.medicine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "dose_events", schema = "medicine")
public class DoseEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DoseStatus status;

    @Column(name = "taken_at")
    private Instant takenAt;

    @Column(name = "missed_at")
    private Instant missedAt;

    @Column(name = "notification_triggered", nullable = false)
    private boolean notificationTriggered;

    @Column(name = "notification_time")
    private Instant notificationTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DoseEvent() {
    }

    public DoseEvent(UUID id, Medication medication, UUID patientId, LocalDate scheduledDate,
                     LocalTime scheduledTime, DoseStatus status, Instant takenAt, Instant missedAt,
                     boolean notificationTriggered, Instant notificationTime,
                     Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.medication = medication;
        this.patientId = patientId;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.status = status != null ? status : DoseStatus.PENDING;
        this.takenAt = takenAt;
        this.missedAt = missedAt;
        this.notificationTriggered = notificationTriggered;
        this.notificationTime = notificationTime;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = DoseStatus.PENDING;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markTaken() {
        this.status = DoseStatus.TAKEN;
        this.takenAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markMissed() {
        this.status = DoseStatus.MISSED;
        this.missedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markSkipped() {
        this.status = DoseStatus.SKIPPED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Instant takenAt) {
        this.takenAt = takenAt;
    }

    public Instant getMissedAt() {
        return missedAt;
    }

    public void setMissedAt(Instant missedAt) {
        this.missedAt = missedAt;
    }

    public boolean isNotificationTriggered() {
        return notificationTriggered;
    }

    public void setNotificationTriggered(boolean notificationTriggered) {
        this.notificationTriggered = notificationTriggered;
    }

    public Instant getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(Instant notificationTime) {
        this.notificationTime = notificationTime;
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
        private Medication medication;
        private UUID patientId;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private DoseStatus status = DoseStatus.PENDING;
        private Instant takenAt;
        private Instant missedAt;
        private boolean notificationTriggered;
        private Instant notificationTime;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder medication(Medication medication) {
            this.medication = medication;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder scheduledDate(LocalDate scheduledDate) {
            this.scheduledDate = scheduledDate;
            return this;
        }

        public Builder scheduledTime(LocalTime scheduledTime) {
            this.scheduledTime = scheduledTime;
            return this;
        }

        public Builder status(DoseStatus status) {
            this.status = status;
            return this;
        }

        public Builder takenAt(Instant takenAt) {
            this.takenAt = takenAt;
            return this;
        }

        public Builder missedAt(Instant missedAt) {
            this.missedAt = missedAt;
            return this;
        }

        public Builder notificationTriggered(boolean notificationTriggered) {
            this.notificationTriggered = notificationTriggered;
            return this;
        }

        public Builder notificationTime(Instant notificationTime) {
            this.notificationTime = notificationTime;
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

        public DoseEvent build() {
            return new DoseEvent(id, medication, patientId, scheduledDate, scheduledTime,
                    status, takenAt, missedAt, notificationTriggered, notificationTime, createdAt, updatedAt);
        }
    }
}
