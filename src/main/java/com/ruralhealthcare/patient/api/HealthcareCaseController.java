package com.ruralhealthcare.patient.api;

import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.RecordSymptomsRequest;
import com.ruralhealthcare.patient.api.dto.UpdateCaseStatusRequest;
import com.ruralhealthcare.patient.service.HealthcareCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@Validated
@Tag(name = "Healthcare Cases", description = "Endpoints for consultation cases, intake channels, symptom recording, and case lifecycles")
public class HealthcareCaseController {

    private final HealthcareCaseService caseService;

    public HealthcareCaseController(HealthcareCaseService caseService) {
        this.caseService = caseService;
    }

    @Operation(summary = "List all healthcare cases", description = "Retrieves all consultation and intake cases")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthcareCaseDto>>> getAllCases() {
        return ResponseEntity.ok(ApiResponse.ok(caseService.getAllCases()));
    }

    @Operation(summary = "Open a new healthcare case", description = "Creates a consultation session for a patient with GPS location and intake modality")
    @PostMapping
    public ResponseEntity<ApiResponse<HealthcareCaseDto>> createCase(@Valid @RequestBody CreateCaseRequest request) {
        HealthcareCaseDto created = caseService.createCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Healthcare case opened successfully", created));
    }

    @Operation(summary = "Get case details by ID", description = "Retrieves case information including patient summary and recorded symptoms")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareCaseDto>> getCaseById(@PathVariable UUID id) {
        return caseService.getCaseById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<HealthcareCaseDto>builder()
                                .success(false)
                                .message("Healthcare case not found with ID: " + id)
                                .build()));
    }

    @Operation(summary = "Get case by unique case number")
    @GetMapping("/by-number/{caseNumber}")
    public ResponseEntity<ApiResponse<HealthcareCaseDto>> getCaseByNumber(@PathVariable String caseNumber) {
        return caseService.getCaseByNumber(caseNumber)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<HealthcareCaseDto>builder()
                                .success(false)
                                .message("Healthcare case not found with case number: " + caseNumber)
                                .build()));
    }

    @Operation(summary = "List all cases for a patient")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<HealthcareCaseDto>>> getCasesByPatientId(@PathVariable UUID patientId) {
        List<HealthcareCaseDto> cases = caseService.getCasesByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.ok(cases));
    }

    @Operation(summary = "Record symptoms to an existing case", description = "Appends symptoms to the case and transitions status to SYMPTOMS_RECORDED")
    @PostMapping("/{id}/case-symptoms")
    public ResponseEntity<ApiResponse<HealthcareCaseDto>> recordSymptoms(
            @PathVariable UUID id,
            @Valid @RequestBody RecordSymptomsRequest request
    ) {
        HealthcareCaseDto updated = caseService.recordSymptoms(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Symptoms recorded successfully", updated));
    }

    @Operation(summary = "Update case status and priority", description = "Transitions case status through defined lifecycle states with guard validation")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HealthcareCaseDto>> updateCaseStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStatusRequest request
    ) {
        HealthcareCaseDto updated = caseService.updateCaseStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Case status updated successfully", updated));
    }
}
