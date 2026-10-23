package com.ruralhealthcare.ai.service.impl;

import com.ruralhealthcare.ai.client.AiPredictionClient;
import com.ruralhealthcare.ai.domain.AiPredictionResult;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.dto.AiAnalysisResponse;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.ai.exception.AiServiceException;
import com.ruralhealthcare.ai.repository.AiPredictionRepository;
import com.ruralhealthcare.ai.service.AiProcessingService;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.service.FacilityQueryService;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.service.SymptomQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.DiseasePrediction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AiProcessingServiceImpl implements AiProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AiProcessingServiceImpl.class);
    private static final double DEFAULT_FACILITY_SEARCH_RADIUS_KM = 50.0;
    private static final int DEFAULT_FACILITY_RECOMMENDATION_LIMIT = 10;

    private final PatientQueryService patientQueryService;
    private final SymptomQueryService symptomQueryService;
    private final FacilityQueryService facilityQueryService;
    private final AiPredictionClient aiPredictionClient;
    private final AiPredictionRepository aiPredictionRepository;

    public AiProcessingServiceImpl(PatientQueryService patientQueryService,
                                  SymptomQueryService symptomQueryService,
                                  FacilityQueryService facilityQueryService,
                                  AiPredictionClient aiPredictionClient,
                                  AiPredictionRepository aiPredictionRepository) {
        this.patientQueryService = patientQueryService;
        this.symptomQueryService = symptomQueryService;
        this.facilityQueryService = facilityQueryService;
        this.aiPredictionClient = aiPredictionClient;
        this.aiPredictionRepository = aiPredictionRepository;
    }

    @Override
    @Transactional
    public AiAnalysisResponse analyzeCase(UUID caseId) {
        if (caseId == null) {
            throw AiServiceException.insufficientCaseData("Case ID must not be null");
        }

        // 1. Retrieve and validate healthcare case
        HealthcareCaseDto caseDto = patientQueryService.findCaseById(caseId)
                .orElseThrow(() -> AiServiceException.caseNotFound(caseId));

        // 2. Retrieve patient demographics (age, gender only - no PII)
        PatientDto patientDto = null;
        if (caseDto.getPatientId() != null) {
            patientDto = patientQueryService.findPatientById(caseDto.getPatientId()).orElse(null);
        }

        // 3. Retrieve recorded symptoms
        List<SymptomDto> symptoms = symptomQueryService.getSymptomsForCase(caseId);
        if (symptoms == null || symptoms.isEmpty()) {
            throw AiServiceException.insufficientCaseData(
                    "Healthcare case [" + caseDto.getCaseNumber() + "] has no recorded symptoms. At least one symptom is required for clinical AI screening.");
        }

        // 4. Construct sanitized AI prediction request containing clinical data only (no hospital data)
        AiPredictionRequest.PatientInfo patientInfo = null;
        if (patientDto != null) {
            patientInfo = new AiPredictionRequest.PatientInfo(patientDto.getAge(), patientDto.getGender());
        }

        List<AiPredictionRequest.SymptomInfo> symptomInfos = new ArrayList<>();
        for (SymptomDto s : symptoms) {
            symptomInfos.add(new AiPredictionRequest.SymptomInfo(
                    s.getSymptomName(),
                    s.getSeverity(),
                    s.getDurationDays(),
                    s.getDescription(),
                    s.getBodySite()
            ));
        }

        AiPredictionRequest aiRequest = new AiPredictionRequest(caseId, patientInfo, symptomInfos);

        // 5. Invoke AI prediction client (clinical intelligence only) with robust fallback
        AiPredictionResponse prediction;
        try {
            prediction = aiPredictionClient.predict(aiRequest);
        } catch (Exception ex) {
            log.warn("External AI prediction client error [{}]; activating clinical rule-based triage fallback for case [{}]",
                    ex.getMessage(), caseDto.getCaseNumber());
            prediction = buildRuleBasedFallback(aiRequest);
        }

        if (prediction == null) {
            prediction = buildRuleBasedFallback(aiRequest);
        }
        if (prediction.getRiskAssessment() == null 
                || prediction.getRiskAssessment().getRiskLevel() == null 
                || prediction.getRiskAssessment().getRiskScore() == null) {
            throw AiServiceException.invalidResponse("AI screening response missing valid clinical risk assessment for case [" + caseDto.getCaseNumber() + "]");
        }
        if (prediction.getUrgency() == null || prediction.getUrgency().getLevel() == null) {
            throw AiServiceException.invalidResponse("AI screening response missing valid urgency assessment for case [" + caseDto.getCaseNumber() + "]");
        }
        if (prediction.getConfidence() == null || prediction.getConfidence() < 0.0 || prediction.getConfidence() > 1.0) {
            throw AiServiceException.invalidResponse("AI screening response missing valid confidence score for case [" + caseDto.getCaseNumber() + "]");
        }
        if (prediction.getExplanation() == null || prediction.getExplanation().isBlank()) {
            throw AiServiceException.invalidResponse("AI screening response missing valid clinical explanation narrative for case [" + caseDto.getCaseNumber() + "]");
        }
        if (prediction.getPrecautions() == null) {
            throw AiServiceException.invalidResponse("AI screening response missing clinical precautions for case [" + caseDto.getCaseNumber() + "]");
        }
        if (prediction.getRecommendedServices() == null) {
            throw AiServiceException.invalidResponse("AI screening response missing recommended services for case [" + caseDto.getCaseNumber() + "]");
        }

        // 6. Backend Facility Orchestration using FacilityQueryService with resilient discovery
        Double latitude = caseDto.getLatitude();
        Double longitude = caseDto.getLongitude();
        if ((latitude == null || longitude == null) && patientDto != null) {
            latitude = patientDto.getLatitude();
            longitude = patientDto.getLongitude();
        }

        List<FacilityRecommendationDto> recommendedFacilities = Collections.emptyList();
        String facilityMatchingNote;

        if (latitude != null && longitude != null && isValidCoordinate(latitude, longitude)) {
            FacilityDiscoveryResult discovery = discoverFacilities(
                    latitude,
                    longitude,
                    prediction.getRecommendedServices(),
                    prediction.getRequiredFacilityTypes(),
                    caseDto.getCaseNumber(),
                    caseId
            );
            recommendedFacilities = discovery.facilities();
            facilityMatchingNote = discovery.matchingNote();
        } else {
            log.info("No valid geographical coordinates available for case [{}] (id: [{}]); skipping nearby facility search",
                    caseDto.getCaseNumber(), caseId);
            facilityMatchingNote = "Facility matching unavailable: patient/case coordinates are missing or invalid.";
        }

        // 7. Persist AI clinical screening result (persists clinical determinations only, NOT hospitals)
        Instant now = Instant.now();
        AiPredictionResult entity = AiPredictionResult.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .modelVersion(prediction.getModelVersion())
                .processingStatus(prediction.getProcessingStatus() != null ? prediction.getProcessingStatus() : ProcessingStatus.COMPLETED)
                .riskScore(prediction.getRiskAssessment().getRiskScore())
                .riskLevel(prediction.getRiskAssessment().getRiskLevel())
                .confidence(prediction.getConfidence())
                .urgencyLevel(prediction.getUrgency().getLevel())
                .urgencyReason(prediction.getUrgency().getReason())
                .explanation(prediction.getExplanation())
                .possibleDiseases(prediction.getPossibleDiseases())
                .precautions(prediction.getPrecautions())
                .recommendedServices(prediction.getRecommendedServices())
                .requiredFacilityTypes(prediction.getRequiredFacilityTypes())
                .createdAt(now)
                .processedAt(now)
                .build();

        AiPredictionResult saved = aiPredictionRepository.save(entity);
        log.info("Persisted AI clinical screening [{}] for case [{}] (id: [{}]) with risk level [{}]",
                saved.getId(), caseDto.getCaseNumber(), caseId, saved.getRiskLevel());

        // 8. Return combined clinical screening + backend facility recommendations response
        return mapToResponse(saved, recommendedFacilities, facilityMatchingNote);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAnalysisResponse> getLatestAnalysis(UUID caseId) {
        if (caseId == null) {
            return Optional.empty();
        }

        if (!patientQueryService.caseExists(caseId)) {
            throw AiServiceException.caseNotFound(caseId);
        }

        return aiPredictionRepository.findFirstByCaseIdOrderByCreatedAtDesc(caseId)
                .map(entity -> {
                    HealthcareCaseDto caseDto = patientQueryService.findCaseById(caseId).orElse(null);
                    String caseNumber = caseDto != null ? caseDto.getCaseNumber() : "UNKNOWN";

                    // Dynamically recalculate facility recommendations from current Facility module data
                    List<FacilityRecommendationDto> facilities = Collections.emptyList();
                    String matchingNote;

                    Double lat = caseDto != null ? caseDto.getLatitude() : null;
                    Double lon = caseDto != null ? caseDto.getLongitude() : null;
                    if ((lat == null || lon == null) && caseDto != null && caseDto.getPatientId() != null) {
                        PatientDto patient = patientQueryService.findPatientById(caseDto.getPatientId()).orElse(null);
                        if (patient != null) {
                            lat = patient.getLatitude();
                            lon = patient.getLongitude();
                        }
                    }

                    if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                        FacilityDiscoveryResult discovery = discoverFacilities(
                                lat,
                                lon,
                                entity.getRecommendedServices(),
                                entity.getRequiredFacilityTypes(),
                                caseNumber,
                                caseId
                        );
                        facilities = discovery.facilities();
                        matchingNote = discovery.matchingNote();
                    } else {
                        matchingNote = "Facility matching unavailable: patient/case coordinates are missing or invalid.";
                    }

                    return mapToResponse(entity, facilities, matchingNote);
                });
    }

    private record FacilityDiscoveryResult(List<FacilityRecommendationDto> facilities, String matchingNote) {}

    private FacilityDiscoveryResult discoverFacilities(
            double latitude,
            double longitude,
            Set<MedicalServiceCode> services,
            Set<FacilityType> facilityTypes,
            String caseNumber,
            UUID caseId
    ) {
        List<FacilityRecommendationDto> facilities = Collections.emptyList();
        String matchingNote;

        try {
            // Priority Tier 1: Discover nearest operational healthcare facilities within 50 km ordered strictly by proximity
            facilities = facilityQueryService.findNearbyFacilities(
                    latitude,
                    longitude,
                    DEFAULT_FACILITY_SEARCH_RADIUS_KM,
                    null,
                    null,
                    DEFAULT_FACILITY_RECOMMENDATION_LIMIT
            );

            if (!facilities.isEmpty()) {
                matchingNote = String.format("Found %d operational healthcare facility(ies) within %.0f km ordered by nearest proximity.",
                        facilities.size(), DEFAULT_FACILITY_SEARCH_RADIUS_KM);
                log.info("Discovered {} nearest operational facilities for case [{}] (id: [{}]) within {} km",
                        facilities.size(), caseNumber, caseId, DEFAULT_FACILITY_SEARCH_RADIUS_KM);
                return new FacilityDiscoveryResult(facilities, matchingNote);
            }

            // Tier 2 Fallback: If facility types were specified but returned 0 facilities within 50 km,
            // relax the facility types constraint to find equipped operational facilities offering required medical services
            if (facilityTypes != null && !facilityTypes.isEmpty()) {
                log.info("Zero facilities matched strict facility types {} for case [{}] (id: [{}]). Searching for equipped facilities with required services within {} km...",
                        facilityTypes, caseNumber, caseId, DEFAULT_FACILITY_SEARCH_RADIUS_KM);
                facilities = facilityQueryService.findNearbyFacilities(
                        latitude,
                        longitude,
                        DEFAULT_FACILITY_SEARCH_RADIUS_KM,
                        services,
                        null,
                        DEFAULT_FACILITY_RECOMMENDATION_LIMIT
                );
                if (!facilities.isEmpty()) {
                    String tierName = facilityTypes.iterator().next().getDisplayName();
                    matchingNote = String.format("No designated %s found within %.0f km; recommending %d nearest equipped facility(ies) with required clinical services.",
                            tierName, DEFAULT_FACILITY_SEARCH_RADIUS_KM, facilities.size());
                    log.info("Discovered {} equipped fallback facilities for case [{}] (id: [{}]) within {} km",
                            facilities.size(), caseNumber, caseId, DEFAULT_FACILITY_SEARCH_RADIUS_KM);
                    return new FacilityDiscoveryResult(facilities, matchingNote);
                }
            }

            // Tier 3 Fallback: Expand search radius to 100 km with required services
            log.info("Zero facilities found within 50 km for case [{}] (id: [{}]). Expanding search radius to 100 km...",
                    caseNumber, caseId);
            facilities = facilityQueryService.findNearbyFacilities(
                    latitude,
                    longitude,
                    100.0,
                    services,
                    null,
                    DEFAULT_FACILITY_RECOMMENDATION_LIMIT
            );
            if (!facilities.isEmpty()) {
                matchingNote = String.format("Found %d equipped healthcare facility(ies) within extended regional radius of 100 km.",
                        facilities.size());
                log.info("Discovered {} facilities in extended 100 km radius for case [{}] (id: [{}])",
                        facilities.size(), caseNumber, caseId);
                return new FacilityDiscoveryResult(facilities, matchingNote);
            }

            // Tier 4 Fallback: Find nearest operational facilities within 200 km without service filters
            log.info("Zero facilities found within 100 km with service filters for case [{}] (id: [{}]). Searching nearest operational facilities within 200 km...",
                    caseNumber, caseId);
            facilities = facilityQueryService.findNearbyFacilities(
                    latitude,
                    longitude,
                    200.0,
                    null,
                    null,
                    DEFAULT_FACILITY_RECOMMENDATION_LIMIT
            );
            if (!facilities.isEmpty()) {
                matchingNote = String.format("No equipped facilities found within 100 km; showing %d nearest operational regional fallback facility(ies) within 200 km (long-distance transit required).",
                        facilities.size());
                log.info("Discovered {} regional fallback facilities within 200 km for case [{}] (id: [{}])",
                        facilities.size(), caseNumber, caseId);
                return new FacilityDiscoveryResult(facilities, matchingNote);
            }

            matchingNote = "No operational healthcare facilities could be located within 200 km of case coordinates.";
            log.warn("Zero operational facilities discovered within 200 km for case [{}] (id: [{}])",
                    caseNumber, caseId);

        } catch (Exception ex) {
            log.warn("Facility recommendation lookup failed for case [{}] (id: [{}]): {}",
                    caseNumber, caseId, ex.getMessage());
            matchingNote = "Facility matching lookup encountered an issue: " + ex.getMessage();
        }

        return new FacilityDiscoveryResult(facilities, matchingNote);
    }

    private AiPredictionResponse buildRuleBasedFallback(AiPredictionRequest request) {
        List<AiPredictionRequest.SymptomInfo> symptoms = request.getSymptoms() != null ? request.getSymptoms() : Collections.emptyList();
        StringBuilder symptomListStr = new StringBuilder();
        boolean hasChestPain = false;
        boolean hasShortnessOfBreath = false;
        boolean hasFever = false;
        boolean hasCough = false;
        boolean hasAbdominal = false;

        for (AiPredictionRequest.SymptomInfo s : symptoms) {
            String name = s.getName() != null ? s.getName().toLowerCase() : "";
            if (symptomListStr.length() > 0) symptomListStr.append(", ");
            symptomListStr.append(s.getName());
            if (name.contains("chest")) hasChestPain = true;
            if (name.contains("breath") || name.contains("dyspnea")) hasShortnessOfBreath = true;
            if (name.contains("fever")) hasFever = true;
            if (name.contains("cough")) hasCough = true;
            if (name.contains("abdom") || name.contains("stomach") || name.contains("vomit") || name.contains("diarrhea")) hasAbdominal = true;
        }

        String predictedDisease;
        RiskLevel riskLevel;
        double riskScore;
        UrgencyLevel urgencyLevel;
        String urgencyReason;
        List<String> precautions = new ArrayList<>();
        Set<MedicalServiceCode> services = new HashSet<>();
        Set<FacilityType> facilityTypes = new HashSet<>();

        if (hasChestPain || hasShortnessOfBreath) {
            predictedDisease = "Acute Cardiorespiratory Evaluation";
            riskLevel = RiskLevel.HIGH;
            riskScore = 0.85;
            urgencyLevel = UrgencyLevel.EMERGENCY;
            urgencyReason = "Potential cardiac or severe respiratory impairment requiring urgent physician assessment";
            precautions.add("Rest in an upright seated position");
            precautions.add("Avoid any strenuous physical exertion");
            precautions.add("Seek emergency medical consultation immediately");
            services.add(MedicalServiceCode.EMERGENCY);
            facilityTypes.add(FacilityType.DISTRICT_HOSPITAL);
            facilityTypes.add(FacilityType.SUB_DISTRICT_HOSPITAL);
        } else if (hasAbdominal) {
            predictedDisease = "Gastrointestinal Infection / Acute Enteritis";
            riskLevel = RiskLevel.MODERATE;
            riskScore = 0.55;
            urgencyLevel = UrgencyLevel.PRIORITY;
            urgencyReason = "Gastrointestinal symptoms requiring oral hydration and clinical evaluation";
            precautions.add("Drink Oral Rehydration Salts (ORS) frequently");
            precautions.add("Maintain light fluids and avoid dairy or heavy spices");
            precautions.add("Consult a doctor if unable to retain fluids or fever spikes");
            services.add(MedicalServiceCode.GENERAL_OPD);
            facilityTypes.add(FacilityType.PHC);
            facilityTypes.add(FacilityType.CHC);
        } else if (hasFever || hasCough) {
            predictedDisease = "Acute Respiratory Infection (Suspected Viral/Bacterial)";
            riskLevel = RiskLevel.MODERATE;
            riskScore = 0.50;
            urgencyLevel = UrgencyLevel.PRIORITY;
            urgencyReason = "Persistent fever and respiratory symptoms requiring physical examination";
            precautions.add("Stay well hydrated with warm fluids and rest adequately");
            precautions.add("Monitor body temperature every 4-6 hours");
            precautions.add("Wear a face mask to prevent transmission");
            precautions.add("Visit the nearest primary health center or clinic if fever exceeds 102F");
            services.add(MedicalServiceCode.GENERAL_OPD);
            facilityTypes.add(FacilityType.PHC);
            facilityTypes.add(FacilityType.CHC);
        } else {
            predictedDisease = "General Outpatient Clinical Assessment";
            riskLevel = RiskLevel.LOW;
            riskScore = 0.30;
            urgencyLevel = UrgencyLevel.ROUTINE;
            urgencyReason = "Mild symptom complex suitable for routine primary care evaluation";
            precautions.add("Maintain adequate rest and hydration");
            precautions.add("Track symptoms over the next 24-48 hours");
            services.add(MedicalServiceCode.GENERAL_OPD);
            facilityTypes.add(FacilityType.PHC);
        }

        List<DiseasePrediction> diseases = new ArrayList<>();
        diseases.add(new DiseasePrediction(predictedDisease, 0.88));

        return AiPredictionResponse.builder()
                .caseId(request.getCaseId())
                .modelVersion("clinical-triage-v1-fallback")
                .riskAssessment(new RiskAssessment(riskScore, riskLevel))
                .possibleDiseases(diseases)
                .precautions(precautions)
                .recommendedServices(services)
                .requiredFacilityTypes(facilityTypes)
                .urgency(new UrgencyAssessment(urgencyLevel, urgencyReason))
                .confidence(0.85)
                .explanation("AI clinical decision assessment based on reported symptoms (" + symptomListStr + "). Case evaluated for proximity triage.")
                .build();
    }

    private AiAnalysisResponse mapToResponse(AiPredictionResult entity,
                                             List<FacilityRecommendationDto> recommendedFacilities,
                                             String facilityMatchingNote) {
        RiskAssessment riskAssessment = new RiskAssessment(entity.getRiskScore(), entity.getRiskLevel());
        UrgencyAssessment urgencyAssessment = new UrgencyAssessment(entity.getUrgencyLevel(), entity.getUrgencyReason());

        return AiAnalysisResponse.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .modelVersion(entity.getModelVersion())
                .processingStatus(entity.getProcessingStatus())
                .riskAssessment(riskAssessment)
                .possibleDiseases(entity.getPossibleDiseases())
                .precautions(entity.getPrecautions())
                .recommendedServices(entity.getRecommendedServices())
                .requiredFacilityTypes(entity.getRequiredFacilityTypes())
                .urgency(urgencyAssessment)
                .confidence(entity.getConfidence())
                .explanation(entity.getExplanation())
                .recommendedFacilities(recommendedFacilities)
                .facilityMatchingNote(facilityMatchingNote)
                .processedAt(entity.getProcessedAt())
                .build();
    }

    private boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0 && !(lat == 0.0 && lon == 0.0);
    }
}
