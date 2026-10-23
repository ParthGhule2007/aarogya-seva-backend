package com.ruralhealthcare.ai.client;

import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GradioTriageParserTest {

    @Test
    void parseLiveGradioSseOutputSuccessfully() {
        String panel1 = "### <span style=\"background-color: #FF3B5C; color: white; padding: 6px 14px; border-radius: 8px; font-weight: bold;\">CRITICAL RISK - 98.5 / 100</span>\n\n**Urgency:** `EMERGENCY`  \n**Clinical Urgency Reason:** Life-threatening acute presentation requiring immediate stabilization.  \n**Confidence Score:** `93.0%`";
        String panel2 = "#### 🩺 Suspected Differential Diagnoses:\n- **Enteric Fever (Typhoid) / Dengue** (Probability: `78%`)\n- **Acute Lower Respiratory Tract Infection / Pneumonia** (Probability: `85%`)\n- **Acute Cardiopulmonary / Cerebrovascular Event** (Probability: `88%`)\n- **Infectious Diseases** (Probability: `85%`)";
        String panel3 = "#### 🏥 Hospital Routing & Facility Level (IPHS Standard):\n- **Target Facilities:** `TERTIARY HOSPITAL`, `DISTRICT HOSPITAL`\n- **Required Departments / Services:** `EMERGENCY`, `ICU BEDS`, `INPATIENT BEDS`, `DIAGNOSTIC LAB`, `RADIOLOGY XRAY`, `BLOOD STORAGE BANK`\n\n#### ⚠️ Immediate Precautions:\n- Consider empirical antibiotic therapy\n- Monitor for signs of sepsis or respiratory failure\n- Monitor platelet counts, maintain strict oral rehydration with ORS.\n- Check SpO2 saturation immediately; keep patient in an upright sitting posture.\n- Do NOT delay transport. Call emergency ambulance (108) immediately for rapid transit.";
        String panel4 = "#### 📋 Clinical Physician Narrative:\n> Patient classified under Geriatric (68y MALE) cohort presenting with Bukhar (Persistent High Fever) (SEVERE, 4d), Sans fulna (Shortness of breath) (MODERATE, 2d). Assigned Risk Score 98.5/100 (CRITICAL) based on symptom severity, duration threshold, and epidemiological vulnerability indicators. Routed to TERTIARY HOSPITAL with priority services: EMERGENCY, ICU BEDS, INPATIENT BEDS.";

        UUID caseId = UUID.randomUUID();
        AiPredictionResponse response = GradioTriageParser.parseGradioSseData(
                caseId, "aarogya-clinical-engine-v2.0", List.of(panel1, panel2, panel3, panel4));

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals("aarogya-clinical-engine-v2.0", response.getModelVersion());

        // Risk & Urgency
        assertNotNull(response.getRiskAssessment());
        assertEquals(RiskLevel.CRITICAL, response.getRiskAssessment().getRiskLevel());
        assertEquals(98.5, response.getRiskAssessment().getRiskScore());

        assertNotNull(response.getUrgency());
        assertEquals(UrgencyLevel.EMERGENCY, response.getUrgency().getLevel());
        assertTrue(response.getUrgency().getReason().contains("Life-threatening"));

        assertEquals(0.93, response.getConfidence(), 0.001);

        // Diseases
        assertEquals(4, response.getPossibleDiseases().size());
        assertEquals("Enteric Fever (Typhoid) / Dengue", response.getPossibleDiseases().get(0).getCategory());
        assertEquals(0.78, response.getPossibleDiseases().get(0).getProbability(), 0.001);

        // Facilities & Services
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.TERTIARY_HOSPITAL));
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.DISTRICT_HOSPITAL));

        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.EMERGENCY));
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.ICU_BEDS));
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.INPATIENT_BEDS));
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.DIAGNOSTIC_LAB));
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.RADIOLOGY_XRAY));
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.BLOOD_STORAGE_BANK));

        // Precautions
        assertEquals(5, response.getPrecautions().size());
        assertTrue(response.getPrecautions().get(0).contains("Consider empirical antibiotic therapy"));

        // Explanation
        assertTrue(response.getExplanation().contains("Patient classified under Geriatric"));
    }

    @Test
    void parseGradioEmptyDataThrowsException() {
        UUID caseId = UUID.randomUUID();
        com.ruralhealthcare.ai.exception.AiServiceException ex = assertThrows(
                com.ruralhealthcare.ai.exception.AiServiceException.class,
                () -> GradioTriageParser.parseGradioSseData(caseId, "model-v1", List.of())
        );
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
    }

    @Test
    void parseGradioMissingRiskThrowsException() {
        UUID caseId = UUID.randomUUID();
        String panel1 = "No risk score in this corrupted text\n**Urgency:** `EMERGENCY`\n**Confidence Score:** `90%`";
        String panel2 = "Diagnoses";
        String panel3 = "Facilities";
        String panel4 = "Narrative text";

        com.ruralhealthcare.ai.exception.AiServiceException ex = assertThrows(
                com.ruralhealthcare.ai.exception.AiServiceException.class,
                () -> GradioTriageParser.parseGradioSseData(caseId, "model-v1", List.of(panel1, panel2, panel3, panel4))
        );
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("risk assessment"));
    }

    @Test
    void parseGradioMissingUrgencyThrowsException() {
        UUID caseId = UUID.randomUUID();
        String panel1 = "HIGH RISK - 72.0\nMissing urgency tag\n**Confidence Score:** `85%`";
        String panel2 = "Diagnoses";
        String panel3 = "Facilities";
        String panel4 = "Narrative text";

        com.ruralhealthcare.ai.exception.AiServiceException ex = assertThrows(
                com.ruralhealthcare.ai.exception.AiServiceException.class,
                () -> GradioTriageParser.parseGradioSseData(caseId, "model-v1", List.of(panel1, panel2, panel3, panel4))
        );
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("urgency level"));
    }

    @Test
    void parseGradioMissingConfidenceThrowsException() {
        UUID caseId = UUID.randomUUID();
        String panel1 = "LOW RISK - 25.0\n**Urgency:** `ROUTINE`\nMissing confidence";
        String panel2 = "Diagnoses";
        String panel3 = "Facilities";
        String panel4 = "Narrative text";

        com.ruralhealthcare.ai.exception.AiServiceException ex = assertThrows(
                com.ruralhealthcare.ai.exception.AiServiceException.class,
                () -> GradioTriageParser.parseGradioSseData(caseId, "model-v1", List.of(panel1, panel2, panel3, panel4))
        );
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("confidence score"));
    }

    @Test
    void parseGradioEmptyNarrativeThrowsException() {
        UUID caseId = UUID.randomUUID();
        String panel1 = "MODERATE RISK - 55.0\n**Urgency:** `PRIORITY`\n**Confidence Score:** `80%`";
        String panel2 = "Diagnoses";
        String panel3 = "Facilities";
        String panel4 = "   ";

        com.ruralhealthcare.ai.exception.AiServiceException ex = assertThrows(
                com.ruralhealthcare.ai.exception.AiServiceException.class,
                () -> GradioTriageParser.parseGradioSseData(caseId, "model-v1", List.of(panel1, panel2, panel3, panel4))
        );
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("narrative"));
    }

    @Test
    void parseGradioModerateRiskTriageSuccessfully() {
        UUID caseId = UUID.randomUUID();
        String panel1 = "MODERATE RISK - 52.0\n**Urgency:** `PRIORITY`\n**Clinical Urgency Reason:** Mild dehydration\n**Confidence Score:** `84.5%`";
        String panel2 = "#### Differential Diagnoses:\n- **Gastroenteritis** (Probability: `70%`)";
        String panel3 = "#### Routing:\n- **Target Facilities:** `CHC`\n- **Required Departments / Services:** `GENERAL OPD`\n\n#### Immediate Precautions:\n- ORS hydration";
        String panel4 = "#### Clinical Physician Narrative:\n> Patient needs evaluation at community center.";

        AiPredictionResponse response = GradioTriageParser.parseGradioSseData(
                caseId, "model-v1", List.of(panel1, panel2, panel3, panel4));

        assertNotNull(response);
        assertEquals(RiskLevel.MODERATE, response.getRiskAssessment().getRiskLevel());
        assertEquals(52.0, response.getRiskAssessment().getRiskScore());
        assertEquals(UrgencyLevel.PRIORITY, response.getUrgency().getLevel());
        assertEquals(FacilityType.CHC, response.getFacilityType());
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.GENERAL_OPD));
    }
}
