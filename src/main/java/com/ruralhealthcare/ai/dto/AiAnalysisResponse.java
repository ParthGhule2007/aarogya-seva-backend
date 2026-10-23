package com.ruralhealthcare.ai.dto;

import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AiAnalysisResponse {

    private UUID id;
    private UUID caseId;
    private String modelVersion;
    private ProcessingStatus processingStatus;
    private RiskAssessment riskAssessment;
    private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
    private List<String> precautions = new ArrayList<>();
    private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
    private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
    private UrgencyAssessment urgency;
    private Double confidence;
    private String explanation;
    private List<FacilityRecommendationDto> recommendedFacilities = new ArrayList<>();
    private String facilityMatchingNote;
    private Instant processedAt;

    public AiAnalysisResponse() {
    }

    public AiAnalysisResponse(UUID id, UUID caseId, String modelVersion, ProcessingStatus processingStatus,
                              RiskAssessment riskAssessment, List<DiseasePrediction> possibleDiseases,
                              List<String> precautions, Set<MedicalServiceCode> recommendedServices,
                              Set<FacilityType> requiredFacilityTypes, UrgencyAssessment urgency,
                              Double confidence, String explanation,
                              List<FacilityRecommendationDto> recommendedFacilities,
                              String facilityMatchingNote, Instant processedAt) {
        this.id = id;
        this.caseId = caseId;
        this.modelVersion = modelVersion;
        this.processingStatus = processingStatus;
        this.riskAssessment = riskAssessment;
        this.possibleDiseases = possibleDiseases != null ? possibleDiseases : new ArrayList<>();
        this.precautions = precautions != null ? precautions : new ArrayList<>();
        this.recommendedServices = recommendedServices != null ? recommendedServices : new HashSet<>();
        this.requiredFacilityTypes = requiredFacilityTypes != null ? requiredFacilityTypes : new HashSet<>();
        this.urgency = urgency;
        this.confidence = confidence;
        this.explanation = explanation;
        this.recommendedFacilities = recommendedFacilities != null ? recommendedFacilities : new ArrayList<>();
        this.facilityMatchingNote = facilityMatchingNote;
        this.processedAt = processedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Set<FacilityType> getRequiredFacilityTypes() {
        return requiredFacilityTypes;
    }

    public void setRequiredFacilityTypes(Set<FacilityType> requiredFacilityTypes) {
        this.requiredFacilityTypes = requiredFacilityTypes;
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

    public List<FacilityRecommendationDto> getRecommendedFacilities() {
        return recommendedFacilities;
    }

    public void setRecommendedFacilities(List<FacilityRecommendationDto> recommendedFacilities) {
        this.recommendedFacilities = recommendedFacilities;
    }

    public String getFacilityMatchingNote() {
        return facilityMatchingNote;
    }

    public void setFacilityMatchingNote(String facilityMatchingNote) {
        this.facilityMatchingNote = facilityMatchingNote;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public static class Builder {
        private UUID id;
        private UUID caseId;
        private String modelVersion;
        private ProcessingStatus processingStatus;
        private RiskAssessment riskAssessment;
        private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
        private List<String> precautions = new ArrayList<>();
        private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
        private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
        private UrgencyAssessment urgency;
        private Double confidence;
        private String explanation;
        private List<FacilityRecommendationDto> recommendedFacilities = new ArrayList<>();
        private String facilityMatchingNote;
        private Instant processedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

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

        public Builder recommendedFacilities(List<FacilityRecommendationDto> recommendedFacilities) {
            this.recommendedFacilities = recommendedFacilities;
            return this;
        }

        public Builder facilityMatchingNote(String facilityMatchingNote) {
            this.facilityMatchingNote = facilityMatchingNote;
            return this;
        }

        public Builder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public AiAnalysisResponse build() {
            return new AiAnalysisResponse(id, caseId, modelVersion, processingStatus, riskAssessment,
                    possibleDiseases, precautions, recommendedServices, requiredFacilityTypes,
                    urgency, confidence, explanation, recommendedFacilities, facilityMatchingNote, processedAt);
        }
    }
}
