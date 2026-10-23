package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateMedicationRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @Size(max = 255, message = "name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "extractedName cannot exceed 255 characters")
    private String extractedName;

    @NotNull(message = "dosageAmount is required")
    @DecimalMin(value = "0.01", message = "dosageAmount must be greater than zero")
    private BigDecimal dosageAmount;

    @NotNull(message = "dosageUnit is required")
    private DosageUnit dosageUnit;

    @NotNull(message = "timesPerDay is required")
    @Min(value = 1, message = "timesPerDay must be at least 1")
    @Max(value = 24, message = "timesPerDay cannot exceed 24")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Number of dose times per day (e.g. 1, 2, 3)", example = "2")
    private Integer timesPerDay;

    @NotEmpty(message = "reminderTimes must contain at least one time")
    @io.swagger.v3.oas.annotations.media.ArraySchema(
            schema = @io.swagger.v3.oas.annotations.media.Schema(
                    type = "string",
                    format = "time",
                    example = "08:00:00",
                    description = "Dose reminder time in HH:mm:ss format"
            )
    )
    private List<LocalTime> reminderTimes = new ArrayList<>();

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private FoodInstruction foodInstruction = FoodInstruction.NONE;

    @Size(max = 500, message = "instructions cannot exceed 500 characters")
    private String instructions;

    public CreateMedicationRequest() {
    }

    // Legacy constructor for backward compatibility in tests and clients
    public CreateMedicationRequest(UUID patientId, BigDecimal dosageAmount, DosageUnit dosageUnit,
                                   Integer timesPerDay, List<LocalTime> reminderTimes,
                                   LocalDate startDate, LocalDate endDate, String instructions) {
        this(patientId, null, null, dosageAmount, dosageUnit, timesPerDay, reminderTimes, startDate, endDate, FoodInstruction.NONE, instructions);
    }

    public CreateMedicationRequest(UUID patientId, String name, String extractedName,
                                   BigDecimal dosageAmount, DosageUnit dosageUnit,
                                   Integer timesPerDay, List<LocalTime> reminderTimes,
                                   LocalDate startDate, LocalDate endDate,
                                   FoodInstruction foodInstruction, String instructions) {
        this.patientId = patientId;
        this.name = name;
        this.extractedName = extractedName;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.timesPerDay = timesPerDay;
        this.reminderTimes = reminderTimes != null ? reminderTimes : new ArrayList<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.foodInstruction = foodInstruction != null ? foodInstruction : FoodInstruction.NONE;
        this.instructions = instructions;
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

    public FoodInstruction getFoodInstruction() {
        return foodInstruction;
    }

    public void setFoodInstruction(FoodInstruction foodInstruction) {
        this.foodInstruction = foodInstruction != null ? foodInstruction : FoodInstruction.NONE;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
