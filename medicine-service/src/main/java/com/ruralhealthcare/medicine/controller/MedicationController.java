package com.ruralhealthcare.medicine.controller;

import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.DoseEventResponse;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.dto.OcrExtractionResponse;
import com.ruralhealthcare.medicine.dto.UpdateMedicationRequest;
import com.ruralhealthcare.medicine.service.DoseEventService;
import com.ruralhealthcare.medicine.service.MedicationService;
import com.ruralhealthcare.medicine.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/medications")
@Tag(name = "Medications", description = "Endpoints for managing patient medicine reminders, visual photo references, and schedules")
public class MedicationController {

    private final MedicationService medicationService;
    private final DoseEventService doseEventService;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MedicationController(MedicationService medicationService,
                                DoseEventService doseEventService,
                                OcrService ocrService,
                                ObjectMapper objectMapper,
                                Validator validator) {
        this.medicationService = medicationService;
        this.doseEventService = doseEventService;
        this.ocrService = ocrService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create medicine reminder (JSON only)",
            description = "Creates a new medicine reminder without photo (Mode C). Accepts standard application/json payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medication reminder created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<MedicationResponse> createMedicationJson(
            @Valid @RequestBody CreateMedicationRequest request) {
        MedicationResponse response = medicationService.createMedication(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create medicine reminder (Multipart)",
            description = "Creates a new medicine reminder with optional photo reference. Supports Mode A (photo + name) and Mode C (name only without photo).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medication reminder created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid image")
    })
    public ResponseEntity<MedicationResponse> createMedication(
            @RequestPart("metadata") String metadata,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        CreateMedicationRequest request = parseAndValidateMetadata(metadata);
        MedicationResponse response = medicationService.createMedication(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private CreateMedicationRequest parseAndValidateMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            throw new MedicationValidationException("metadata part is required");
        }
        CreateMedicationRequest request;
        try {
            request = objectMapper.readValue(metadata.trim(), CreateMedicationRequest.class);
        } catch (Exception e) {
            throw new MedicationValidationException("Invalid JSON format in metadata part: " + e.getMessage());
        }
        Set<ConstraintViolation<CreateMedicationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new MedicationValidationException("Validation failed: " + message);
        }
        return request;
    }

    @PostMapping(value = "/extract-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract medicine packaging text via OCR",
            description = "Extracts text from uploaded medicine packaging photograph. Transparently reports simulated/mock status.")
    public ResponseEntity<OcrExtractionResponse> extractMedicineOcr(
            @RequestPart("image") MultipartFile image) {
        OcrExtractionResponse response = ocrService.extractText(image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medication by ID", description = "Retrieves medication details including active presigned image URL.")
    public ResponseEntity<MedicationResponse> getMedicationById(
            @Parameter(description = "Medication UUID") @PathVariable UUID id) {
        MedicationResponse response = medicationService.getMedicationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all medications for patient", description = "Retrieves all active medications configured for a patient.")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByPatientId(
            @Parameter(description = "Patient UUID") @PathVariable UUID patientId) {
        List<MedicationResponse> responses = medicationService.getMedicationsByPatientId(patientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/today/{patientId}")
    @Operation(summary = "Get today's scheduled doses",
            description = "Retrieves all scheduled dose events for today for a patient, with statuses (PENDING, TAKEN, MISSED).")
    public ResponseEntity<List<DoseEventResponse>> getTodayDoses(
            @Parameter(description = "Patient UUID") @PathVariable UUID patientId) {
        List<DoseEventResponse> responses = doseEventService.getTodayDosesForPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/history/{patientId}")
    @Operation(summary = "Get dose event history", description = "Retrieves all historical doses and their taken/missed statuses.")
    public ResponseEntity<List<DoseEventResponse>> getDoseHistory(
            @Parameter(description = "Patient UUID") @PathVariable UUID patientId) {
        List<DoseEventResponse> responses = doseEventService.getDoseHistoryForPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update medication reminder", description = "Updates dosage, reminder times, or instructions.")
    public ResponseEntity<MedicationResponse> updateMedication(
            @Parameter(description = "Medication UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicationRequest request) {
        MedicationResponse response = medicationService.updateMedication(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark medication course as COMPLETED", description = "Marks a medication course as completed upon finishing.")
    public ResponseEntity<MedicationResponse> completeMedication(
            @Parameter(description = "Medication UUID") @PathVariable UUID id) {
        MedicationResponse response = medicationService.completeMedication(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/discontinue")
    @Operation(summary = "Mark medication as DISCONTINUED", description = "Discontinues a medication by clinician or patient decision.")
    public ResponseEntity<MedicationResponse> discontinueMedication(
            @Parameter(description = "Medication UUID") @PathVariable UUID id) {
        MedicationResponse response = medicationService.discontinueMedication(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate medication reminder", description = "Deactivates a medication schedule.")
    public ResponseEntity<Void> deactivateMedication(
            @Parameter(description = "Medication UUID") @PathVariable UUID id) {
        medicationService.deactivateMedication(id);
        return ResponseEntity.noContent().build();
    }
}
