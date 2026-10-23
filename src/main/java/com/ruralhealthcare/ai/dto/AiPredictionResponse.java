package com.ruralhealthcare.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPredictionResponse {

    private UUID caseId;
    private String modelVersion;
    private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;
    private RiskAssessment riskAssessment;
    private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
    private List<String> precautions = new ArrayList<>();
    private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
    private FacilityType facilityType;
    private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
    private UrgencyAssessment urgency;
    private Double confidence;
    private String explanation;

    public AiPredictionResponse() {
    }

    public AiPredictionResponse(UUID caseId, String modelVersion, ProcessingStatus processingStatus,
                                RiskAssessment riskAssessment, List<DiseasePrediction> possibleDiseases,
                                List<String> precautions, Set<MedicalServiceCode> recommendedServices,
                                FacilityType facilityType, Set<FacilityType> requiredFacilityTypes,
                                UrgencyAssessment urgency, Double confidence, String explanation) {
        this.caseId = caseId;
        this.modelVersion = modelVersion;
        this.processingStatus = processingStatus;
        this.riskAssessment = riskAssessment;
        this.possibleDiseases = possibleDiseases != null ? possibleDiseases : new ArrayList<>();
        this.precautions = precautions != null ? precautions : new ArrayList<>();
        this.recommendedServices = recommendedServices != null ? recommendedServices : new HashSet<>();
        this.requiredFacilityTypes = requiredFacilityTypes != null ? requiredFacilityTypes : new HashSet<>();
        this.facilityType = facilityType != null ? facilityType
                : (!this.requiredFacilityTypes.isEmpty() ? this.requiredFacilityTypes.iterator().next() : null);
        if (this.facilityType != null && this.requiredFacilityTypes.isEmpty()) {
            this.requiredFacilityTypes.add(this.facilityType);
        }
        this.urgency = urgency;
        this.confidence = confidence;
        this.explanation = explanation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
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

    public FacilityType getFacilityType() {
        if (facilityType != null) {
            return facilityType;
        }
        if (requiredFacilityTypes != null && !requiredFacilityTypes.isEmpty()) {
            return requiredFacilityTypes.iterator().next();
        }
        return null;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
        if (facilityType != null && (requiredFacilityTypes == null || requiredFacilityTypes.isEmpty())) {
            if (this.requiredFacilityTypes == null) {
                this.requiredFacilityTypes = new HashSet<>();
            }
            this.requiredFacilityTypes.add(facilityType);
        }
    }

    public Set<FacilityType> getRequiredFacilityTypes() {
        return requiredFacilityTypes;
    }

    public void setRequiredFacilityTypes(Set<FacilityType> requiredFacilityTypes) {
        this.requiredFacilityTypes = requiredFacilityTypes;
        if (this.facilityType == null && requiredFacilityTypes != null && !requiredFacilityTypes.isEmpty()) {
            this.facilityType = requiredFacilityTypes.iterator().next();
        }
    }

    public UrgencyAssessment getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyAssessment urgency) {
        this.urgency = urgency;
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

    public static class Builder {
        private UUID caseId;
        private String modelVersion;
        private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;
        private RiskAssessment riskAssessment;
        private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
        private List<String> precautions = new ArrayList<>();
        private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
        private FacilityType facilityType;
        private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
        private UrgencyAssessment urgency;
        private Double confidence;
        private String explanation;

        public Builder caseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public Builder processingStatus(ProcessingStatus processingStatus) {
            this.processingStatus = processingStatus;
            return this;
        }

        public Builder riskAssessment(RiskAssessment riskAssessment) {
            this.riskAssessment = riskAssessment;
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

        public Builder facilityType(FacilityType facilityType) {
            this.facilityType = facilityType;
            return this;
        }

        public Builder requiredFacilityTypes(Set<FacilityType> requiredFacilityTypes) {
            this.requiredFacilityTypes = requiredFacilityTypes;
            return this;
        }

        public Builder urgency(UrgencyAssessment urgency) {
            this.urgency = urgency;
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

        public AiPredictionResponse build() {
            return new AiPredictionResponse(caseId, modelVersion, processingStatus, riskAssessment,
                    possibleDiseases, precautions, recommendedServices, facilityType, requiredFacilityTypes,
                    urgency, confidence, explanation);
        }
    }
}
