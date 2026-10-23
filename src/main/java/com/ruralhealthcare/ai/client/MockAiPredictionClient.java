package com.ruralhealthcare.ai.client;

import com.ruralhealthcare.ai.config.AiConfigProperties;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DEVELOPMENT / TESTING ONLY — NOT A MEDICALLY VALIDATED MODEL.
 *
 * Deterministic clinical decision support mock implementation of {@link AiPredictionClient}.
 * Provides synthetic test data for development and integration verification.
 * The risk thresholds, disease categories, and care levels returned by this class are strictly
 * for software testing and must NOT be interpreted as validated clinical guidelines.
 */
public class MockAiPredictionClient implements AiPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(MockAiPredictionClient.class);

    private final AiConfigProperties properties;

    public MockAiPredictionClient(AiConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiPredictionResponse predict(AiPredictionRequest request) {
        log.info("Running deterministic mock clinical AI prediction for case [{}] with {} symptoms",
                request.getCaseId(), request.getSymptoms() != null ? request.getSymptoms().size() : 0);

        boolean hasCritical = false;
        boolean hasSevere = false;
        boolean hasModerate = false;

        if (request.getSymptoms() != null) {
            for (AiPredictionRequest.SymptomInfo s : request.getSymptoms()) {
                if (s.getSeverity() == SymptomSeverity.CRITICAL) {
                    hasCritical = true;
                } else if (s.getSeverity() == SymptomSeverity.SEVERE) {
                    hasSevere = true;
                } else if (s.getSeverity() == SymptomSeverity.MODERATE) {
                    hasModerate = true;
                }
            }
        }

        double riskScore;
        RiskLevel riskLevel;
        UrgencyLevel urgencyLevel;
        String urgencyReason;
        double confidence;
        List<DiseasePrediction> diseases = new ArrayList<>();
        List<String> precautions = new ArrayList<>();
        Set<MedicalServiceCode> services = new HashSet<>();
        Set<FacilityType> facilityTypes = new HashSet<>();

        if (hasCritical) {
            riskScore = 92.5;
            riskLevel = RiskLevel.CRITICAL;
            urgencyLevel = UrgencyLevel.EMERGENCY;
            urgencyReason = "Critical red-flag symptoms detected requiring immediate emergency triage";
            confidence = 0.94;

            diseases.add(new DiseasePrediction("Severe Acute Respiratory Distress / Sepsis", 0.91));
            diseases.add(new DiseasePrediction("Complicated Severe Infection", 0.83));

            precautions.add("Immediate emergency transportation to higher clinical center");
            precautions.add("Administer oxygen therapy if spo2 is below 92%");
            precautions.add("Establish continuous vital signs and hemodynamic monitoring");

            services.add(MedicalServiceCode.EMERGENCY);
            services.add(MedicalServiceCode.GENERAL_OPD);
            services.add(MedicalServiceCode.DIAGNOSTIC_LAB);

            facilityTypes.add(FacilityType.CHC);
            facilityTypes.add(FacilityType.DISTRICT_HOSPITAL);
            facilityTypes.add(FacilityType.SUB_DISTRICT_HOSPITAL);
        } else if (hasSevere) {
            riskScore = 76.0;
            riskLevel = RiskLevel.HIGH;
            urgencyLevel = UrgencyLevel.URGENT;
            urgencyReason = "Severe clinical presentation requires urgent same-day medical evaluation";
            confidence = 0.87;

            diseases.add(new DiseasePrediction("Acute Respiratory Infection", 0.76));
            diseases.add(new DiseasePrediction("Systemic Febrile Illness", 0.68));

            precautions.add("Maintain adequate hydration and oral fluid intake");
            precautions.add("Monitor body temperature and pulse every 4 hours");
            precautions.add("Seek immediate hospitalization if breathing difficulty worsens");

            services.add(MedicalServiceCode.GENERAL_OPD);
            services.add(MedicalServiceCode.DIAGNOSTIC_LAB);
            services.add(MedicalServiceCode.INFECTIOUS_DISEASE);

            facilityTypes.add(FacilityType.PHC);
            facilityTypes.add(FacilityType.CHC);
            facilityTypes.add(FacilityType.DISTRICT_HOSPITAL);
        } else if (hasModerate) {
            riskScore = 48.0;
            riskLevel = RiskLevel.MODERATE;
            urgencyLevel = UrgencyLevel.PRIORITY;
            urgencyReason = "Moderate symptoms warrant timely clinical consultation within 24 to 48 hours";
            confidence = 0.81;

            diseases.add(new DiseasePrediction("Upper Respiratory Tract Infection", 0.72));
            diseases.add(new DiseasePrediction("Acute Bronchitis", 0.59));

            precautions.add("Rest in well-ventilated room and drink warm fluids");
            precautions.add("Take antipyretics for fever as prescribed by primary healthcare worker");
            precautions.add("Practice respiratory hygiene and wear mask around family members");

            services.add(MedicalServiceCode.GENERAL_OPD);
            services.add(MedicalServiceCode.DIAGNOSTIC_LAB);

            facilityTypes.add(FacilityType.PHC);
            facilityTypes.add(FacilityType.CHC);
        } else {
            riskScore = 22.0;
            riskLevel = RiskLevel.LOW;
            urgencyLevel = UrgencyLevel.ROUTINE;
            urgencyReason = "Mild symptomatic presentation suitable for routine outpatient or community screening";
            confidence = 0.75;

            diseases.add(new DiseasePrediction("Common Cold / Viral Rhinitis", 0.84));
            diseases.add(new DiseasePrediction("Mild Environmental Allergy", 0.46));

            precautions.add("Rest at home and maintain good nutrition and hydration");
            precautions.add("Observe symptoms and return for check-up if fever or cough persists beyond 5 days");

            services.add(MedicalServiceCode.GENERAL_OPD);

            facilityTypes.add(FacilityType.SUB_CENTRE);
            facilityTypes.add(FacilityType.PHC);
        }

        String explanation = String.format(
                "[DEVELOPMENT / TESTING ONLY - NOT A MEDICAL DIAGNOSIS] Clinical decision support test screening generated based on %d reported symptom(s). Indicated test risk: %s, urgency: %s.",
                request.getSymptoms() != null ? request.getSymptoms().size() : 0,
                riskLevel.name(),
                urgencyLevel.name()
        );

        String version = properties != null && properties.getVersion() != null
                ? properties.getVersion()
                : "mock-model-v1 (DEVELOPMENT/TESTING ONLY)";

        return AiPredictionResponse.builder()
                .caseId(request.getCaseId())
                .modelVersion(version)
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(riskScore, riskLevel))
                .possibleDiseases(diseases)
                .precautions(precautions)
                .recommendedServices(services)
                .requiredFacilityTypes(facilityTypes)
                .urgency(new UrgencyAssessment(urgencyLevel, urgencyReason))
                .confidence(confidence)
                .explanation(explanation)
                .build();
    }
}
