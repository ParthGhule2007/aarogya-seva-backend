package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.common.domain.GeoLocation;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.RecordSymptomsRequest;
import com.ruralhealthcare.patient.api.dto.SymptomInputDto;
import com.ruralhealthcare.patient.api.dto.UpdateCaseStatusRequest;
import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import com.ruralhealthcare.patient.domain.IntakeChannel;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.domain.SymptomSeverity;
import com.ruralhealthcare.patient.repository.CaseSymptomRepository;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.patient.service.impl.HealthcareCaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthcareCaseServiceTest {

    @Mock
    private HealthcareCaseRepository caseRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private CaseSymptomRepository symptomRepository;

    @InjectMocks
    private HealthcareCaseServiceImpl caseService;

    private Patient patient;
    private HealthcareCase healthcareCase;
    private UUID caseId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        caseId = UUID.randomUUID();

        patient = Patient.builder()
                .id(patientId)
                .fullName("Sunita Devi")
                .gender(Gender.FEMALE)
                .contactNumber("+91-9876543211")
                .villageTown("Mohanlalganj")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .build();

        healthcareCase = HealthcareCase.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-A1B2C3")
                .patient(patient)
                .status(CaseStatus.CREATED)
                .priority(CasePriority.LOW)
                .intakeChannel(IntakeChannel.FORM)
                .chiefComplaint("Fever and persistent cough")
                .geoLocation(GeoLocation.of(26.6811, 80.9984))
                .build();
    }

    @Test
    @DisplayName("Should successfully create a healthcare case with initial symptoms")
    void testCreateCaseWithSymptoms() {
        CreateCaseRequest request = CreateCaseRequest.builder()
                .patientId(patientId)
                .intakeChannel(IntakeChannel.FORM)
                .chiefComplaint("High fever")
                .latitude(26.6811)
                .longitude(80.9984)
                .initialSymptoms(List.of(
                        SymptomInputDto.builder()
                                .symptomName("Fever")
                                .severity(SymptomSeverity.MODERATE)
                                .durationDays(3)
                                .build()
                ))
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(caseRepository.existsByCaseNumber(any())).thenReturn(false);
        when(caseRepository.save(any(HealthcareCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HealthcareCaseDto result = caseService.createCase(request);

        assertNotNull(result);
        assertNotNull(result.getCaseNumber());
        assertEquals("Sunita Devi", result.getPatientName());
        assertEquals(CaseStatus.SYMPTOMS_RECORDED, result.getStatus());
        assertEquals(1, result.getSymptoms().size());
        assertEquals("Fever", result.getSymptoms().get(0).getSymptomName());
    }

    @Test
    @DisplayName("Should append symptoms to an active case and update status")
    void testRecordSymptoms() {
        RecordSymptomsRequest request = new RecordSymptomsRequest(List.of(
                SymptomInputDto.builder()
                        .symptomName("Headache")
                        .severity(SymptomSeverity.MILD)
                        .durationDays(2)
                        .build()
        ));

        when(caseRepository.findByIdWithSymptoms(caseId)).thenReturn(Optional.of(healthcareCase));
        when(caseRepository.save(any(HealthcareCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HealthcareCaseDto updated = caseService.recordSymptoms(caseId, request);

        assertNotNull(updated);
        assertEquals(CaseStatus.SYMPTOMS_RECORDED, updated.getStatus());
        assertEquals(1, updated.getSymptoms().size());
    }

    @Test
    @DisplayName("Should reject recording symptoms to a closed case")
    void testRecordSymptomsClosedCase() {
        healthcareCase.setStatus(CaseStatus.CLOSED);
        when(caseRepository.findByIdWithSymptoms(caseId)).thenReturn(Optional.of(healthcareCase));

        RecordSymptomsRequest request = new RecordSymptomsRequest(List.of(
                SymptomInputDto.builder().symptomName("Fever").build()
        ));

        assertThrows(ValidationException.class, () -> caseService.recordSymptoms(caseId, request));
    }

    @Test
    @DisplayName("Should validate valid status transitions")
    void testValidStatusTransition() {
        healthcareCase.setStatus(CaseStatus.SYMPTOMS_RECORDED);
        when(caseRepository.findByIdWithSymptoms(caseId)).thenReturn(Optional.of(healthcareCase));
        when(caseRepository.save(any(HealthcareCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.PENDING_TRIAGE, CasePriority.MEDIUM, "Escalated for screening");

        HealthcareCaseDto updated = caseService.updateCaseStatus(caseId, request);

        assertEquals(CaseStatus.PENDING_TRIAGE, updated.getStatus());
        assertEquals(CasePriority.MEDIUM, updated.getPriority());
    }

    @Test
    @DisplayName("Should reject invalid status transition from CREATED directly to CLOSED")
    void testInvalidStatusTransition() {
        healthcareCase.setStatus(CaseStatus.CREATED);
        when(caseRepository.findByIdWithSymptoms(caseId)).thenReturn(Optional.of(healthcareCase));

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.CLOSED, null, null);

        assertThrows(ValidationException.class, () -> caseService.updateCaseStatus(caseId, request));
    }
}
