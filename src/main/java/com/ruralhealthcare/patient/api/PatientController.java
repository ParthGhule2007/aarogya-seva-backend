package com.ruralhealthcare.patient.api;

import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.patient.api.dto.CreatePatientRequest;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.api.dto.UpdatePatientRequest;
import com.ruralhealthcare.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@Validated
@Tag(name = "Patients", description = "Endpoints for rural patient registration, profile retrieval, and search")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Register a new patient", description = "Onboards a rural patient with demographics and location context")
    @PostMapping
    public ResponseEntity<ApiResponse<PatientDto>> registerPatient(@Valid @RequestBody CreatePatientRequest request) {
        PatientDto created = patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Patient registered successfully", created));
    }

    @Operation(summary = "Get patient profile by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientDto>> getPatientById(@PathVariable UUID id) {
        return patientService.getPatientById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PatientDto>builder()
                                .success(false)
                                .message("Patient not found with ID: " + id)
                                .build()));
    }

    @Operation(summary = "List all patients", description = "Retrieves all registered rural patients")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientDto>>> getAllPatients() {
        return ResponseEntity.ok(ApiResponse.ok(patientService.getAllPatients()));
    }

    @Operation(summary = "Find patient by contact number")
    @GetMapping({"/by-contact", "/by-phone"})
    public ResponseEntity<ApiResponse<PatientDto>> getPatientByContactNumber(
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "phone", required = false) String phone
    ) {
        String queryPhone = contactNumber != null ? contactNumber : phone;
        if (queryPhone == null || queryPhone.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<PatientDto>builder()
                            .success(false)
                            .message("contactNumber or phone query parameter is required")
                            .build());
        }
        return patientService.getPatientByContactNumber(queryPhone.trim())
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PatientDto>builder()
                                .success(false)
                                .message("Patient not found with contact number: " + queryPhone)
                                .build()));
    }

    @Operation(summary = "Search patients by name")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PatientDto>>> searchPatientsByName(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "query", required = false) String query
    ) {
        String searchQuery = name != null ? name : query;
        if (searchQuery == null || searchQuery.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<PatientDto> patients = patientService.searchPatientsByName(searchQuery.trim());
        return ResponseEntity.ok(ApiResponse.ok(patients));
    }

    @Operation(summary = "Update patient profile")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientDto>> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        PatientDto updated = patientService.updatePatient(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Patient profile updated successfully", updated));
    }
}
