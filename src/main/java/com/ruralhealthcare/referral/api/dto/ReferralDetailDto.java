package com.ruralhealthcare.referral.api.dto;

import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;

import java.util.ArrayList;
import java.util.List;

public class ReferralDetailDto {

    private ReferralResponseDto referral;
    private PatientDto patient;
    private HealthcareCaseDto healthcareCase;
    private List<SymptomDto> symptoms = new ArrayList<>();
    private AiAssessmentSummaryDto aiAssessment;
    private FacilityDto facility;

    public ReferralDetailDto() {
    }

    public ReferralDetailDto(ReferralResponseDto referral, PatientDto patient,
                             HealthcareCaseDto healthcareCase, List<SymptomDto> symptoms,
                             AiAssessmentSummaryDto aiAssessment, FacilityDto facility) {
        this.referral = referral;
        this.patient = patient;
        this.healthcareCase = healthcareCase;
        this.symptoms = symptoms != null ? symptoms : new ArrayList<>();
        this.aiAssessment = aiAssessment;
        this.facility = facility;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReferralResponseDto getReferral() {
        return referral;
    }

    public void setReferral(ReferralResponseDto referral) {
        this.referral = referral;
    }

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }

    public HealthcareCaseDto getHealthcareCase() {
        return healthcareCase;
    }

    public void setHealthcareCase(HealthcareCaseDto healthcareCase) {
        this.healthcareCase = healthcareCase;
    }

    public List<SymptomDto> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<SymptomDto> symptoms) {
        this.symptoms = symptoms;
    }

    public AiAssessmentSummaryDto getAiAssessment() {
        return aiAssessment;
    }

    public void setAiAssessment(AiAssessmentSummaryDto aiAssessment) {
        this.aiAssessment = aiAssessment;
    }

    public FacilityDto getFacility() {
        return facility;
    }

    public void setFacility(FacilityDto facility) {
        this.facility = facility;
    }

    public static class Builder {
        private ReferralResponseDto referral;
        private PatientDto patient;
        private HealthcareCaseDto healthcareCase;
        private List<SymptomDto> symptoms = new ArrayList<>();
        private AiAssessmentSummaryDto aiAssessment;
        private FacilityDto facility;

        public Builder referral(ReferralResponseDto referral) {
            this.referral = referral;
            return this;
        }

        public Builder patient(PatientDto patient) {
            this.patient = patient;
            return this;
        }

        public Builder healthcareCase(HealthcareCaseDto healthcareCase) {
            this.healthcareCase = healthcareCase;
            return this;
        }

        public Builder symptoms(List<SymptomDto> symptoms) {
            this.symptoms = symptoms;
            return this;
        }

        public Builder aiAssessment(AiAssessmentSummaryDto aiAssessment) {
            this.aiAssessment = aiAssessment;
            return this;
        }

        public Builder facility(FacilityDto facility) {
            this.facility = facility;
            return this;
        }

        public ReferralDetailDto build() {
            return new ReferralDetailDto(referral, patient, healthcareCase, symptoms, aiAssessment, facility);
        }
    }
}
