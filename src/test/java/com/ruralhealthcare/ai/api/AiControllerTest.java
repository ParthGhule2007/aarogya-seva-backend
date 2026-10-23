package com.ruralhealthcare.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiAnalysisResponse;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.ai.service.AiProcessingService;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProcessingService aiProcessingService;

    @Test
    @DisplayName("POST /api/v1/cases/{caseId}/ai-analysis should return 200 OK with screening results")
    void testRunAiAnalysisSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();

        FacilityRecommendationDto facilityDto = FacilityRecommendationDto.builder()
                .facilityId(UUID.randomUUID())
                .name("Shirwal Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .distanceKm(4.2)
                .isOperational(true)
                .availableServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        AiAnalysisResponse response = AiAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(78.5, RiskLevel.HIGH))
                .possibleDiseases(List.of(new DiseasePrediction("Respiratory Infection", 0.72)))
                .precautions(List.of("Maintain hydration", "Seek medical evaluation"))
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD, MedicalServiceCode.DIAGNOSTIC_LAB))
                .requiredFacilityTypes(Set.of(FacilityType.PHC, FacilityType.CHC))
                .urgency(new UrgencyAssessment(UrgencyLevel.URGENT, "Persistent fever with systemic symptoms"))
                .confidence(0.81)
                .explanation("Screening result based on submitted symptoms.")
                .recommendedFacilities(List.of(facilityDto))
                .processedAt(Instant.now())
                .build();

        when(aiProcessingService.analyzeCase(caseId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/cases/{caseId}/ai-analysis", caseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId.toString()))
                .andExpect(jsonPath("$.data.riskAssessment.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.urgency.level").value("URGENT"))
                .andExpect(jsonPath("$.data.possibleDiseases[0].category").value("Respiratory Infection"))
                .andExpect(jsonPath("$.data.recommendedFacilities[0].name").value("Shirwal Primary Health Centre"))
                .andExpect(jsonPath("$.data.recommendedFacilities[0].facilityType").value("PHC"))
                .andExpect(jsonPath("$.data.recommendedFacilities[0].distanceKm").value(4.2));
    }

    @Test
    @DisplayName("GET /api/v1/cases/{caseId}/ai-analysis should return 200 OK when analysis exists")
    void testGetLatestAnalysisSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();

        AiAnalysisResponse response = AiAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .modelVersion("health-model-v1")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(42.0, RiskLevel.MODERATE))
                .urgency(new UrgencyAssessment(UrgencyLevel.PRIORITY, "Moderate symptoms"))
                .confidence(0.79)
                .processedAt(Instant.now())
                .build();

        when(aiProcessingService.getLatestAnalysis(caseId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/cases/{caseId}/ai-analysis", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId.toString()))
                .andExpect(jsonPath("$.data.riskAssessment.riskLevel").value("MODERATE"));
    }

    @Test
    @DisplayName("GET /api/v1/cases/{caseId}/ai-analysis should return 404 NOT_FOUND when no analysis exists")
    void testGetLatestAnalysisNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();

        when(aiProcessingService.getLatestAnalysis(caseId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cases/{caseId}/ai-analysis", caseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
