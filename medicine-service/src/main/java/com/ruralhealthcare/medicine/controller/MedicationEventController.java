package com.ruralhealthcare.medicine.controller;

import com.ruralhealthcare.medicine.dto.DoseEventResponse;
import com.ruralhealthcare.medicine.service.DoseEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medication-events")
@Tag(name = "Medication Events", description = "Endpoints for marking doses TAKEN or MISSED and tracking patient compliance")
public class MedicationEventController {

    private final DoseEventService doseEventService;

    public MedicationEventController(DoseEventService doseEventService) {
        this.doseEventService = doseEventService;
    }

    @PostMapping("/{eventId}/taken")
    @Operation(summary = "Mark dose as TAKEN",
            description = "Marks a scheduled dose as taken. Once taken, dose cannot automatically transition to missed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dose successfully marked as taken"),
            @ApiResponse(responseCode = "404", description = "Dose event not found")
    })
    public ResponseEntity<DoseEventResponse> markDoseTaken(
            @Parameter(description = "Dose Event UUID") @PathVariable UUID eventId) {
        DoseEventResponse response = doseEventService.markDoseTaken(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/missed")
    @Operation(summary = "Mark dose as MISSED",
            description = "Marks a scheduled dose as missed and updates consecutive missed count for caregiver escalation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dose marked as missed"),
            @ApiResponse(responseCode = "404", description = "Dose event not found"),
            @ApiResponse(responseCode = "409", description = "Cannot mark a taken dose as missed")
    })
    public ResponseEntity<DoseEventResponse> markDoseMissed(
            @Parameter(description = "Dose Event UUID") @PathVariable UUID eventId) {
        DoseEventResponse response = doseEventService.markDoseMissed(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/skip")
    @Operation(summary = "Mark dose as SKIPPED",
            description = "Intentionally skips a scheduled dose. Neutral towards consecutive missed streak.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dose marked as skipped"),
            @ApiResponse(responseCode = "404", description = "Dose event not found"),
            @ApiResponse(responseCode = "409", description = "Cannot skip a taken dose")
    })
    public ResponseEntity<DoseEventResponse> markDoseSkipped(
            @Parameter(description = "Dose Event UUID") @PathVariable UUID eventId) {
        DoseEventResponse response = doseEventService.markDoseSkipped(eventId);
        return ResponseEntity.ok(response);
    }
}
