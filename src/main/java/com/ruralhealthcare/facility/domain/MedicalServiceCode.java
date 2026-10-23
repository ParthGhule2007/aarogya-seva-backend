package com.ruralhealthcare.facility.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MedicalServiceCode {
    GENERAL_OPD("General Outpatient Care"),
    EMERGENCY("Emergency and Trauma Care"),
    MATERNAL_HEALTH("Maternal and Obstetrics Care"),
    PEDIATRIC_CARE("Pediatric and Child Health"),
    INFECTIOUS_DISEASE("Infectious Disease Triage and Management"),
    SURGICAL_CARE("General and Minor Surgery"),
    DIAGNOSTIC_LAB("Diagnostic Pathology and Radiology"),
    DENTAL_CARE("Dental OPD"),
    OPHTHALMOLOGY("Eye Care"),
    ICU_BEDS("Intensive Care Unit Beds"),
    INPATIENT_BEDS("Inpatient Beds"),
    RADIOLOGY_XRAY("Radiology and X-Ray"),
    BLOOD_STORAGE_BANK("Blood Storage Bank"),
    TELEMEDICINE("Telemedicine Consultation"),
    SURGICAL_UNIT("Surgical Unit");

    private final String description;

    MedicalServiceCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static MedicalServiceCode fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        for (MedicalServiceCode code : values()) {
            if (code.name().equalsIgnoreCase(normalized)) {
                return code;
            }
        }
        if ("SURGERY".equals(normalized) || "SURGICAL".equals(normalized)) return SURGICAL_CARE;
        if ("RADIOLOGY".equals(normalized) || "XRAY".equals(normalized) || "X_RAY".equals(normalized)) return RADIOLOGY_XRAY;
        if ("BLOOD_BANK".equals(normalized)) return BLOOD_STORAGE_BANK;
        if ("ICU".equals(normalized)) return ICU_BEDS;
        if ("IPD".equals(normalized)) return INPATIENT_BEDS;
        if ("OPD".equals(normalized)) return GENERAL_OPD;
        if ("LAB".equals(normalized) || "PATHOLOGY".equals(normalized)) return DIAGNOSTIC_LAB;

        return null;
    }
}
