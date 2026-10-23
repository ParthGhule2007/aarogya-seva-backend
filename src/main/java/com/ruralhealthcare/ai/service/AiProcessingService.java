package com.ruralhealthcare.ai.service;

import com.ruralhealthcare.ai.dto.AiAnalysisResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for AI-driven clinical decision support and screening orchestration.
 */
public interface AiProcessingService {

    /**
     * Executes AI clinical screening for an existing healthcare case.
     * Aggregates case demographics and recorded symptoms, invokes the configured AI model client,
     * resolves matching healthcare facilities via Module 1, and persists the screening audit record.
     *
     * @param caseId the healthcare case UUID
     * @return structured AI clinical analysis response
     */
    AiAnalysisResponse analyzeCase(UUID caseId);

    /**
     * Retrieves the most recent AI screening assessment performed for a case.
     *
     * @param caseId the healthcare case UUID
     * @return Optional containing the latest analysis if one has been generated, otherwise empty
     */
    Optional<AiAnalysisResponse> getLatestAnalysis(UUID caseId);
}
