package com.ruralhealthcare.medicine.controller;

import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CaregiverResponse;
import com.ruralhealthcare.medicine.dto.EscalationLogResponse;
import com.ruralhealthcare.medicine.service.CaregiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caregivers")
@Tag(name = "Caregivers", description = "Endpoints for managing patient caregivers and reviewing missed-dose escalation audit history")
public class CaregiverController {

    private final CaregiverService caregiverService;

    public CaregiverController(CaregiverService caregiverService) {
        this.caregiverService = caregiverService;
    }

    @PostMapping
    @Operation(summary = "Register caregiver", description = "Registers a new caregiver contact with multi-channel notification preference.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Caregiver successfully registered"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<CaregiverResponse> createCaregiver(@Valid @RequestBody CaregiverRequest request) {
        CaregiverResponse response = caregiverService.createCaregiver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get caregivers for patient", description = "Lists all active caregivers associated with a patient.")
    public ResponseEntity<List<CaregiverResponse>> getCaregiversForPatient(
            @Parameter(description = "Patient UUID") @PathVariable UUID patientId) {
        List<CaregiverResponse> responses = caregiverService.getCaregiversForPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update caregiver details", description = "Updates contact details or channel preference for a caregiver.")
    public ResponseEntity<CaregiverResponse> updateCaregiver(
            @Parameter(description = "Caregiver UUID") @PathVariable UUID id,
            @RequestBody CaregiverRequest request) {
        CaregiverResponse response = caregiverService.updateCaregiver(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate caregiver", description = "Deactivates a caregiver association.")
    public ResponseEntity<Void> deactivateCaregiver(
            @Parameter(description = "Caregiver UUID") @PathVariable UUID id) {
        caregiverService.deactivateCaregiver(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/escalations/patient/{patientId}")
    @Operation(summary = "Get caregiver escalation audit logs",
            description = "Retrieves complete, immutable escalation audit history for consecutive missed doses of a patient.")
    public ResponseEntity<List<EscalationLogResponse>> getEscalationHistory(
            @Parameter(description = "Patient UUID") @PathVariable UUID patientId) {
        List<EscalationLogResponse> responses = caregiverService.getEscalationHistoryForPatient(patientId);
        return ResponseEntity.ok(responses);
    }
}
