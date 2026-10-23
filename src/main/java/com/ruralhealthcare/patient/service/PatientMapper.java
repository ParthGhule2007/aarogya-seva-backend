package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.patient.api.dto.CaseSymptomDto;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.domain.CaseSymptom;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import com.ruralhealthcare.patient.domain.Patient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static PatientDto toDto(Patient patient) {
        if (patient == null) {
            return null;
        }

        return PatientDto.builder()
                .id(patient.getId())
                .fullName(patient.getFullName())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .age(patient.getAge())
                .contactNumber(patient.getContactNumber())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactNumber(patient.getEmergencyContactNumber())
                .villageTown(patient.getVillageTown())
                .subDistrict(patient.getSubDistrict())
                .district(patient.getDistrict())
                .state(patient.getState())
                .pincode(patient.getPincode())
                .bloodGroup(patient.getBloodGroup())
                .preferredLanguage(patient.getPreferredLanguage())
                .heightCm(patient.getHeightCm())
                .weightKg(patient.getWeightKg())
                .medicalHistory(patient.getMedicalHistory())
                .latitude(patient.getGeoLocation() != null ? patient.getGeoLocation().getLatitude() : null)
                .longitude(patient.getGeoLocation() != null ? patient.getGeoLocation().getLongitude() : null)
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    public static CaseSymptomDto toDto(CaseSymptom symptom) {
        if (symptom == null) {
            return null;
        }

        return CaseSymptomDto.builder()
                .id(symptom.getId())
                .caseId(symptom.getHealthcareCase() != null ? symptom.getHealthcareCase().getId() : null)
                .symptomName(symptom.getSymptomName())
                .severity(symptom.getSeverity())
                .durationDays(symptom.getDurationDays())
                .description(symptom.getDescription())
                .createdAt(symptom.getCreatedAt())
                .build();
    }

    public static HealthcareCaseDto toDto(HealthcareCase healthcareCase) {
        if (healthcareCase == null) {
            return null;
        }

        List<CaseSymptomDto> symptomDtos = healthcareCase.getSymptoms() != null
                ? healthcareCase.getSymptoms().stream()
                .map(PatientMapper::toDto)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return HealthcareCaseDto.builder()
                .id(healthcareCase.getId())
                .caseNumber(healthcareCase.getCaseNumber())
                .patientId(healthcareCase.getPatient() != null ? healthcareCase.getPatient().getId() : null)
                .patientName(healthcareCase.getPatient() != null ? healthcareCase.getPatient().getFullName() : null)
                .patientContactNumber(healthcareCase.getPatient() != null ? healthcareCase.getPatient().getContactNumber() : null)
                .status(healthcareCase.getStatus())
                .priority(healthcareCase.getPriority())
                .intakeChannel(healthcareCase.getIntakeChannel())
                .intakeChannelDescription(healthcareCase.getIntakeChannel() != null ? healthcareCase.getIntakeChannel().getDescription() : null)
                .chiefComplaint(healthcareCase.getChiefComplaint())
                .notes(healthcareCase.getNotes())
                .latitude(healthcareCase.getGeoLocation() != null ? healthcareCase.getGeoLocation().getLatitude() : null)
                .longitude(healthcareCase.getGeoLocation() != null ? healthcareCase.getGeoLocation().getLongitude() : null)
                .symptoms(symptomDtos)
                .createdAt(healthcareCase.getCreatedAt())
                .updatedAt(healthcareCase.getUpdatedAt())
                .build();
    }
}
