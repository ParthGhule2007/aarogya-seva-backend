package com.ruralhealthcare.ai.dto;

import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;

import java.util.List;
import java.util.UUID;

public class AiPredictionRequest {

    private UUID caseId;
    private PatientInfo patient;
    private List<SymptomInfo> symptoms;

    public AiPredictionRequest() {
    }

    public AiPredictionRequest(UUID caseId, PatientInfo patient, List<SymptomInfo> symptoms) {
        this.caseId = caseId;
        this.patient = patient;
        this.symptoms = symptoms;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public PatientInfo getPatient() {
        return patient;
    }

    public void setPatient(PatientInfo patient) {
        this.patient = patient;
    }

    public List<SymptomInfo> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<SymptomInfo> symptoms) {
        this.symptoms = symptoms;
    }

    public static class PatientInfo {
        private Integer age;
        private Gender gender;

        public PatientInfo() {
        }

        public PatientInfo(Integer age, Gender gender) {
            this.age = age;
            this.gender = gender;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
        }
    }

    public static class SymptomInfo {
        private String name;
        private SymptomSeverity severity;
        private int durationDays;
        private String description;
        private String bodySite;

        public SymptomInfo() {
        }

        public SymptomInfo(String name, SymptomSeverity severity, int durationDays, String description, String bodySite) {
            this.name = name;
            this.severity = severity;
            this.durationDays = durationDays;
            this.description = description;
            this.bodySite = bodySite;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SymptomSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SymptomSeverity severity) {
            this.severity = severity;
        }

        public int getDurationDays() {
            return durationDays;
        }

        public void setDurationDays(int durationDays) {
            this.durationDays = durationDays;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getBodySite() {
            return bodySite;
        }

        public void setBodySite(String bodySite) {
            this.bodySite = bodySite;
        }
    }
}
