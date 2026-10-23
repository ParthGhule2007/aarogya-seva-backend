package com.ruralhealthcare.medicine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_reminder_times", schema = "medicine")
public class MedicationReminderTime {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MedicationReminderTime() {
    }

    public MedicationReminderTime(UUID id, Medication medication, LocalTime reminderTime, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.medication = medication;
        this.reminderTime = reminderTime;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Medication medication;
        private LocalTime reminderTime;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder medication(Medication medication) {
            this.medication = medication;
            return this;
        }

        public Builder reminderTime(LocalTime reminderTime) {
            this.reminderTime = reminderTime;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MedicationReminderTime build() {
            return new MedicationReminderTime(id, medication, reminderTime, createdAt);
        }
    }
}
