package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.RecordSymptomsRequest;
import com.ruralhealthcare.patient.api.dto.UpdateCaseStatusRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthcareCaseService {

    HealthcareCaseDto createCase(CreateCaseRequest request);

    Optional<HealthcareCaseDto> getCaseById(UUID caseId);

    Optional<HealthcareCaseDto> getCaseByNumber(String caseNumber);

    List<HealthcareCaseDto> getCasesByPatientId(UUID patientId);

    HealthcareCaseDto recordSymptoms(UUID caseId, RecordSymptomsRequest request);

    HealthcareCaseDto updateCaseStatus(UUID caseId, UpdateCaseStatusRequest request);

    List<HealthcareCaseDto> getAllCases();
}
