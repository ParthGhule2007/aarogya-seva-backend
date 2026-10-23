package com.ruralhealthcare.symptom.service;

import com.ruralhealthcare.symptom.api.dto.BatchCreateSymptomsRequest;
import com.ruralhealthcare.symptom.api.dto.CreateSymptomRequest;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.api.dto.UpdateSymptomRequest;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SymptomService {

    SymptomDto addSymptom(UUID caseId, CreateSymptomRequest request);

    List<SymptomDto> addSymptomsBatch(UUID caseId, BatchCreateSymptomsRequest request);

    List<SymptomDto> getSymptomsByCaseId(UUID caseId, SymptomSeverity severityFilter);

    Optional<SymptomDto> getSymptomById(UUID symptomId);

    SymptomDto updateSymptom(UUID symptomId, UpdateSymptomRequest request);

    void removeSymptom(UUID symptomId);
}
