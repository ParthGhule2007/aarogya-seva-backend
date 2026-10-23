package com.ruralhealthcare.symptom.service;

import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.domain.Symptom;

public final class SymptomMapper {

    private SymptomMapper() {
    }

    public static SymptomDto toDto(Symptom symptom) {
        if (symptom == null) {
            return null;
        }

        return SymptomDto.builder()
                .id(symptom.getId())
                .caseId(symptom.getCaseId())
                .symptomName(symptom.getSymptomName())
                .severity(symptom.getSeverity())
                .severityDescription(symptom.getSeverity() != null ? symptom.getSeverity().getDescription() : null)
                .durationDays(symptom.getDurationDays())
                .description(symptom.getDescription())
                .bodySite(symptom.getBodySite())
                .createdAt(symptom.getCreatedAt())
                .updatedAt(symptom.getUpdatedAt())
                .build();
    }
}
