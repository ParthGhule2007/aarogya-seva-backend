package com.ruralhealthcare.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.ai.exception.AiServiceException;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradioTriageParser {

    private static final Logger log = LoggerFactory.getLogger(GradioTriageParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern RISK_SCORE_PATTERN =
            Pattern.compile("(LOW|MODERATE|HIGH|CRITICAL)\\s*RISK\\s*-\\s*([0-9.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URGENCY_PATTERN =
            Pattern.compile("\\*\\*Urgency:\\*\\*\\s*`?([A-Za-z_-]+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern URGENCY_REASON_PATTERN =
            Pattern.compile("\\*\\*Clinical Urgency Reason:\\*\\*\\s*(.*)");
    private static final Pattern CONFIDENCE_PATTERN =
            Pattern.compile("\\*\\*Confidence Score:\\*\\*\\s*`?([0-9.]+)%?`?");
    private static final Pattern DIAGNOSIS_PATTERN =
            Pattern.compile("-\\s*\\*\\*(.*?)\\*\\*\\s*\\(Probability:\\s*`?([0-9.]+)%?`?\\)");
    private static final Pattern TARGET_FACILITIES_PATTERN =
            Pattern.compile("\\*\\*Target Facilities:\\*\\*\\s*(.*)");
    private static final Pattern REQUIRED_SERVICES_PATTERN =
            Pattern.compile("\\*\\*Required Departments / Services:\\*\\*\\s*(.*)");

    public static AiPredictionResponse parseGradioSseData(UUID caseId, String modelVersion, List<String> dataArray) {
        if (dataArray == null || dataArray.isEmpty()) {
            throw AiServiceException.invalidResponse("Empty Gradio triage data array received from AI model");
        }

        String panel1 = dataArray.size() > 0 ? dataArray.get(0) : "";
        String panel2 = dataArray.size() > 1 ? dataArray.get(1) : "";
        String panel3 = dataArray.size() > 2 ? dataArray.get(2) : "";
        String panel4 = dataArray.size() > 3 ? dataArray.get(3) : "";

        // Panel 1: Strict Risk Assessment & Urgency Validation
        RiskLevel riskLevel = null;
        Double riskScore = null;
        Matcher riskMatcher = RISK_SCORE_PATTERN.matcher(panel1);
        if (riskMatcher.find()) {
            try {
                riskLevel = RiskLevel.fromString(riskMatcher.group(1));
            } catch (Exception e) {
                log.warn("Could not parse risk level: {}", riskMatcher.group(1));
            }
            try {
                riskScore = Double.parseDouble(riskMatcher.group(2));
            } catch (Exception e) {
                log.warn("Could not parse risk score: {}", riskMatcher.group(2));
            }
        }
        if (riskLevel == null || riskScore == null) {
            throw AiServiceException.invalidResponse("Could not parse valid clinical risk assessment from AI model triage panel");
        }
        if (riskScore < 0.0 || riskScore > 100.0) {
            throw AiServiceException.invalidResponse("Parsed risk score [" + riskScore + "] is outside valid range (0-100)");
        }

        UrgencyLevel urgencyLevel = null;
        Matcher urgencyMatcher = URGENCY_PATTERN.matcher(panel1);
        if (urgencyMatcher.find()) {
            try {
                urgencyLevel = UrgencyLevel.fromString(urgencyMatcher.group(1));
            } catch (Exception e) {
                log.warn("Could not parse urgency level: {}", urgencyMatcher.group(1));
            }
        }
        if (urgencyLevel == null) {
            throw AiServiceException.invalidResponse("Could not parse valid clinical urgency level from AI model triage panel");
        }

        String urgencyReason = null;
        Matcher reasonMatcher = URGENCY_REASON_PATTERN.matcher(panel1);
        if (reasonMatcher.find()) {
            urgencyReason = reasonMatcher.group(1).trim().replaceAll("^`|`$", "");
        }
        if (urgencyReason == null || urgencyReason.isBlank()) {
            urgencyReason = "Clinical evaluation recommended based on recorded symptoms";
        }

        Double confidence = null;
        Matcher confMatcher = CONFIDENCE_PATTERN.matcher(panel1);
        if (confMatcher.find()) {
            try {
                double val = Double.parseDouble(confMatcher.group(1));
                confidence = val > 1.0 ? val / 100.0 : val;
            } catch (Exception e) {
                log.warn("Could not parse confidence: {}", confMatcher.group(1));
            }
        }
        if (confidence == null || confidence < 0.0 || confidence > 1.0) {
            throw AiServiceException.invalidResponse("Could not parse valid confidence score from AI model triage panel");
        }

        // Panel 2: Possible Diagnoses
        List<DiseasePrediction> possibleDiseases = new ArrayList<>();
        Matcher diagMatcher = DIAGNOSIS_PATTERN.matcher(panel2);
        while (diagMatcher.find()) {
            String category = diagMatcher.group(1).trim();
            double prob = 0.70;
            try {
                double raw = Double.parseDouble(diagMatcher.group(2));
                prob = raw > 1.0 ? raw / 100.0 : raw;
            } catch (Exception e) {
                log.warn("Could not parse diagnosis probability: {}", diagMatcher.group(2));
            }
            possibleDiseases.add(new DiseasePrediction(category, prob));
        }

        // Panel 3: Facilities, Services & Precautions
        Set<FacilityType> requiredFacilityTypes = new LinkedHashSet<>();
        Matcher facMatcher = TARGET_FACILITIES_PATTERN.matcher(panel3);
        if (facMatcher.find()) {
            String facString = facMatcher.group(1);
            String[] tokens = facString.split(",");
            for (String tok : tokens) {
                String clean = tok.replace("`", "").trim();
                try {
                    FacilityType ft = FacilityType.fromString(clean);
                    if (ft != null) {
                        requiredFacilityTypes.add(ft);
                    }
                } catch (Exception ex) {
                    log.debug("Unknown facility token: {}", clean);
                }
            }
        }

        Set<MedicalServiceCode> recommendedServices = new LinkedHashSet<>();
        Matcher srvMatcher = REQUIRED_SERVICES_PATTERN.matcher(panel3);
        if (srvMatcher.find()) {
            String srvString = srvMatcher.group(1);
            String[] tokens = srvString.split(",");
            for (String tok : tokens) {
                String clean = tok.replace("`", "").trim();
                MedicalServiceCode sc = MedicalServiceCode.fromString(clean);
                if (sc != null) {
                    recommendedServices.add(sc);
                }
            }
        }

        List<String> precautions = new ArrayList<>();
        int precIndex = panel3.indexOf("Immediate Precautions:");
        if (precIndex != -1) {
            String precSection = panel3.substring(precIndex);
            String[] lines = precSection.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("- ")) {
                    precautions.add(trimmed.substring(2).trim());
                }
            }
        }

        // Panel 4: Clinical Narrative / Explanation
        String explanation = panel4.trim();
        int narrIndex = explanation.indexOf("Clinical Physician Narrative:");
        if (narrIndex != -1) {
            explanation = explanation.substring(narrIndex + "Clinical Physician Narrative:".length()).trim();
            explanation = explanation.replaceFirst("^>\\s*", "").trim();
        }
        if (explanation.isBlank()) {
            throw AiServiceException.invalidResponse("AI model returned empty clinical physician narrative");
        }

        FacilityType primaryFacility;
        if (!requiredFacilityTypes.isEmpty()) {
            primaryFacility = requiredFacilityTypes.iterator().next();
        } else {
            switch (riskLevel) {
                case CRITICAL -> primaryFacility = FacilityType.TERTIARY_HOSPITAL;
                case HIGH -> primaryFacility = FacilityType.DISTRICT_HOSPITAL;
                case MODERATE -> primaryFacility = FacilityType.CHC;
                default -> primaryFacility = FacilityType.PHC;
            }
        }

        return AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion(modelVersion != null ? modelVersion : "aarogya-clinical-engine-v2.0")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(riskScore, riskLevel))
                .urgency(new UrgencyAssessment(urgencyLevel, urgencyReason))
                .possibleDiseases(possibleDiseases)
                .precautions(precautions)
                .facilityType(primaryFacility)
                .requiredFacilityTypes(requiredFacilityTypes)
                .recommendedServices(recommendedServices)
                .confidence(confidence)
                .explanation(explanation)
                .build();
    }
}
