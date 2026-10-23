package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.DoseStatus;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class DoseEventResponse {

    private UUID id;
    private UUID medicationId;
    private String medicationName;
    private UUID patientId;
    private String imageKey;
    private String imageUrl;
    private BigDecimal dosageAmount;
    private DosageUnit dosageUnit;
    private FoodInstruction foodInstruction;
    private String instructions;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private DoseStatus status;
    private Instant takenAt;
    private Instant missedAt;
    private Instant createdAt;

    public DoseEventResponse() {
    }

    public DoseEventResponse(UUID id, UUID medicationId, String medicationName, UUID patientId, String imageKey, String imageUrl,
                             BigDecimal dosageAmount, DosageUnit dosageUnit, FoodInstruction foodInstruction, String instructions,
                             LocalDate scheduledDate, LocalTime scheduledTime, DoseStatus status,
                             Instant takenAt, Instant missedAt, Instant createdAt) {
        this.id = id;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.patientId = patientId;
        this.imageKey = imageKey;
        this.imageUrl = imageUrl;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.foodInstruction = foodInstruction != null ? foodInstruction : FoodInstruction.NONE;
        this.instructions = instructions;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.status = status;
        this.takenAt = takenAt;
        this.missedAt = missedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(UUID medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getDosageAmount() {
        return dosageAmount;
    }

    public void setDosageAmount(BigDecimal dosageAmount) {
        this.dosageAmount = dosageAmount;
    }

    public DosageUnit getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(DosageUnit dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public FoodInstruction getFoodInstruction() {
        return foodInstruction;
    }

    public void setFoodInstruction(FoodInstruction foodInstruction) {
        this.foodInstruction = foodInstruction;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
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
        private UUID medicationId;
        private String medicationName;
        private UUID patientId;
        private String imageKey;
        private String imageUrl;
        private BigDecimal dosageAmount;
        private DosageUnit dosageUnit;
        private FoodInstruction foodInstruction = FoodInstruction.NONE;
        private String instructions;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private DoseStatus status;
        private Instant takenAt;
        private Instant missedAt;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder medicationId(UUID medicationId) {
            this.medicationId = medicationId;
            return this;
        }

        public Builder medicationName(String medicationName) {
            this.medicationName = medicationName;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder imageKey(String imageKey) {
            this.imageKey = imageKey;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder dosageAmount(BigDecimal dosageAmount) {
            this.dosageAmount = dosageAmount;
            return this;
        }

        public Builder dosageUnit(DosageUnit dosageUnit) {
            this.dosageUnit = dosageUnit;
            return this;
        }

        public Builder foodInstruction(FoodInstruction foodInstruction) {
            this.foodInstruction = foodInstruction;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
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

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DoseEventResponse build() {
            return new DoseEventResponse(id, medicationId, medicationName, patientId, imageKey, imageUrl,
                    dosageAmount, dosageUnit, foodInstruction, instructions, scheduledDate, scheduledTime, status, takenAt, missedAt, createdAt);
        }
    }
}
