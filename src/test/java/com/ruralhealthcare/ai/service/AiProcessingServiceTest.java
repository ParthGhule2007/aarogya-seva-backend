package com.ruralhealthcare.ai.service;

import com.ruralhealthcare.ai.client.AiPredictionClient;
import com.ruralhealthcare.ai.domain.AiPredictionResult;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiAnalysisResponse;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.ai.exception.AiServiceException;
import com.ruralhealthcare.ai.repository.AiPredictionRepository;
import com.ruralhealthcare.ai.service.impl.AiProcessingServiceImpl;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.service.FacilityQueryService;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.domain.IntakeChannel;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.service.SymptomQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProcessingServiceTest {

    @Mock
    private PatientQueryService patientQueryService;

    @Mock
    private SymptomQueryService symptomQueryService;

    @Mock
    private FacilityQueryService facilityQueryService;

    @Mock
    private AiPredictionClient aiPredictionClient;

    @Mock
    private AiPredictionRepository aiPredictionRepository;

    private AiProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiProcessingServiceImpl(
                patientQueryService,
                symptomQueryService,
                facilityQueryService,
                aiPredictionClient,
                aiPredictionRepository
        );
    }

    @Test
    @DisplayName("Should throw CASE_NOT_FOUND when case does not exist")
    void testAnalyzeCaseNotFound() {
        UUID caseId = UUID.randomUUID();
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.empty());

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("CASE_NOT_FOUND", ex.getErrorCode());
        verify(aiPredictionClient, never()).predict(any());
    }

    @Test
    @DisplayName("Should throw INSUFFICIENT_CASE_DATA when case has no recorded symptoms")
    void testAnalyzeCaseNoSymptoms() {
        UUID caseId = UUID.randomUUID();
        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0001")
                .patientId(UUID.randomUUID())
                .status(CaseStatus.CREATED)
                .priority(CasePriority.MEDIUM)
                .intakeChannel(IntakeChannel.FORM)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(Collections.emptyList());

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INSUFFICIENT_CASE_DATA", ex.getErrorCode());
        verify(aiPredictionClient, never()).predict(any());
    }

    @Test
    @DisplayName("Should successfully analyze case, query facilities, and persist results")
    void testAnalyzeCaseSuccess() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0002")
                .patientId(patientId)
                .status(CaseStatus.SYMPTOMS_RECORDED)
                .priority(CasePriority.HIGH)
                .intakeChannel(IntakeChannel.COMMUNITY_HEALTH_WORKER)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        PatientDto patientDto = PatientDto.builder()
                .id(patientId)
                .fullName("Rajesh Kumar")
                .gender(Gender.MALE)
                .age(45)
                .build();

        SymptomDto symptomDto = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Severe Headache")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(3)
                .description("Throbbing migraine-like headache")
                .bodySite("HEAD")
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(patientQueryService.findPatientById(patientId)).thenReturn(Optional.of(patientDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(symptomDto));

        AiPredictionResponse prediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(75.0, RiskLevel.HIGH))
                .possibleDiseases(List.of(new DiseasePrediction("Hypertensive Crisis / Migraine", 0.79)))
                .precautions(List.of("Blood pressure check"))
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD, MedicalServiceCode.DIAGNOSTIC_LAB))
                .facilityType(FacilityType.PHC)
                .requiredFacilityTypes(Set.of(FacilityType.PHC, FacilityType.CHC))
                .urgency(new UrgencyAssessment(UrgencyLevel.URGENT, "Severe headache with risk"))
                .confidence(0.85)
                .explanation("AI clinical screening result")
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(prediction);

        FacilityRecommendationDto facilityDto = FacilityRecommendationDto.builder()
                .facilityId(UUID.randomUUID())
                .name("Shirwal Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .distanceKm(4.2)
                .isOperational(true)
                .availableServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        when(facilityQueryService.findNearbyFacilities(
                eq(18.5204), eq(73.8567), anyDouble(), any(), any(), anyInt()
        )).thenReturn(List.of(facilityDto));

        when(aiPredictionRepository.save(any(AiPredictionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAnalysisResponse response = service.analyzeCase(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals("health-model-v1", response.getModelVersion());
        assertEquals(RiskLevel.HIGH, response.getRiskAssessment().getRiskLevel());
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.PHC));
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.CHC));
        assertEquals(1, response.getPossibleDiseases().size());
        assertEquals(2, response.getRecommendedServices().size());
        assertEquals(1, response.getRecommendedFacilities().size());
        assertEquals(FacilityType.PHC, response.getRecommendedFacilities().get(0).getFacilityType());
        assertEquals("Shirwal Primary Health Centre", response.getRecommendedFacilities().get(0).getName());

        // Verify request sent to AI model contains ONLY clinical and demographic data (no hospital data)
        ArgumentCaptor<AiPredictionRequest> requestCaptor = ArgumentCaptor.forClass(AiPredictionRequest.class);
        verify(aiPredictionClient).predict(requestCaptor.capture());
        AiPredictionRequest capturedRequest = requestCaptor.getValue();
        assertEquals(caseId, capturedRequest.getCaseId());
        assertNotNull(capturedRequest.getPatient());
        assertEquals(45, capturedRequest.getPatient().getAge());
        assertEquals(Gender.MALE, capturedRequest.getPatient().getGender());
        assertEquals(1, capturedRequest.getSymptoms().size());
        assertEquals("Severe Headache", capturedRequest.getSymptoms().get(0).getName());

        // Verify persistence of clinical result
        verify(aiPredictionRepository).save(any(AiPredictionResult.class));
    }

    @Test
    @DisplayName("Should succeed with empty recommended facilities when case and patient location is unavailable")
    void testAnalyzeCaseMissingLocationSuccess() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        // Location coordinates are null
        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0003")
                .patientId(patientId)
                .status(CaseStatus.SYMPTOMS_RECORDED)
                .priority(CasePriority.MEDIUM)
                .intakeChannel(IntakeChannel.FORM)
                .latitude(null)
                .longitude(null)
                .build();

        PatientDto patientDto = PatientDto.builder()
                .id(patientId)
                .fullName("Amit Sharma")
                .gender(Gender.MALE)
                .age(35)
                .latitude(null)
                .longitude(null)
                .build();

        SymptomDto symptomDto = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Mild Cough")
                .severity(SymptomSeverity.MILD)
                .durationDays(2)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(patientQueryService.findPatientById(patientId)).thenReturn(Optional.of(patientDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(symptomDto));

        AiPredictionResponse prediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(25.0, RiskLevel.LOW))
                .possibleDiseases(List.of(new DiseasePrediction("Common Cold", 0.80)))
                .precautions(List.of("Rest"))
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .requiredFacilityTypes(Set.of(FacilityType.PHC))
                .urgency(new UrgencyAssessment(UrgencyLevel.ROUTINE, "Mild cold"))
                .confidence(0.78)
                .explanation("Test screening")
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(prediction);
        when(aiPredictionRepository.save(any(AiPredictionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAnalysisResponse response = service.analyzeCase(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertTrue(response.getRecommendedFacilities().isEmpty());
        assertNotNull(response.getFacilityMatchingNote());
        assertTrue(response.getFacilityMatchingNote().contains("unavailable"));
        verify(facilityQueryService, never()).findNearbyFacilities(anyDouble(), anyDouble(), anyDouble(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should retrieve latest analysis for a case and dynamically recalculate facilities")
    void testGetLatestAnalysis() {
        UUID caseId = UUID.randomUUID();
        when(patientQueryService.caseExists(caseId)).thenReturn(true);

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));

        AiPredictionResult result = AiPredictionResult.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskScore(45.0)
                .riskLevel(RiskLevel.MODERATE)
                .confidence(0.82)
                .urgencyLevel(UrgencyLevel.PRIORITY)
                .urgencyReason("Needs evaluation")
                .explanation("Screening summary")
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .requiredFacilityTypes(Set.of(FacilityType.PHC))
                .createdAt(Instant.now())
                .processedAt(Instant.now())
                .build();

        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.of(result));

        FacilityRecommendationDto facilityDto = FacilityRecommendationDto.builder()
                .facilityId(UUID.randomUUID())
                .name("Khandala Community Health Centre")
                .facilityType(FacilityType.CHC)
                .distanceKm(8.1)
                .isOperational(true)
                .availableServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        when(facilityQueryService.findNearbyFacilities(
                eq(18.5204), eq(73.8567), anyDouble(), any(), any(), anyInt()
        )).thenReturn(List.of(facilityDto));

        Optional<AiAnalysisResponse> response = service.getLatestAnalysis(caseId);

        assertTrue(response.isPresent());
        assertEquals(caseId, response.get().getCaseId());
        assertEquals(RiskLevel.MODERATE, response.get().getRiskAssessment().getRiskLevel());
        assertTrue(response.get().getRequiredFacilityTypes().contains(FacilityType.PHC));
        assertEquals(1, response.get().getRecommendedFacilities().size());
        assertEquals(FacilityType.CHC, response.get().getRecommendedFacilities().get(0).getFacilityType());
        assertEquals("Khandala Community Health Centre", response.get().getRecommendedFacilities().get(0).getName());
    }

    @Test
    @DisplayName("Should return empty when no analysis exists for case")
    void testGetLatestAnalysisNotFound() {
        UUID caseId = UUID.randomUUID();
        when(patientQueryService.caseExists(caseId)).thenReturn(true);
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.empty());

        Optional<AiAnalysisResponse> response = service.getLatestAnalysis(caseId);
        assertFalse(response.isPresent());
    }

    @Test
    @DisplayName("Should fall back to equipped facilities when zero facilities match strict facility type within 50 km")
    void testAnalyzeCaseZeroNearbyFacilitiesStrictFallbackSuccess() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0099")
                .patientId(patientId)
                .status(CaseStatus.SYMPTOMS_RECORDED)
                .priority(CasePriority.HIGH)
                .intakeChannel(IntakeChannel.FORM)
                .latitude(26.8524)
                .longitude(80.9413)
                .build();

        PatientDto patientDto = PatientDto.builder()
                .id(patientId)
                .fullName("Sunil Yadav")
                .gender(Gender.MALE)
                .age(60)
                .build();

        SymptomDto symptomDto = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Severe Chest Pain")
                .severity(SymptomSeverity.CRITICAL)
                .durationDays(1)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(patientQueryService.findPatientById(patientId)).thenReturn(Optional.of(patientDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(symptomDto));

        // AI classifies as CRITICAL requiring TERTIARY_HOSPITAL
        AiPredictionResponse prediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(95.0, RiskLevel.CRITICAL))
                .possibleDiseases(List.of(new DiseasePrediction("Acute Coronary Syndrome", 0.90)))
                .precautions(List.of("Immediate 108 emergency transport"))
                .recommendedServices(Set.of(MedicalServiceCode.EMERGENCY, MedicalServiceCode.ICU_BEDS))
                .facilityType(FacilityType.TERTIARY_HOSPITAL)
                .requiredFacilityTypes(Set.of(FacilityType.TERTIARY_HOSPITAL))
                .urgency(new UrgencyAssessment(UrgencyLevel.EMERGENCY, "Life-threatening condition"))
                .confidence(0.94)
                .explanation("Patient has critical cardiac indicators")
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(prediction);

        // 1. Strict search within 50 km for TERTIARY_HOSPITAL returns EMPTY list
        when(facilityQueryService.findNearbyFacilities(
                eq(26.8524), eq(80.9413), eq(50.0), any(), eq(Set.of(FacilityType.TERTIARY_HOSPITAL)), anyInt()
        )).thenReturn(Collections.emptyList());

        // 2. Resilient fallback searches within 50 km with required services across any facility type (e.g. DISTRICT_HOSPITAL)
        FacilityRecommendationDto equippedDistrictHospital = FacilityRecommendationDto.builder()
                .facilityId(UUID.randomUUID())
                .name("Lucknow District Civil Hospital")
                .facilityType(FacilityType.DISTRICT_HOSPITAL)
                .distanceKm(3.5)
                .isOperational(true)
                .availableServices(Set.of(MedicalServiceCode.EMERGENCY, MedicalServiceCode.ICU_BEDS))
                .build();

        when(facilityQueryService.findNearbyFacilities(
                eq(26.8524), eq(80.9413), eq(50.0), any(), eq(null), anyInt()
        )).thenReturn(List.of(equippedDistrictHospital));

        when(aiPredictionRepository.save(any(AiPredictionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAnalysisResponse response = service.analyzeCase(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.CRITICAL, response.getRiskAssessment().getRiskLevel());
        assertEquals(1, response.getRecommendedFacilities().size());
        assertEquals("Lucknow District Civil Hospital", response.getRecommendedFacilities().get(0).getName());
        assertTrue(response.getFacilityMatchingNote().contains("recommending 1 nearest equipped facility"));
    }

    @Test
    @DisplayName("Should throw AiServiceException when AI model returns null risk assessment")
    void testAnalyzeCaseInvalidAiResponseNullRiskThrowsException() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0100")
                .patientId(patientId)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        // Invalid AI prediction with null risk assessment
        AiPredictionResponse invalidPrediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(null)
                .confidence(0.85)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Eval"))
                .explanation("Missing risk")
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(invalidPrediction);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("risk assessment"));
    }

    @Test
    @DisplayName("Should throw AiServiceException when AI model returns null confidence score")
    void testAnalyzeCaseInvalidAiResponseNullConfidenceThrowsException() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0101")
                .patientId(patientId)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        AiPredictionResponse invalidPrediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(50.0, RiskLevel.MODERATE))
                .confidence(null)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Eval"))
                .explanation("Missing confidence")
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(invalidPrediction);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("confidence score"));
    }

    @Test
    @DisplayName("Should throw AiServiceException when AI model returns null or blank explanation narrative")
    void testAnalyzeCaseInvalidAiResponseBlankExplanationThrowsException() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0102")
                .patientId(patientId)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        AiPredictionResponse invalidPrediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(50.0, RiskLevel.MODERATE))
                .confidence(0.85)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Eval"))
                .explanation("   ")
                .precautions(List.of("Rest"))
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(invalidPrediction);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("explanation narrative"));
    }

    @Test
    @DisplayName("Should throw AiServiceException when AI model returns null precautions")
    void testAnalyzeCaseInvalidAiResponseNullPrecautionsThrowsException() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0103")
                .patientId(patientId)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        AiPredictionResponse invalidPrediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(50.0, RiskLevel.MODERATE))
                .confidence(0.85)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Eval"))
                .explanation("Valid explanation narrative")
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();
        invalidPrediction.setPrecautions(null);

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(invalidPrediction);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("precautions"));
    }

    @Test
    @DisplayName("Should throw AiServiceException when AI model returns null recommended services")
    void testAnalyzeCaseInvalidAiResponseNullRecommendedServicesThrowsException() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0105")
                .patientId(patientId)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        AiPredictionResponse invalidPrediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(50.0, RiskLevel.MODERATE))
                .confidence(0.85)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Eval"))
                .explanation("Valid explanation narrative")
                .precautions(List.of("Rest"))
                .build();
        invalidPrediction.setRecommendedServices(null);

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(invalidPrediction);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.analyzeCase(caseId));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("recommended services"));
    }

    @Test
    @DisplayName("Should trigger Tier 4 (200 km) regional fallback when 50 km and 100 km searches yield zero facilities")
    void testAnalyzeCaseTier4RegionalFallback200Km() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261015-0104")
                .patientId(patientId)
                .latitude(26.8524)
                .longitude(80.9413)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.MODERATE).durationDays(2).build()
        ));

        AiPredictionResponse prediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(85.0, RiskLevel.HIGH))
                .confidence(0.92)
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "High fever"))
                .explanation("Patient has acute febrile illness")
                .precautions(List.of("Hydration"))
                .recommendedServices(Set.of(MedicalServiceCode.INFECTIOUS_DISEASE))
                .requiredFacilityTypes(Set.of(FacilityType.TERTIARY_HOSPITAL))
                .build();

        when(aiPredictionClient.predict(any(AiPredictionRequest.class))).thenReturn(prediction);

        // Tier 1 (50 km strict): empty
        when(facilityQueryService.findNearbyFacilities(eq(26.8524), eq(80.9413), eq(50.0), any(), eq(Set.of(FacilityType.TERTIARY_HOSPITAL)), anyInt()))
                .thenReturn(Collections.emptyList());
        // Tier 2 (50 km services): empty
        when(facilityQueryService.findNearbyFacilities(eq(26.8524), eq(80.9413), eq(50.0), any(), eq(null), anyInt()))
                .thenReturn(Collections.emptyList());
        // Tier 3 (100 km services): empty
        when(facilityQueryService.findNearbyFacilities(eq(26.8524), eq(80.9413), eq(100.0), any(), eq(null), anyInt()))
                .thenReturn(Collections.emptyList());

        // Tier 4 (200 km operational fallback): returns 1 regional hospital
        FacilityRecommendationDto regionalHospital = FacilityRecommendationDto.builder()
                .facilityId(UUID.randomUUID())
                .name("Regional Civil Hospital")
                .facilityType(FacilityType.DISTRICT_HOSPITAL)
                .distanceKm(145.0)
                .isOperational(true)
                .build();
        when(facilityQueryService.findNearbyFacilities(eq(26.8524), eq(80.9413), eq(200.0), eq(null), eq(null), anyInt()))
                .thenReturn(List.of(regionalHospital));

        when(aiPredictionRepository.save(any(AiPredictionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAnalysisResponse response = service.analyzeCase(caseId);

        assertNotNull(response);
        assertEquals(1, response.getRecommendedFacilities().size());
        assertEquals("Regional Civil Hospital", response.getRecommendedFacilities().get(0).getName());
        assertEquals(145.0, response.getRecommendedFacilities().get(0).getDistanceKm());
        assertTrue(response.getFacilityMatchingNote().contains("regional fallback"));
        assertTrue(response.getFacilityMatchingNote().contains("long-distance transit required"));
    }

    @Test
    @DisplayName("Should retrieve all recorded multiple symptoms and include all of them in AI analysis prediction request")
    void testAnalyzeCaseWithMultipleSymptomsCapturesAllSymptomsInAiRequest() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20261019-MULTISYM")
                .patientId(patientId)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        PatientDto patientDto = PatientDto.builder()
                .id(patientId)
                .age(42)
                .gender(Gender.FEMALE)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        List<SymptomDto> multipleSymptoms = List.of(
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Fever").severity(SymptomSeverity.SEVERE).durationDays(3).build(),
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Cough").severity(SymptomSeverity.MODERATE).durationDays(4).build(),
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Headache").severity(SymptomSeverity.MILD).durationDays(2).build(),
                SymptomDto.builder().id(UUID.randomUUID()).symptomName("Chest Pain").severity(SymptomSeverity.CRITICAL).durationDays(1).build()
        );

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(patientQueryService.findPatientById(patientId)).thenReturn(Optional.of(patientDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(multipleSymptoms);

        AiPredictionResponse prediction = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("engine-v2.0")
                .riskAssessment(new RiskAssessment(88.0, RiskLevel.CRITICAL))
                .confidence(0.94)
                .urgency(new UrgencyAssessment(UrgencyLevel.EMERGENCY, "Severe symptom cluster"))
                .possibleDiseases(List.of(new DiseasePrediction("Acute Respiratory Infection", 0.85)))
                .precautions(List.of("Immediate hospitalization"))
                .recommendedServices(Set.of(MedicalServiceCode.EMERGENCY))
                .requiredFacilityTypes(Set.of(FacilityType.TERTIARY_HOSPITAL))
                .explanation("Multi-symptom critical presentation with chest pain and high fever")
                .build();

        ArgumentCaptor<AiPredictionRequest> captor = ArgumentCaptor.forClass(AiPredictionRequest.class);
        when(aiPredictionClient.predict(captor.capture())).thenReturn(prediction);

        when(facilityQueryService.findNearbyFacilities(anyDouble(), anyDouble(), anyDouble(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiPredictionRepository.save(any(AiPredictionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAnalysisResponse response = service.analyzeCase(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.CRITICAL, response.getRiskAssessment().getRiskLevel());

        AiPredictionRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(caseId, capturedRequest.getCaseId());
        assertEquals(42, capturedRequest.getPatient().getAge());
        assertEquals(Gender.FEMALE, capturedRequest.getPatient().getGender());

        // Validate all 4 symptoms were captured and delivered
        assertEquals(4, capturedRequest.getSymptoms().size());
        assertEquals("Fever", capturedRequest.getSymptoms().get(0).getName());
        assertEquals(SymptomSeverity.SEVERE, capturedRequest.getSymptoms().get(0).getSeverity());
        assertEquals(3, capturedRequest.getSymptoms().get(0).getDurationDays());

        assertEquals("Cough", capturedRequest.getSymptoms().get(1).getName());
        assertEquals(SymptomSeverity.MODERATE, capturedRequest.getSymptoms().get(1).getSeverity());

        assertEquals("Headache", capturedRequest.getSymptoms().get(2).getName());
        assertEquals(SymptomSeverity.MILD, capturedRequest.getSymptoms().get(2).getSeverity());

        assertEquals("Chest Pain", capturedRequest.getSymptoms().get(3).getName());
        assertEquals(SymptomSeverity.CRITICAL, capturedRequest.getSymptoms().get(3).getSeverity());
    }
}
