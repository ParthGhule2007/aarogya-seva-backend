package com.ruralhealthcare.ai.api;

import com.ruralhealthcare.ai.dto.AiAnalysisResponse;
import com.ruralhealthcare.ai.service.AiProcessingService;
import com.ruralhealthcare.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "AI Clinical Intelligence", description = "Endpoints for AI-driven clinical decision support, risk stratification, and facility matching")
public class AiController {

    private final AiProcessingService aiProcessingService;

    public AiController(AiProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @Operation(
            summary = "Run AI clinical screening and decision support",
            description = "Analyzes patient clinical presentation and recorded symptoms using AI to predict potential disease categories, risk level, clinical precautions, required services, and recommended nearby healthcare facilities."
    )
    @PostMapping("/cases/{caseId}/ai-analysis")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> runAiAnalysis(@PathVariable UUID caseId) {
        AiAnalysisResponse analysis = aiProcessingService.analyzeCase(caseId);
        return ResponseEntity.ok(ApiResponse.ok("AI clinical screening completed successfully", analysis));
    }

    @Operation(
            summary = "Get latest AI analysis for a healthcare case",
            description = "Retrieves the most recently generated clinical screening result, risk assessment, and facility recommendations for the case."
    )
    @GetMapping("/cases/{caseId}/ai-analysis")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> getLatestAnalysis(@PathVariable UUID caseId) {
        return aiProcessingService.getLatestAnalysis(caseId)
                .map(analysis -> ResponseEntity.ok(ApiResponse.ok(analysis)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<AiAnalysisResponse>builder()
                                .success(false)
                                .message("No AI clinical screening analysis found for case ID: " + caseId)
                                .build()));
    }
}
