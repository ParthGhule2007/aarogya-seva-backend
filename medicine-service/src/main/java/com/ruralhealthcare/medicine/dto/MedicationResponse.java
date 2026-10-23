package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import com.ruralhealthcare.medicine.domain.MedicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MedicationResponse {

    private UUID id;
    private UUID patientId;
    private String name;
    private String extractedName;
    private String imageKey;
    private String imageUrl;
    private BigDecimal dosageAmount;
    private DosageUnit dosageUnit;
    private Integer timesPerDay;
    private List<LocalTime> reminderTimes = new ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private MedicationStatus status;
    private FoodInstruction foodInstruction;
    private String instructions;
    private Instant createdAt;
    private Instant updatedAt;

    public MedicationResponse() {
    }

    public MedicationResponse(UUID id, UUID patientId, String name, String extractedName, String imageKey, String imageUrl,
                              BigDecimal dosageAmount, DosageUnit dosageUnit, Integer timesPerDay,
                              List<LocalTime> reminderTimes, LocalDate startDate, LocalDate endDate,
                              MedicationStatus status, FoodInstruction foodInstruction, String instructions,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.extractedName = extractedName;
        this.imageKey = imageKey;
        this.imageUrl = imageUrl;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.timesPerDay = timesPerDay;
        this.reminderTimes = reminderTimes != null ? reminderTimes : new ArrayList<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.foodInstruction = foodInstruction != null ? foodInstruction : FoodInstruction.NONE;
        this.instructions = instructions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtractedName() {
        return extractedName;
    }

    public void setExtractedName(String extractedName) {
        this.extractedName = extractedName;
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

    public Integer getTimesPerDay() {
        return timesPerDay;
    }

    public void setTimesPerDay(Integer timesPerDay) {
        this.timesPerDay = timesPerDay;
    }

    public List<LocalTime> getReminderTimes() {
        return reminderTimes;
    }

    public void setReminderTimes(List<LocalTime> reminderTimes) {
        this.reminderTimes = reminderTimes != null ? reminderTimes : new ArrayList<>();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public MedicationStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationStatus status) {
        this.status = status;
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
        private UUID patientId;
        private String name;
        private String extractedName;
        private String imageKey;
        private String imageUrl;
        private BigDecimal dosageAmount;
        private DosageUnit dosageUnit;
        private Integer timesPerDay;
        private List<LocalTime> reminderTimes = new ArrayList<>();
        private LocalDate startDate;
        private LocalDate endDate;
        private MedicationStatus status;
        private FoodInstruction foodInstruction = FoodInstruction.NONE;
        private String instructions;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder extractedName(String extractedName) {
            this.extractedName = extractedName;
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

        public Builder timesPerDay(Integer timesPerDay) {
            this.timesPerDay = timesPerDay;
            return this;
        }

        public Builder reminderTimes(List<LocalTime> reminderTimes) {
            this.reminderTimes = reminderTimes;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder status(MedicationStatus status) {
            this.status = status;
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

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MedicationResponse build() {
            return new MedicationResponse(id, patientId, name, extractedName, imageKey, imageUrl, dosageAmount,
                    dosageUnit, timesPerDay, reminderTimes, startDate, endDate, status, foodInstruction, instructions, createdAt, updatedAt);
        }
    }
}
