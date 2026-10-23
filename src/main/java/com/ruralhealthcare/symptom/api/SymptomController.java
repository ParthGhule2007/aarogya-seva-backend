package com.ruralhealthcare.symptom.api;

import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.symptom.api.dto.BatchCreateSymptomsRequest;
import com.ruralhealthcare.symptom.api.dto.CreateSymptomRequest;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.api.dto.UpdateSymptomRequest;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.service.SymptomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Symptoms", description = "Endpoints for recording, querying, updating, and managing symptoms for healthcare cases")
public class SymptomController {

    private final SymptomService symptomService;

    public SymptomController(SymptomService symptomService) {
        this.symptomService = symptomService;
    }

    @Operation(summary = "Add a symptom to a healthcare case", description = "Records a single clinical symptom associated with a case")
    @PostMapping("/cases/{caseId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomDto>> addSymptom(
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateSymptomRequest request
    ) {
        SymptomDto created = symptomService.addSymptom(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Symptom recorded successfully", created));
    }

    @Operation(summary = "Add multiple symptoms in batch to a case", description = "Records a collection of clinical symptoms in a single atomic request")
    @PostMapping("/cases/{caseId}/symptoms/batch")
    public ResponseEntity<ApiResponse<List<SymptomDto>>> addSymptomsBatch(
            @PathVariable UUID caseId,
            @Valid @RequestBody BatchCreateSymptomsRequest request
    ) {
        List<SymptomDto> created = symptomService.addSymptomsBatch(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created.size() + " symptoms recorded successfully", created));
    }

    @Operation(summary = "Retrieve all symptoms for a case", description = "Returns all recorded symptoms for a case, with optional severity filtering")
    @GetMapping("/cases/{caseId}/symptoms")
    public ResponseEntity<ApiResponse<List<SymptomDto>>> getSymptomsByCaseId(
            @PathVariable UUID caseId,
            @RequestParam(required = false) SymptomSeverity severity
    ) {
        List<SymptomDto> symptoms = symptomService.getSymptomsByCaseId(caseId, severity);
        return ResponseEntity.ok(ApiResponse.ok(symptoms));
    }

    @Operation(summary = "Retrieve symptom by ID")
    @GetMapping("/symptoms/{id}")
    public ResponseEntity<ApiResponse<SymptomDto>> getSymptomById(@PathVariable UUID id) {
        return symptomService.getSymptomById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<SymptomDto>builder()
                                .success(false)
                                .message("Symptom not found with ID: " + id)
                                .build()));
    }

    @Operation(summary = "Update symptom details", description = "Modifies symptom name, severity, duration, or description")
    @PutMapping("/symptoms/{id}")
    public ResponseEntity<ApiResponse<SymptomDto>> updateSymptom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSymptomRequest request
    ) {
        SymptomDto updated = symptomService.updateSymptom(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Symptom updated successfully", updated));
    }

    @Operation(summary = "Delete a symptom", description = "Removes a symptom from a healthcare case")
    @DeleteMapping("/symptoms/{id}")
    public ResponseEntity<ApiResponse<Void>> removeSymptom(@PathVariable UUID id) {
        symptomService.removeSymptom(id);
        return ResponseEntity.ok(ApiResponse.ok("Symptom removed successfully", null));
    }
}
