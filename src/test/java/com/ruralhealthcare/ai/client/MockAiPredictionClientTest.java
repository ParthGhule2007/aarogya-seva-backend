package com.ruralhealthcare.ai.client;

import com.ruralhealthcare.ai.config.AiConfigProperties;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiPredictionClientTest {

    private MockAiPredictionClient client;
    private AiConfigProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiConfigProperties();
        properties.setVersion("health-model-v1");
        client = new MockAiPredictionClient(properties);
    }

    @Test
    @DisplayName("Should return CRITICAL risk and EMERGENCY urgency when critical symptoms are present")
    void testPredictCriticalSymptoms() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(55, Gender.MALE),
                List.of(
                        new AiPredictionRequest.SymptomInfo("Chest Pain", SymptomSeverity.CRITICAL, 1, "Acute crushing retrosternal pain", "CHEST"),
                        new AiPredictionRequest.SymptomInfo("Dyspnea", SymptomSeverity.SEVERE, 1, "Shortness of breath", "CHEST")
                )
        );

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals("health-model-v1", response.getModelVersion());
        assertEquals(ProcessingStatus.COMPLETED, response.getProcessingStatus());
        assertEquals(RiskLevel.CRITICAL, response.getRiskAssessment().getRiskLevel());
        assertEquals(UrgencyLevel.EMERGENCY, response.getUrgency().getLevel());
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.EMERGENCY));
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.DISTRICT_HOSPITAL));
        assertNotNull(response.getFacilityType());
        assertFalse(response.getPossibleDiseases().isEmpty());
        assertFalse(response.getPrecautions().isEmpty());
        assertTrue(response.getExplanation().contains("DEVELOPMENT / TESTING ONLY"));
    }

    @Test
    @DisplayName("Should return HIGH risk and URGENT urgency when severe symptoms are present")
    void testPredictSevereSymptoms() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(30, Gender.FEMALE),
                List.of(
                        new AiPredictionRequest.SymptomInfo("High Grade Fever", SymptomSeverity.SEVERE, 4, "High temperature with rigors", "WHOLE_BODY")
                )
        );

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(RiskLevel.HIGH, response.getRiskAssessment().getRiskLevel());
        assertEquals(UrgencyLevel.URGENT, response.getUrgency().getLevel());
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.INFECTIOUS_DISEASE));
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.PHC));
    }

    @Test
    @DisplayName("Should return MODERATE risk and PRIORITY urgency when moderate symptoms are present")
    void testPredictModerateSymptoms() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(22, Gender.OTHER),
                List.of(
                        new AiPredictionRequest.SymptomInfo("Persistent Cough", SymptomSeverity.MODERATE, 3, "Productive cough", "CHEST")
                )
        );

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(RiskLevel.MODERATE, response.getRiskAssessment().getRiskLevel());
        assertEquals(UrgencyLevel.PRIORITY, response.getUrgency().getLevel());
        assertTrue(response.getRecommendedServices().contains(MedicalServiceCode.GENERAL_OPD));
    }

    @Test
    @DisplayName("Should return LOW risk and ROUTINE urgency when only mild symptoms are present")
    void testPredictMildSymptoms() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(18, Gender.FEMALE),
                List.of(
                        new AiPredictionRequest.SymptomInfo("Runny Nose", SymptomSeverity.MILD, 2, "Watery nasal discharge", "NOSE")
                )
        );

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(RiskLevel.LOW, response.getRiskAssessment().getRiskLevel());
        assertEquals(UrgencyLevel.ROUTINE, response.getUrgency().getLevel());
        assertTrue(response.getRequiredFacilityTypes().contains(FacilityType.SUB_CENTRE));
    }
}
