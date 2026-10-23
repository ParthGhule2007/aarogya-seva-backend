package com.ruralhealthcare.referral.service.impl;

import com.ruralhealthcare.ai.domain.AiPredictionResult;
import com.ruralhealthcare.ai.repository.AiPredictionRepository;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.service.FacilityQueryService;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.referral.api.dto.AiAssessmentSummaryDto;
import com.ruralhealthcare.referral.api.dto.CreateReferralRequest;
import com.ruralhealthcare.referral.api.dto.ReferralDetailDto;
import com.ruralhealthcare.referral.api.dto.ReferralResponseDto;
import com.ruralhealthcare.referral.api.dto.UpdateReferralStatusRequest;
import com.ruralhealthcare.referral.domain.ReferralRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import com.ruralhealthcare.referral.exception.ReferralException;
import com.ruralhealthcare.referral.repository.ReferralRepository;
import com.ruralhealthcare.referral.service.ReferralService;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.service.SymptomQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReferralServiceImpl implements ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralServiceImpl.class);

    private final ReferralRepository referralRepository;
    private final PatientQueryService patientQueryService;
    private final FacilityQueryService facilityQueryService;
    private final SymptomQueryService symptomQueryService;
    private final AiPredictionRepository aiPredictionRepository;

    public ReferralServiceImpl(ReferralRepository referralRepository,
                               PatientQueryService patientQueryService,
                               FacilityQueryService facilityQueryService,
                               SymptomQueryService symptomQueryService,
                               AiPredictionRepository aiPredictionRepository) {
        this.referralRepository = referralRepository;
        this.patientQueryService = patientQueryService;
        this.facilityQueryService = facilityQueryService;
        this.symptomQueryService = symptomQueryService;
        this.aiPredictionRepository = aiPredictionRepository;
    }

    @Override
    @Transactional
    public ReferralResponseDto createReferral(CreateReferralRequest request) {
        UUID caseId = request.getCaseId();
        UUID facilityId = request.getFacilityId();

        // 1. Verify case exists
        HealthcareCaseDto caseDto = patientQueryService.findCaseById(caseId)
                .orElseThrow(() -> ReferralException.caseNotFound(caseId));

        log.info("Processing referral creation for case [{}] (id: [{}]) to facility [{}]",
                caseDto.getCaseNumber(), caseId, facilityId);

        // 2. Extract patient from case
        UUID patientId = caseDto.getPatientId();
        if (patientId == null) {
            throw new ReferralException("Healthcare case [" + caseId + "] does not have an associated patient ID.", "INVALID_CASE_DATA");
        }

        // 2b. Verify patient ID matches if provided in request
        if (request.getPatientId() != null && !request.getPatientId().equals(patientId)) {
            log.warn("Provided patient ID [{}] does not match case patient ID [{}] for case [{}]",
                    request.getPatientId(), patientId, caseId);
            throw new ReferralException("Provided patient ID [" + request.getPatientId() 
                    + "] does not match case patient ID [" + patientId + "].", "PATIENT_MISMATCH");
        }

        // 3. Verify facility exists
        FacilityDto facilityDto = facilityQueryService.getFacilityById(facilityId)
                .orElseThrow(() -> ReferralException.facilityNotFound(facilityId));

        // 4. Verify facility is operational
        if (!facilityDto.isOperational()) {
            log.warn("Facility [{}] is non-operational; rejecting referral creation for case [{}] ({})",
                    facilityId, caseDto.getCaseNumber(), caseId);
            throw ReferralException.facilityNonOperational(facilityId);
        }

        // 5. Verify AI analysis exists for the case
        AiPredictionResult aiPrediction = aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)
                .orElseThrow(() -> ReferralException.aiAnalysisNotFound(caseId));

        // 6. Enforce active referral rule: Only one active referral (PENDING, ACCEPTED, IN_PROGRESS) per case
        boolean activeExists = referralRepository.existsByCaseIdAndStatusIn(caseId, ReferralStatus.getActiveStatuses());
        if (activeExists) {
            log.warn("Active referral already exists for case [{}] ({}); blocking duplicate active referral",
                    caseDto.getCaseNumber(), caseId);
            throw ReferralException.activeReferralExists(caseId);
        }

        // 7. Persist referral entity
        Instant now = Instant.now();
        ReferralRequest referral = ReferralRequest.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityId)
                .aiPredictionId(aiPrediction.getId())
                .status(ReferralStatus.PENDING)
                .patientMessage(request.getPatientMessage())
                .requestedAt(now)
                .build();

        ReferralRequest saved = referralRepository.save(referral);
        log.info("Referral [{}] created successfully with status PENDING for case [{}] (id: [{}]) to facility [{}]",
                saved.getId(), caseDto.getCaseNumber(), caseId, facilityDto.getName());

        return mapToSummaryDto(saved, facilityDto);
    }

    @Override
    public ReferralDetailDto getReferralById(UUID referralId) {
        ReferralRequest referral = referralRepository.findById(referralId)
                .orElseThrow(() -> ReferralException.referralNotFound(referralId));

        FacilityDto facilityDto = facilityQueryService.getFacilityById(referral.getFacilityId()).orElse(null);
        PatientDto patientDto = patientQueryService.findPatientById(referral.getPatientId()).orElse(null);
        HealthcareCaseDto caseDto = patientQueryService.findCaseById(referral.getCaseId()).orElse(null);
        List<SymptomDto> symptoms = symptomQueryService.getSymptomsForCase(referral.getCaseId());

        AiAssessmentSummaryDto aiAssessment = aiPredictionRepository.findById(referral.getAiPredictionId())
                .map(this::mapToAiAssessmentDto)
                .orElse(null);

        ReferralResponseDto referralSummary = mapToSummaryDto(referral, facilityDto);

        return ReferralDetailDto.builder()
                .referral(referralSummary)
                .patient(patientDto)
                .healthcareCase(caseDto)
                .symptoms(symptoms)
                .aiAssessment(aiAssessment)
                .facility(facilityDto)
                .build();
    }

    @Override
    public List<ReferralResponseDto> getReferralsByCaseId(UUID caseId) {
        if (!patientQueryService.caseExists(caseId)) {
            throw ReferralException.caseNotFound(caseId);
        }

        List<ReferralRequest> referrals = referralRepository.findByCaseIdOrderByRequestedAtDesc(caseId);
        return mapReferralsWithFacilities(referrals);
    }

    @Override
    public List<ReferralResponseDto> getReferralsByPatientId(UUID patientId) {
        if (!patientQueryService.patientExists(patientId)) {
            throw new ReferralException("Patient not found with ID: " + patientId, "PATIENT_NOT_FOUND");
        }

        List<ReferralRequest> referrals = referralRepository.findByPatientIdOrderByRequestedAtDesc(patientId);
        return mapReferralsWithFacilities(referrals);
    }

    @Override
    public List<ReferralResponseDto> getReferralsByFacilityId(UUID facilityId, ReferralStatus status) {
        if (facilityQueryService.getFacilityById(facilityId).isEmpty()) {
            throw ReferralException.facilityNotFound(facilityId);
        }

        List<ReferralRequest> referrals;
        if (status != null) {
            referrals = referralRepository.findByFacilityIdAndStatusOrderByRequestedAtDesc(facilityId, status);
        } else {
            referrals = referralRepository.findByFacilityIdOrderByRequestedAtDesc(facilityId);
        }

        return mapReferralsWithFacilities(referrals);
    }

    @Override
    @Transactional
    public ReferralResponseDto updateReferralStatus(UUID referralId, UpdateReferralStatusRequest request) {
        ReferralRequest referral = referralRepository.findById(referralId)
                .orElseThrow(() -> ReferralException.referralNotFound(referralId));

        ReferralStatus currentStatus = referral.getStatus();
        ReferralStatus targetStatus = request.getStatus();

        if (targetStatus == null) {
            throw new ReferralException("Target referral status must not be null", "VALIDATION_FAILED");
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("Invalid referral status transition requested for [{}]: {} -> {}",
                    referralId, currentStatus, targetStatus);
            throw ReferralException.invalidStatusTransition(currentStatus, targetStatus);
        }

        Instant now = Instant.now();
        referral.setStatus(targetStatus);

        if (request.getFacilityResponse() != null && !request.getFacilityResponse().isBlank()) {
            referral.setFacilityResponse(request.getFacilityResponse().trim());
        }

        switch (targetStatus) {
            case ACCEPTED -> {
                referral.setRespondedAt(now);
                referral.setAcceptedAt(now);
            }
            case REJECTED -> referral.setRespondedAt(now);
            case COMPLETED -> referral.setCompletedAt(now);
            case CANCELLED -> {
                // If cancelled before response, leave respondedAt null; otherwise keep existing
            }
            case IN_PROGRESS -> {
                // In progress after acceptance
            }
            case PENDING -> {
                // Cannot transition back to pending
            }
        }

        ReferralRequest updated = referralRepository.save(referral);
        log.info("Referral [{}] status updated from {} to {}", referralId, currentStatus, targetStatus);

        FacilityDto facilityDto = facilityQueryService.getFacilityById(updated.getFacilityId()).orElse(null);
        return mapToSummaryDto(updated, facilityDto);
    }

    private List<ReferralResponseDto> mapReferralsWithFacilities(List<ReferralRequest> referrals) {
        if (referrals.isEmpty()) {
            return new ArrayList<>();
        }

        Map<UUID, FacilityDto> facilityMap = facilityQueryService.getAllFacilities().stream()
                .collect(Collectors.toMap(FacilityDto::getId, f -> f, (existing, replacement) -> existing));

        return referrals.stream()
                .map(r -> mapToSummaryDto(r, facilityMap.get(r.getFacilityId())))
                .collect(Collectors.toList());
    }

    private ReferralResponseDto mapToSummaryDto(ReferralRequest entity, FacilityDto facilityDto) {
        String facilityName = facilityDto != null ? facilityDto.getName() : "Unknown Facility";
        String facilityType = facilityDto != null && facilityDto.getFacilityType() != null 
                ? facilityDto.getFacilityType().name() : null;

        return ReferralResponseDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .patientId(entity.getPatientId())
                .facilityId(entity.getFacilityId())
                .facilityName(facilityName)
                .facilityType(facilityType)
                .aiPredictionId(entity.getAiPredictionId())
                .status(entity.getStatus())
                .statusDisplayName(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)
                .patientMessage(entity.getPatientMessage())
                .facilityResponse(entity.getFacilityResponse())
                .requestedAt(entity.getRequestedAt())
                .respondedAt(entity.getRespondedAt())
                .acceptedAt(entity.getAcceptedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AiAssessmentSummaryDto mapToAiAssessmentDto(AiPredictionResult prediction) {
        return AiAssessmentSummaryDto.builder()
                .aiPredictionId(prediction.getId())
                .modelVersion(prediction.getModelVersion())
                .riskScore(prediction.getRiskScore())
                .riskLevel(prediction.getRiskLevel())
                .urgencyLevel(prediction.getUrgencyLevel())
                .urgencyReason(prediction.getUrgencyReason())
                .confidence(prediction.getConfidence())
                .explanation(prediction.getExplanation())
                .possibleDiseases(prediction.getPossibleDiseases())
                .precautions(prediction.getPrecautions())
                .recommendedServices(prediction.getRecommendedServices())
                .requiredFacilityTypes(prediction.getRequiredFacilityTypes())
                .processedAt(prediction.getProcessedAt())
                .build();
    }
}
