package com.ruralhealthcare.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruralhealthcare.ai.domain.RiskLevel;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskAssessment {

    private Double riskScore;
    private RiskLevel riskLevel;

    public RiskAssessment() {
    }

    public RiskAssessment(Double riskScore, RiskLevel riskLevel) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
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
}
