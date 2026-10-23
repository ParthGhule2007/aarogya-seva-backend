package com.ruralhealthcare.medicine.dto;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import com.ruralhealthcare.medicine.domain.MedicationStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class UpdateMedicationRequest {

    @Size(max = 255, message = "name cannot exceed 255 characters")
    private String name;

    @DecimalMin(value = "0.01", message = "dosageAmount must be greater than zero")
    private BigDecimal dosageAmount;

    private DosageUnit dosageUnit;

    @Min(value = 1, message = "timesPerDay must be at least 1")
    @Max(value = 24, message = "timesPerDay cannot exceed 24")
    private Integer timesPerDay;

    @io.swagger.v3.oas.annotations.media.ArraySchema(
            schema = @io.swagger.v3.oas.annotations.media.Schema(
                    type = "string",
                    format = "time",
                    example = "08:00:00",
                    description = "Dose reminder time in HH:mm:ss format"
            )
    )
    private List<LocalTime> reminderTimes;

    private LocalDate endDate;

    private FoodInstruction foodInstruction;

    @Size(max = 500, message = "instructions cannot exceed 500 characters")
    private String instructions;

    private MedicationStatus status;

    public UpdateMedicationRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.reminderTimes = reminderTimes;
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
        this.foodInstruction = foodInstruction;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public MedicationStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationStatus status) {
        this.status = status;
    }
}
