package com.ruralhealthcare.referral.service;

import com.ruralhealthcare.ai.domain.AiPredictionResult;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.repository.AiPredictionRepository;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
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
import com.ruralhealthcare.referral.api.dto.CreateReferralRequest;
import com.ruralhealthcare.referral.api.dto.ReferralDetailDto;
import com.ruralhealthcare.referral.api.dto.ReferralResponseDto;
import com.ruralhealthcare.referral.api.dto.UpdateReferralStatusRequest;
import com.ruralhealthcare.referral.domain.ReferralRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import com.ruralhealthcare.referral.exception.ReferralException;
import com.ruralhealthcare.referral.repository.ReferralRepository;
import com.ruralhealthcare.referral.service.impl.ReferralServiceImpl;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.service.SymptomQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private PatientQueryService patientQueryService;

    @Mock
    private FacilityQueryService facilityQueryService;

    @Mock
    private SymptomQueryService symptomQueryService;

    @Mock
    private AiPredictionRepository aiPredictionRepository;

    private ReferralServiceImpl service;

    private UUID caseId;
    private UUID patientId;
    private UUID facilityIdA;
    private UUID facilityIdB;
    private UUID aiPredictionId;

    private HealthcareCaseDto caseDto;
    private PatientDto patientDto;
    private FacilityDto facilityDtoA;
    private FacilityDto facilityDtoB;
    private AiPredictionResult aiPrediction;

    @BeforeEach
    void setUp() {
        service = new ReferralServiceImpl(
                referralRepository,
                patientQueryService,
                facilityQueryService,
                symptomQueryService,
                aiPredictionRepository
        );

        caseId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        facilityIdA = UUID.randomUUID();
        facilityIdB = UUID.randomUUID();
        aiPredictionId = UUID.randomUUID();

        caseDto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260923-001")
                .patientId(patientId)
                .status(CaseStatus.TRIAGED)
                .priority(CasePriority.HIGH)
                .intakeChannel(IntakeChannel.FORM)
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        patientDto = PatientDto.builder()
                .id(patientId)
                .fullName("Ramesh Jadhav")
                .age(52)
                .gender(Gender.MALE)
                .contactNumber("+919876543210")
                .villageTown("Shirwal")
                .district("Satara")
                .state("Maharashtra")
                .build();

        facilityDtoA = FacilityDto.builder()
                .id(facilityIdA)
                .name("Shirwal Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .isOperational(true)
                .contactNumber("+912169222111")
                .villageTown("Shirwal")
                .district("Satara")
                .state("Maharashtra")
                .build();

        facilityDtoB = FacilityDto.builder()
                .id(facilityIdB)
                .name("Khandala Community Health Centre")
                .facilityType(FacilityType.CHC)
                .isOperational(true)
                .contactNumber("+912169222222")
                .villageTown("Khandala")
                .district("Satara")
                .state("Maharashtra")
                .build();

        aiPrediction = AiPredictionResult.builder()
                .id(aiPredictionId)
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskScore(76.0)
                .riskLevel(RiskLevel.HIGH)
                .urgencyLevel(UrgencyLevel.URGENT)
                .urgencyReason("Severe respiratory distress requiring immediate oxygen therapy")
                .confidence(0.88)
                .explanation("Patient presents with acute symptoms indicative of severe lower respiratory infection")
                .possibleDiseases(List.of(new DiseasePrediction("Pneumonia", 0.79)))
                .precautions(List.of("Immediate oxygen", "Continuous monitoring"))
                .recommendedServices(Set.of(MedicalServiceCode.EMERGENCY, MedicalServiceCode.GENERAL_OPD))
                .requiredFacilityTypes(Set.of(FacilityType.PHC, FacilityType.CHC))
                .createdAt(Instant.now())
                .processedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a referral with PENDING status when all validations pass")
    void testCreateReferralSuccess() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.of(aiPrediction));
        when(referralRepository.existsByCaseIdAndStatusIn(eq(caseId), any())).thenReturn(false);
        when(referralRepository.save(any(ReferralRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .patientMessage("Need urgent evaluation for severe shortness of breath")
                .build();

        ReferralResponseDto response = service.createReferral(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(patientId, response.getPatientId());
        assertEquals(facilityIdA, response.getFacilityId());
        assertEquals("Shirwal Primary Health Centre", response.getFacilityName());
        assertEquals(ReferralStatus.PENDING, response.getStatus());
        assertEquals("Pending Review", response.getStatusDisplayName());
        assertEquals("Need urgent evaluation for severe shortness of breath", response.getPatientMessage());
        assertNotNull(response.getRequestedAt());

        verify(referralRepository).save(any(ReferralRequest.class));
    }

    @Test
    @DisplayName("Complete Scenario: Facility A rejects -> Patient refers to Facility B for SAME case -> Facility B accepts -> IN_PROGRESS -> COMPLETED")
    void testCompleteReferralLifecycleWithRejectionAndReReferral() {
        // --- 1. First Referral to Facility A ---
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.of(aiPrediction));
        when(referralRepository.existsByCaseIdAndStatusIn(eq(caseId), any())).thenReturn(false);

        UUID referralAId = UUID.randomUUID();
        when(referralRepository.save(any(ReferralRequest.class))).thenAnswer(inv -> {
            ReferralRequest req = inv.getArgument(0);
            req.setId(referralAId);
            return req;
        });

        CreateReferralRequest requestA = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .patientMessage("Shortness of breath")
                .build();

        ReferralResponseDto refA = service.createReferral(requestA);
        assertEquals(ReferralStatus.PENDING, refA.getStatus());

        // --- 2. Facility A reviews and rejects referral ---
        ReferralRequest entityA = ReferralRequest.builder()
                .id(referralAId)
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityIdA)
                .aiPredictionId(aiPredictionId)
                .status(ReferralStatus.PENDING)
                .patientMessage("Shortness of breath")
                .requestedAt(Instant.now())
                .build();

        when(referralRepository.findById(referralAId)).thenReturn(Optional.of(entityA));

        UpdateReferralStatusRequest rejectRequest = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.REJECTED)
                .facilityResponse("Oxygen beds currently occupied; please refer to CHC")
                .build();

        ReferralResponseDto rejectedA = service.updateReferralStatus(referralAId, rejectRequest);
        assertEquals(ReferralStatus.REJECTED, rejectedA.getStatus());
        assertEquals("Oxygen beds currently occupied; please refer to CHC", rejectedA.getFacilityResponse());
        assertNotNull(rejectedA.getRespondedAt());

        // --- 3. Patient sends Referral to Facility B for the SAME case (now allowed because status is REJECTED) ---
        when(facilityQueryService.getFacilityById(facilityIdB)).thenReturn(Optional.of(facilityDtoB));
        when(referralRepository.existsByCaseIdAndStatusIn(eq(caseId), any())).thenReturn(false);

        UUID referralBId = UUID.randomUUID();
        when(referralRepository.save(any(ReferralRequest.class))).thenAnswer(inv -> {
            ReferralRequest req = inv.getArgument(0);
            req.setId(referralBId);
            return req;
        });

        CreateReferralRequest requestB = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdB)
                .patientMessage("Re-referral: PHC full, requesting CHC admission")
                .build();

        ReferralResponseDto refB = service.createReferral(requestB);
        assertEquals(ReferralStatus.PENDING, refB.getStatus());
        assertEquals(facilityIdB, refB.getFacilityId());
        assertEquals("Khandala Community Health Centre", refB.getFacilityName());

        // --- 4. Facility B reviews and accepts referral ---
        ReferralRequest entityB = ReferralRequest.builder()
                .id(referralBId)
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityIdB)
                .aiPredictionId(aiPredictionId)
                .status(ReferralStatus.PENDING)
                .patientMessage(requestB.getPatientMessage())
                .requestedAt(Instant.now())
                .build();

        when(referralRepository.findById(referralBId)).thenReturn(Optional.of(entityB));

        UpdateReferralStatusRequest acceptRequest = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.ACCEPTED)
                .facilityResponse("Bed confirmed in Ward 2; proceed immediately")
                .build();

        ReferralResponseDto acceptedB = service.updateReferralStatus(referralBId, acceptRequest);
        assertEquals(ReferralStatus.ACCEPTED, acceptedB.getStatus());
        assertNotNull(acceptedB.getRespondedAt());
        assertNotNull(acceptedB.getAcceptedAt());

        // --- 5. Facility B advances status to IN_PROGRESS ---
        UpdateReferralStatusRequest inProgressRequest = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.IN_PROGRESS)
                .facilityResponse("Patient arrived, triage and oxygenation initiated")
                .build();

        ReferralResponseDto inProgressB = service.updateReferralStatus(referralBId, inProgressRequest);
        assertEquals(ReferralStatus.IN_PROGRESS, inProgressB.getStatus());

        // --- 6. Referral completed ---
        UpdateReferralStatusRequest completeRequest = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.COMPLETED)
                .facilityResponse("Patient stabilized, discharge summary prepared")
                .build();

        ReferralResponseDto completedB = service.updateReferralStatus(referralBId, completeRequest);
        assertEquals(ReferralStatus.COMPLETED, completedB.getStatus());
        assertNotNull(completedB.getCompletedAt());
    }

    @Test
    @DisplayName("Should block duplicate referral when an active referral already exists for the case")
    void testDuplicateActiveReferralBlocked() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.of(aiPrediction));
        when(referralRepository.existsByCaseIdAndStatusIn(eq(caseId), any())).thenReturn(true);

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .patientMessage("Duplicate request")
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("ACTIVE_REFERRAL_EXISTS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should fail when case does not exist")
    void testCreateReferralCaseNotFound() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.empty());

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("CASE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should fail when facility does not exist")
    void testCreateReferralFacilityNotFound() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.empty());

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("FACILITY_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should fail when facility is non-operational")
    void testCreateReferralFacilityNonOperational() {
        FacilityDto inactiveFacility = FacilityDto.builder()
                .id(facilityIdA)
                .name("Inactive Health Post")
                .isOperational(false)
                .build();

        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(inactiveFacility));

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("FACILITY_NON_OPERATIONAL", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should fail when AI clinical screening analysis does not exist for the case")
    void testCreateReferralAiAnalysisNotFound() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.empty());

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityIdA)
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("AI_ANALYSIS_REQUIRED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject invalid state transitions")
    void testInvalidStatusTransitions() {
        UUID refId = UUID.randomUUID();

        // 1. PENDING cannot transition directly to COMPLETED
        ReferralRequest pendingReq = ReferralRequest.builder()
                .id(refId)
                .status(ReferralStatus.PENDING)
                .build();
        when(referralRepository.findById(refId)).thenReturn(Optional.of(pendingReq));

        UpdateReferralStatusRequest directComplete = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.COMPLETED)
                .build();
        ReferralException ex1 = assertThrows(ReferralException.class,
                () -> service.updateReferralStatus(refId, directComplete));
        assertEquals("INVALID_STATUS_TRANSITION", ex1.getErrorCode());

        // 2. REJECTED cannot transition to ACCEPTED
        ReferralRequest rejectedReq = ReferralRequest.builder()
                .id(refId)
                .status(ReferralStatus.REJECTED)
                .build();
        when(referralRepository.findById(refId)).thenReturn(Optional.of(rejectedReq));

        UpdateReferralStatusRequest toAccepted = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.ACCEPTED)
                .build();
        ReferralException ex2 = assertThrows(ReferralException.class,
                () -> service.updateReferralStatus(refId, toAccepted));
        assertEquals("INVALID_STATUS_TRANSITION", ex2.getErrorCode());

        // 3. COMPLETED cannot transition to CANCELLED
        ReferralRequest completedReq = ReferralRequest.builder()
                .id(refId)
                .status(ReferralStatus.COMPLETED)
                .build();
        when(referralRepository.findById(refId)).thenReturn(Optional.of(completedReq));

        UpdateReferralStatusRequest toCancelled = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.CANCELLED)
                .build();
        ReferralException ex3 = assertThrows(ReferralException.class,
                () -> service.updateReferralStatus(refId, toCancelled));
        assertEquals("INVALID_STATUS_TRANSITION", ex3.getErrorCode());
    }

    @Test
    @DisplayName("Should retrieve full referral context with patient, case, symptoms, AI assessment, and facility for facility review")
    void testGetReferralByIdFullDetail() {
        UUID refId = UUID.randomUUID();
        ReferralRequest entity = ReferralRequest.builder()
                .id(refId)
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityIdA)
                .aiPredictionId(aiPredictionId)
                .status(ReferralStatus.PENDING)
                .patientMessage("Shortness of breath")
                .requestedAt(Instant.now())
                .build();

        SymptomDto symptom = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Dyspnea")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(3)
                .build();

        when(referralRepository.findById(refId)).thenReturn(Optional.of(entity));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(patientQueryService.findPatientById(patientId)).thenReturn(Optional.of(patientDto));
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(symptomQueryService.getSymptomsForCase(caseId)).thenReturn(List.of(symptom));
        when(aiPredictionRepository.findById(aiPredictionId)).thenReturn(Optional.of(aiPrediction));

        ReferralDetailDto detail = service.getReferralById(refId);

        assertNotNull(detail);
        assertNotNull(detail.getReferral());
        assertEquals(refId, detail.getReferral().getId());
        assertNotNull(detail.getPatient());
        assertEquals("Ramesh Jadhav", detail.getPatient().getFullName());
        assertNotNull(detail.getHealthcareCase());
        assertEquals("CASE-20260923-001", detail.getHealthcareCase().getCaseNumber());
        assertEquals(1, detail.getSymptoms().size());
        assertEquals("Dyspnea", detail.getSymptoms().get(0).getSymptomName());
        assertNotNull(detail.getAiAssessment());
        assertEquals(RiskLevel.HIGH, detail.getAiAssessment().getRiskLevel());
        assertEquals(UrgencyLevel.URGENT, detail.getAiAssessment().getUrgencyLevel());
        assertNotNull(detail.getFacility());
        assertEquals("Shirwal Primary Health Centre", detail.getFacility().getName());
    }

    @Test
    @DisplayName("Should retrieve full audit history for a healthcare case ordered by request time descending")
    void testGetReferralsByCaseIdPreservesAuditHistory() {
        ReferralRequest ref1 = ReferralRequest.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .facilityId(facilityIdA)
                .status(ReferralStatus.REJECTED)
                .requestedAt(Instant.now().minusSeconds(3600))
                .build();

        ReferralRequest ref2 = ReferralRequest.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .facilityId(facilityIdB)
                .status(ReferralStatus.COMPLETED)
                .requestedAt(Instant.now())
                .build();

        when(patientQueryService.caseExists(caseId)).thenReturn(true);
        when(referralRepository.findByCaseIdOrderByRequestedAtDesc(caseId)).thenReturn(List.of(ref2, ref1));
        when(facilityQueryService.getAllFacilities()).thenReturn(List.of(facilityDtoA, facilityDtoB));

        List<ReferralResponseDto> history = service.getReferralsByCaseId(caseId);

        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(ReferralStatus.COMPLETED, history.get(0).getStatus());
        assertEquals(ReferralStatus.REJECTED, history.get(1).getStatus());
    }

    @Test
    @DisplayName("Should reject referral creation when provided patient ID does not match case patient ID")
    void testCreateReferralPatientMismatchThrowsException() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));

        UUID wrongPatientId = UUID.randomUUID();
        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .patientId(wrongPatientId)
                .facilityId(facilityIdA)
                .patientMessage("Test message")
                .build();

        ReferralException ex = assertThrows(ReferralException.class, () -> service.createReferral(request));
        assertEquals("PATIENT_MISMATCH", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should accept referral creation when provided patient ID matches case patient ID")
    void testCreateReferralWithMatchingPatientIdSuccess() {
        when(patientQueryService.findCaseById(caseId)).thenReturn(Optional.of(caseDto));
        when(facilityQueryService.getFacilityById(facilityIdA)).thenReturn(Optional.of(facilityDtoA));
        when(aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Optional.of(aiPrediction));
        when(referralRepository.existsByCaseIdAndStatusIn(eq(caseId), any())).thenReturn(false);
        when(referralRepository.save(any(ReferralRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityIdA)
                .patientMessage("Valid patient referral")
                .build();

        ReferralResponseDto response = service.createReferral(request);
        assertNotNull(response);
        assertEquals(patientId, response.getPatientId());
        assertEquals(caseId, response.getCaseId());
    }
}
