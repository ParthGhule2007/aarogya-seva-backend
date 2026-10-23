package com.ruralhealthcare.ai.domain;

import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "ai_predictions")
@EntityListeners(AuditingEntityListener.class)
public class AiPredictionResult {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 30)
    private UrgencyLevel urgencyLevel;

    @Column(name = "urgency_reason", columnDefinition = "TEXT")
    private String urgencyReason;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "possible_diseases", columnDefinition = "jsonb")
    private List<DiseasePrediction> possibleDiseases = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "precautions", columnDefinition = "jsonb")
    private List<String> precautions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_services", columnDefinition = "jsonb")
    private Set<MedicalServiceCode> recommendedServices = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_facility_types", columnDefinition = "jsonb")
    private Set<FacilityType> requiredFacilityTypes = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected AiPredictionResult() {
    }

    public AiPredictionResult(UUID id, UUID caseId, String modelVersion, ProcessingStatus processingStatus,
                              Double riskScore, RiskLevel riskLevel, Double confidence,
                              UrgencyLevel urgencyLevel, String urgencyReason, String explanation,
                              List<DiseasePrediction> possibleDiseases, List<String> precautions,
                              Set<MedicalServiceCode> recommendedServices, Set<FacilityType> requiredFacilityTypes,
                              Instant createdAt, Instant processedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.caseId = caseId;
        this.modelVersion = modelVersion;
        this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.COMPLETED;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.urgencyLevel = urgencyLevel;
        this.urgencyReason = urgencyReason;
        this.explanation = explanation;
        this.possibleDiseases = possibleDiseases != null ? possibleDiseases : new ArrayList<>();
        this.precautions = precautions != null ? precautions : new ArrayList<>();
        this.recommendedServices = recommendedServices != null ? recommendedServices : new HashSet<>();
        this.requiredFacilityTypes = requiredFacilityTypes != null ? requiredFacilityTypes : new HashSet<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.processedAt = processedAt != null ? processedAt : Instant.now();
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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
        private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;
        private Double riskScore;
        private RiskLevel riskLevel;
        private Double confidence;
        private UrgencyLevel urgencyLevel;
        private String urgencyReason;
        private String explanation;
        private List<DiseasePrediction> possibleDiseases = new ArrayList<>();
        private List<String> precautions = new ArrayList<>();
        private Set<MedicalServiceCode> recommendedServices = new HashSet<>();
        private Set<FacilityType> requiredFacilityTypes = new HashSet<>();
        private Instant createdAt;
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

        public Builder riskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
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

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public AiPredictionResult build() {
            return new AiPredictionResult(id, caseId, modelVersion, processingStatus, riskScore,
                    riskLevel, confidence, urgencyLevel, urgencyReason, explanation,
                    possibleDiseases, precautions, recommendedServices, requiredFacilityTypes,
                    createdAt, processedAt);
        }
    }
}
