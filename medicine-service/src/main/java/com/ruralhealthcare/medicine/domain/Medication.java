package com.ruralhealthcare.medicine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medications", schema = "medicine")
public class Medication {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "extracted_name", length = 255)
    private String extractedName;

    @Column(name = "image_key", length = 512)
    private String imageKey;

    @Column(name = "dosage_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal dosageAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_unit", nullable = false, length = 50)
    private DosageUnit dosageUnit;

    @Column(name = "times_per_day", nullable = false)
    private Integer timesPerDay;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MedicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_instruction", nullable = false, length = 50)
    private FoodInstruction foodInstruction = FoodInstruction.NONE;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MedicationReminderTime> reminderTimes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Medication() {
    }

    public Medication(UUID id, UUID patientId, String name, String extractedName, String imageKey,
                      BigDecimal dosageAmount, DosageUnit dosageUnit, Integer timesPerDay,
                      LocalDate startDate, LocalDate endDate, MedicationStatus status,
                      FoodInstruction foodInstruction, String instructions,
                      List<MedicationReminderTime> reminderTimes, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.patientId = patientId;
        this.name = (name != null && !name.isBlank()) ? name : "Prescribed Medicine";
        this.extractedName = extractedName;
        this.imageKey = imageKey;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.timesPerDay = timesPerDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : MedicationStatus.ACTIVE;
        this.foodInstruction = foodInstruction != null ? foodInstruction : FoodInstruction.NONE;
        this.instructions = instructions;
        if (reminderTimes != null) {
            this.reminderTimes = reminderTimes;
        }
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (name == null || name.isBlank()) {
            name = "Prescribed Medicine";
        }
        if (status == null) {
            status = MedicationStatus.ACTIVE;
        }
        if (foodInstruction == null) {
            foodInstruction = FoodInstruction.NONE;
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

    public void addReminderTime(MedicationReminderTime time) {
        reminderTimes.add(time);
        time.setMedication(this);
    }

    public void removeReminderTime(MedicationReminderTime time) {
        reminderTimes.remove(time);
        time.setMedication(null);
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

    public List<MedicationReminderTime> getReminderTimes() {
        return reminderTimes;
    }

    public void setReminderTimes(List<MedicationReminderTime> reminderTimes) {
        this.reminderTimes = reminderTimes;
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
        private BigDecimal dosageAmount;
        private DosageUnit dosageUnit;
        private Integer timesPerDay;
        private LocalDate startDate;
        private LocalDate endDate;
        private MedicationStatus status = MedicationStatus.ACTIVE;
        private FoodInstruction foodInstruction = FoodInstruction.NONE;
        private String instructions;
        private List<MedicationReminderTime> reminderTimes = new ArrayList<>();
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

        public Builder reminderTimes(List<MedicationReminderTime> reminderTimes) {
            this.reminderTimes = reminderTimes;
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

        public Medication build() {
            return new Medication(id, patientId, name, extractedName, imageKey, dosageAmount, dosageUnit,
                    timesPerDay, startDate, endDate, status, foodInstruction, instructions, reminderTimes, createdAt, updatedAt);
        }
    }
}
