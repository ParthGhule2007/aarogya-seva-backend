package com.ruralhealthcare.symptom.service;

import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;

import java.util.List;
import java.util.UUID;

/**
 * Public service contract exposed by Module 3 for downstream modules (AI Screening, Risk Classification).
 * Downstream modules must interact with the Symptom domain exclusively through this interface.
 */
public interface SymptomQueryService {

    List<SymptomDto> getSymptomsForCase(UUID caseId);

    List<SymptomDto> getSymptomsBySeverity(UUID caseId, SymptomSeverity severity);

    boolean hasSevereOrCriticalSymptoms(UUID caseId);

    long countSymptoms(UUID caseId);

    boolean hasSymptoms(UUID caseId);
}
