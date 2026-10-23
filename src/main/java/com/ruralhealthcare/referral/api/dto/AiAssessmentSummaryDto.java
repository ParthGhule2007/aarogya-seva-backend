package com.ruralhealthcare.referral.api.dto;

import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AiAssessmentSummaryDto {

    private UUID aiPredictionId;
    private String modelVersion;
    private Double riskScore;
    private RiskLevel riskLevel;
    private UrgencyLevel urgencyLevel;
    private String urgencyReason;
    private Double confidence;
    private String explanation;
    private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
    private List<String> precautions = new ArrayList<>();
    private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
    private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
    private Instant processedAt;

    public AiAssessmentSummaryDto() {
    }

    public AiAssessmentSummaryDto(UUID aiPredictionId, String modelVersion, Double riskScore,
                                  RiskLevel riskLevel, UrgencyLevel urgencyLevel, String urgencyReason,
                                  Double confidence, String explanation,
                                  List<DiseasePrediction> possibleDiseases, List<String> precautions,
                                  Set<MedicalServiceCode> recommendedServices,
                                  Set<FacilityType> requiredFacilityTypes, Instant processedAt) {
        this.aiPredictionId = aiPredictionId;
        this.modelVersion = modelVersion;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.urgencyLevel = urgencyLevel;
        this.urgencyReason = urgencyReason;
        this.confidence = confidence;
        this.explanation = explanation;
        this.possibleDiseases = possibleDiseases != null ? possibleDiseases : new ArrayList<>();
        this.precautions = precautions != null ? precautions : new ArrayList<>();
        this.recommendedServices = recommendedServices != null ? recommendedServices : new HashSet<>();
        this.requiredFacilityTypes = requiredFacilityTypes != null ? requiredFacilityTypes : new HashSet<>();
        this.processedAt = processedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getAiPredictionId() {
        return aiPredictionId;
    }

    public void setAiPredictionId(UUID aiPredictionId) {
        this.aiPredictionId = aiPredictionId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getUrgencyReason() {
        return urgencyReason;
    }

    public void setUrgencyReason(String urgencyReason) {
        this.urgencyReason = urgencyReason;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<DiseasePrediction> getPossibleDiseases() {
        return possibleDiseases;
    }

    public void setPossibleDiseases(List<DiseasePrediction> possibleDiseases) {
        this.possibleDiseases = possibleDiseases;
    }

    public List<String> getPrecautions() {
        return precautions;
    }

    public void setPrecautions(List<String> precautions) {
        this.precautions = precautions;
    }

    public Set<MedicalServiceCode> getRecommendedServices() {
        return recommendedServices;
    }

    public void setRecommendedServices(Set<MedicalServiceCode> recommendedServices) {
        this.recommendedServices = recommendedServices;
    }

    public Set<FacilityType> getRequiredFacilityTypes() {
        return requiredFacilityTypes;
    }

    public void setRequiredFacilityTypes(Set<FacilityType> requiredFacilityTypes) {
        this.requiredFacilityTypes = requiredFacilityTypes;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public static class Builder {
        private UUID aiPredictionId;
        private String modelVersion;
        private Double riskScore;
        private RiskLevel riskLevel;
        private UrgencyLevel urgencyLevel;
        private String urgencyReason;
        private Double confidence;
        private String explanation;
        private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
        private List<String> precautions = new ArrayList<>();
        private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
        private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
        private Instant processedAt;

        public Builder aiPredictionId(UUID aiPredictionId) {
            this.aiPredictionId = aiPredictionId;
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public Builder riskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder urgencyLevel(UrgencyLevel urgencyLevel) {
            this.urgencyLevel = urgencyLevel;
            return this;
        }

        public Builder urgencyReason(String urgencyReason) {
            this.urgencyReason = urgencyReason;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Builder possibleDiseases(List<DiseasePrediction> possibleDiseases) {
            this.possibleDiseases = possibleDiseases;
            return this;
        }

        public Builder precautions(List<String> precautions) {
            this.precautions = precautions;
            return this;
        }

        public Builder recommendedServices(Set<MedicalServiceCode> recommendedServices) {
            this.recommendedServices = recommendedServices;
            return this;
        }

        public Builder requiredFacilityTypes(Set<FacilityType> requiredFacilityTypes) {
            this.requiredFacilityTypes = requiredFacilityTypes;
            return this;
        }

        public Builder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public AiAssessmentSummaryDto build() {
            return new AiAssessmentSummaryDto(aiPredictionId, modelVersion, riskScore, riskLevel,
                    urgencyLevel, urgencyReason, confidence, explanation, possibleDiseases, precautions,
                    recommendedServices, requiredFacilityTypes, processedAt);
        }
    }
}
