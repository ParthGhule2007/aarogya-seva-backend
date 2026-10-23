package com.ruralhealthcare.symptom.service;

import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.symptom.api.dto.BatchCreateSymptomsRequest;
import com.ruralhealthcare.symptom.api.dto.CreateSymptomRequest;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.api.dto.UpdateSymptomRequest;
import com.ruralhealthcare.symptom.domain.Symptom;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.repository.SymptomRepository;
import com.ruralhealthcare.symptom.service.impl.SymptomServiceImpl;
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
class SymptomServiceTest {

    @Mock
    private SymptomRepository symptomRepository;

    @Mock
    private PatientQueryService patientQueryService;

    @InjectMocks
    private SymptomServiceImpl symptomService;

    private UUID caseId;
    private UUID symptomId;
    private HealthcareCaseDto activeCaseDto;
    private Symptom testSymptom;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        symptomId = UUID.randomUUID();

        activeCaseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0001")
                .status(CaseStatus.CREATED)
                .build();

        testSymptom = Symptom.builder()
                .id(symptomId)
                .caseId(caseId)
                .symptomName("Persistent Cough")
                .severity(SymptomSeverity.MODERATE)
                .durationDays(5)
                .description("Dry cough mainly during nighttime")
                .bodySite("Chest")
                .build();
    }

    @Test
    @DisplayName("Should successfully record a single symptom for an active case")
    void testAddSymptomSuccess() {
        CreateSymptomRequest request = CreateSymptomRequest.builder()
                .symptomName("Persistent Cough")
                .severity(SymptomSeverity.MODERATE)
                .durationDays(5)
                .description("Dry cough mainly during nighttime")
                .bodySite("Chest")
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(activeCaseDto));
        when(symptomRepository.save(any(Symptom.class))).thenReturn(testSymptom);

        SymptomDto result = symptomService.addSymptom(caseId, request);

        assertNotNull(result);
        assertEquals(symptomId, result.getId());
        assertEquals("Persistent Cough", result.getSymptomName());
        assertEquals(SymptomSeverity.MODERATE, result.getSeverity());
        assertEquals(5, result.getDurationDays());
        assertEquals("Chest", result.getBodySite());
    }

    @Test
    @DisplayName("Should record multiple symptoms in batch")
    void testAddSymptomsBatchSuccess() {
        BatchCreateSymptomsRequest request = BatchCreateSymptomsRequest.builder()
                .symptoms(List.of(
                        CreateSymptomRequest.builder()
                                .symptomName("High Fever")
                                .severity(SymptomSeverity.SEVERE)
                                .durationDays(3)
                                .build(),
                        CreateSymptomRequest.builder()
                                .symptomName("Headache")
                                .severity(SymptomSeverity.MILD)
                                .durationDays(2)
                                .build()
                ))
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(activeCaseDto));
        when(symptomRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SymptomDto> results = symptomService.addSymptomsBatch(caseId, request);

        assertEquals(2, results.size());
        assertEquals("High Fever", results.get(0).getSymptomName());
        assertEquals(SymptomSeverity.SEVERE, results.get(0).getSeverity());
        assertEquals("Headache", results.get(1).getSymptomName());
    }

    @Test
    @DisplayName("Should reject adding symptom if healthcare case is closed")
    void testAddSymptomCaseClosed() {
        HealthcareCaseDto closedCase = HealthcareCaseDto.builder()
                .id(caseId)
                .status(CaseStatus.CLOSED)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(closedCase));

        CreateSymptomRequest request = CreateSymptomRequest.builder()
                .symptomName("Fever")
                .severity(SymptomSeverity.MILD)
                .durationDays(1)
                .build();

        assertThrows(ValidationException.class, () -> symptomService.addSymptom(caseId, request));
        verify(symptomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when adding symptom to non-existent case")
    void testAddSymptomCaseNotFound() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.empty());

        CreateSymptomRequest request = CreateSymptomRequest.builder()
                .symptomName("Fever")
                .severity(SymptomSeverity.MILD)
                .durationDays(1)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> symptomService.addSymptom(caseId, request));
    }

    @Test
    @DisplayName("Should update an existing symptom")
    void testUpdateSymptomSuccess() {
        UpdateSymptomRequest request = UpdateSymptomRequest.builder()
                .symptomName("Severe Cough with Blood")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(7)
                .build();

        when(symptomRepository.findById(symptomId)).thenReturn(Optional.of(testSymptom));
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(activeCaseDto));
        when(symptomRepository.save(any(Symptom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SymptomDto updated = symptomService.updateSymptom(symptomId, request);

        assertNotNull(updated);
        assertEquals("Severe Cough with Blood", updated.getSymptomName());
        assertEquals(SymptomSeverity.SEVERE, updated.getSeverity());
        assertEquals(7, updated.getDurationDays());
    }

    @Test
    @DisplayName("Should delete an existing symptom")
    void testRemoveSymptomSuccess() {
        when(symptomRepository.findById(symptomId)).thenReturn(Optional.of(testSymptom));
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(activeCaseDto));

        symptomService.removeSymptom(symptomId);

        verify(symptomRepository).delete(testSymptom);
    }

    @Test
    @DisplayName("Should filter symptoms by severity")
    void testGetSymptomsBySeverity() {
        when(patientQueryService.caseExists(caseId)).thenReturn(true);
        when(symptomRepository.findByCaseIdAndSeverity(caseId, SymptomSeverity.MODERATE))
                .thenReturn(List.of(testSymptom));

        List<SymptomDto> results = symptomService.getSymptomsByCaseId(caseId, SymptomSeverity.MODERATE);

        assertEquals(1, results.size());
        assertEquals(SymptomSeverity.MODERATE, results.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should verify public contract hasSevereOrCriticalSymptoms returns true")
    void testHasSevereOrCriticalSymptomsTrue() {
        when(symptomRepository.countByCaseIdAndSeverityIn(eq(caseId), any())).thenReturn(1L);

        boolean hasSevere = symptomService.hasSevereOrCriticalSymptoms(caseId);

        assertTrue(hasSevere);
    }
}
