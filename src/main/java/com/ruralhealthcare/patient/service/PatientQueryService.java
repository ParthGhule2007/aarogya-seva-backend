package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Public service contract exposed by Module 2 for other domain modules (Screening, Referral).
 * Other modules must interact with Patient domain exclusively through this interface.
 */
public interface PatientQueryService {

    Optional<PatientDto> findPatientById(UUID patientId);

    boolean patientExists(UUID patientId);

    Optional<HealthcareCaseDto> findCaseById(UUID caseId);

    boolean caseExists(UUID caseId);
}
