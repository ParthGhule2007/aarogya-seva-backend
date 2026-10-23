package com.ruralhealthcare.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.ai.config.AiConfigProperties;
import com.ruralhealthcare.ai.domain.ProcessingStatus;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.domain.UrgencyLevel;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.dto.RiskAssessment;
import com.ruralhealthcare.ai.dto.UrgencyAssessment;
import com.ruralhealthcare.ai.exception.AiServiceException;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalAiModelClientTest {

    private ExternalAiModelClient client;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private AiConfigProperties properties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = new AiConfigProperties();
        properties.setBaseUrl("http://localhost:9000");
        properties.setPredictPath("/predict");
        properties.setApiKey("test-ai-key-123");
        properties.setVersion("model-v1.0");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new ExternalAiModelClient(properties, restClientBuilder.build());
    }

    @Test
    @DisplayName("Should successfully parse external AI response")
    void testPredictSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        AiPredictionResponse mockResponse = AiPredictionResponse.builder()
                .caseId(caseId)
                .modelVersion("model-v1.0")
                .processingStatus(ProcessingStatus.COMPLETED)
                .riskAssessment(new RiskAssessment(82.0, RiskLevel.HIGH))
                .possibleDiseases(List.of(new DiseasePrediction("Viral Fever", 0.85)))
                .precautions(List.of("Hydration"))
                .recommendedServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .requiredFacilityTypes(Set.of(FacilityType.PHC))
                .urgency(new UrgencyAssessment(UrgencyLevel.URGENT, "High fever"))
                .confidence(0.88)
                .explanation("Model screened high fever")
                .build();

        mockServer.expect(requestTo("http://localhost:9000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key-123"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.HIGH, response.getRiskAssessment().getRiskLevel());
        assertEquals(FacilityType.PHC, response.getFacilityType());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceException when external service returns 500 error")
    void testPredictServerError() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        mockServer.expect(requestTo("http://localhost:9000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Model inference failed\"}"));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("AI_SERVICE_UNAVAILABLE", ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceException when external service returns 400 Bad Request")
    void testPredictClientError() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        mockServer.expect(requestTo("http://localhost:9000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"Malformed input\"}"));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should fallback to Gradio triage API when direct /predict returns HTTP 405 Method Not Allowed")
    void testPredictHttp405FallbackToGradioSuccess() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(68, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Persistent Fever", SymptomSeverity.SEVERE, 4, "High temperature", "WHOLE_BODY"))
        );

        String ssePayload = "event: complete\n" +
                "data: [\"### CRITICAL RISK - 95.0\\n\\n**Urgency:** `EMERGENCY`\\n**Clinical Urgency Reason:** Severe critical fever\\n**Confidence Score:** `92%`\", " +
                "\"#### Diagnoses:\\n- **Pneumonia** (Probability: `85%`)\", " +
                "\"#### Routing:\\n- **Target Facilities:** `TERTIARY HOSPITAL`\\n- **Required Departments / Services:** `EMERGENCY`\\n\\n#### Immediate Precautions:\\n- Immediate hospital admission\", " +
                "\"#### Clinical Physician Narrative:\\n> Patient exhibits severe respiratory symptoms.\"]\n\n";

        // 1. Direct /predict returns 405 Method Not Allowed
        mockServer.expect(requestTo("http://localhost:9000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.METHOD_NOT_ALLOWED).body("<!doctype html><html>405 Method Not Allowed</html>"));

        // 2. Gradio POST call returns event_id
        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"event_id\": \"evt-405-fallback\"}", MediaType.APPLICATION_JSON));

        // 3. Gradio SSE stream read returns triage panels
        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-405-fallback"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ssePayload, MediaType.TEXT_EVENT_STREAM));

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.CRITICAL, response.getRiskAssessment().getRiskLevel());
        assertEquals(95.0, response.getRiskAssessment().getRiskScore());
        assertEquals(UrgencyLevel.EMERGENCY, response.getUrgency().getLevel());
        assertEquals(0.92, response.getConfidence(), 0.001);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should execute Gradio triage directly when predictPath is configured as Gradio endpoint")
    void testPredictGradioDirectSuccess() {
        properties.setPredictPath("/gradio_api/call/run_clinical_triage");

        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(35, Gender.FEMALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.MODERATE, 2, "Body ache", "BODY"))
        );

        String ssePayload = "event: complete\n" +
                "data: [\"### MODERATE RISK - 50.0\\n\\n**Urgency:** `PRIORITY`\\n**Clinical Urgency Reason:** Mild fever\\n**Confidence Score:** `85%`\", " +
                "\"#### Diagnoses:\\n- **Viral Infection** (Probability: `75%`)\", " +
                "\"#### Routing:\\n- **Target Facilities:** `CHC`\\n- **Required Departments / Services:** `GENERAL OPD`\\n\\n#### Immediate Precautions:\\n- Rest and hydration\", " +
                "\"#### Clinical Physician Narrative:\\n> Patient needs evaluation at community center.\"]\n\n";

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key-123"))
                .andRespond(withSuccess("{\"event_id\": \"evt-direct-gradio\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-direct-gradio"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-ai-key-123"))
                .andRespond(withSuccess(ssePayload, MediaType.TEXT_EVENT_STREAM));

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.MODERATE, response.getRiskAssessment().getRiskLevel());
        assertEquals(50.0, response.getRiskAssessment().getRiskScore());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should securely resolve token from HF_TOKEN system property and send Bearer on both Gradio POST and SSE GET")
    void testPredictGradioResolvesTokenFromHfTokenSystemPropertyWhenApiKeyBlank() {
        String testHfToken = "hf_mockTokenForTestingPurposesOnly123456789";
        System.setProperty("HF_TOKEN", testHfToken);
        try {
            properties.setPredictPath("/gradio_api/call/run_clinical_triage");
            properties.setApiKey(""); // Empty apiKey to force HF_TOKEN resolution

            UUID caseId = UUID.randomUUID();
            AiPredictionRequest request = new AiPredictionRequest(
                    caseId,
                    new AiPredictionRequest.PatientInfo(35, Gender.FEMALE),
                    List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.MODERATE, 2, "Body ache", "BODY"))
            );

            String ssePayload = "event: complete\n" +
                    "data: [\"### LOW RISK - 20.0\\n\\n**Urgency:** `ROUTINE`\\n**Clinical Urgency Reason:** Mild\\n**Confidence Score:** `90%`\", " +
                    "\"#### Diagnoses:\\n- **Normal Cold** (Probability: `80%`)\", " +
                    "\"#### Routing:\\n- **Target Facilities:** `PHC`\\n- **Required Departments / Services:** `GENERAL OPD`\\n\\n#### Immediate Precautions:\\n- Rest\", " +
                    "\"#### Clinical Physician Narrative:\\n> Patient has mild symptoms.\"]\n\n";

            mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Authorization", "Bearer " + testHfToken))
                    .andRespond(withSuccess("{\"event_id\": \"evt-token-env-check\"}", MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-token-env-check"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Authorization", "Bearer " + testHfToken))
                    .andRespond(withSuccess(ssePayload, MediaType.TEXT_EVENT_STREAM));

            AiPredictionResponse response = client.predict(request);

            assertNotNull(response);
            assertEquals(caseId, response.getCaseId());
            assertEquals(RiskLevel.LOW, response.getRiskAssessment().getRiskLevel());
            mockServer.verify();
        } finally {
            System.clearProperty("HF_TOKEN");
        }
    }

    @Test
    @DisplayName("Should throw AiServiceException when direct predict returns 405 and Gradio fallback fails")
    void testPredictHttp405FallbackGradioFailure() {
        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        // Direct returns 405
        mockServer.expect(requestTo("http://localhost:9000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.METHOD_NOT_ALLOWED).body("<!doctype html><html>405 Method Not Allowed</html>"));

        // Gradio endpoint returns 500
        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Gradio server error"));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("AI_SERVICE_UNAVAILABLE", ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AI_SERVICE_UNAVAILABLE when Gradio POST returns HTTP 429 ZeroGPU quota exhaustion")
    void testPredictGradioZeroGpuQuotaErrorOnPostThrowsServiceUnavailable() {
        properties.setPredictPath("/gradio_api/call/run_clinical_triage");

        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\": \"You have exceeded your ZeroGPU runs limit\"}"));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("AI_SERVICE_UNAVAILABLE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("ZeroGPU"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceException when Gradio SSE stream is empty or invalid")
    void testPredictGradioInvalidSseStream() {
        properties.setPredictPath("/gradio_api/call/run_clinical_triage");

        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"event_id\": \"evt-empty-stream\"}", MediaType.APPLICATION_JSON));

        // Returns empty body
        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-empty-stream"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.TEXT_EVENT_STREAM));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("INVALID_AI_RESPONSE", ex.getErrorCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AI_SERVICE_UNAVAILABLE when Gradio SSE stream contains error payload like ZeroGPU limit")
    void testPredictGradioSseStreamErrorPayload() {
        properties.setPredictPath("/gradio_api/call/run_clinical_triage");

        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(40, Gender.MALE),
                List.of(new AiPredictionRequest.SymptomInfo("Fever", SymptomSeverity.SEVERE, 3, "High fever", "HEAD"))
        );

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"event_id\": \"evt-quota-error\"}", MediaType.APPLICATION_JSON));

        String errorSse = "event: error\ndata: {\"error\": \"You have exceeded your ZeroGPU runs limit\", \"title\": \"ZeroGPU quota exceeded\"}\n\n";
        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-quota-error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(errorSse, MediaType.TEXT_EVENT_STREAM));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> client.predict(request));
        assertEquals("AI_SERVICE_UNAVAILABLE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("ZeroGPU"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should format multiple symptoms and bilingual names into Gradio data array payload")
    void testPredictMultipleSymptomsWithMultilingualNamesAndDataArrayPayload() {
        properties.setPredictPath("/gradio_api/call/run_clinical_triage");

        UUID caseId = UUID.randomUUID();
        AiPredictionRequest request = new AiPredictionRequest(
                caseId,
                new AiPredictionRequest.PatientInfo(45, Gender.FEMALE),
                List.of(
                        new AiPredictionRequest.SymptomInfo("Fever (बुखार / ताप)", SymptomSeverity.SEVERE, 3, "High persistent fever", "HEAD"),
                        new AiPredictionRequest.SymptomInfo("Cough (खांसी / खोकला)", SymptomSeverity.MODERATE, 4, "Dry cough", "CHEST"),
                        new AiPredictionRequest.SymptomInfo("Headache (सिरदर्द / डोकेदुखी)", SymptomSeverity.MILD, 2, "Throbbing headache", "HEAD"),
                        new AiPredictionRequest.SymptomInfo("Chest Pain (छाती में दर्द)", SymptomSeverity.CRITICAL, 1, "Sharp tightness", "CHEST")
                )
        );

        String ssePayload = "event: complete\n" +
                "data: [\"### HIGH RISK - 78.0\\n\\n**Urgency:** `URGENT`\\n**Clinical Urgency Reason:** Multimorbid presentation with severe fever\\n**Confidence Score:** `91%`\", " +
                "\"#### Diagnoses:\\n- **Lower Respiratory Tract Infection** (Probability: `82%`)\", " +
                "\"#### Routing:\\n- **Target Facilities:** `DISTRICT HOSPITAL`\\n- **Required Departments / Services:** `GENERAL OPD`, `EMERGENCY`\\n\\n#### Immediate Precautions:\\n- Urgent physician evaluation\", " +
                "\"#### Clinical Physician Narrative:\\n> Patient shows multiple systemic symptoms with high risk classification.\"]\n\n";

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.data[0]").value(45))
                .andExpect(jsonPath("$.data[1]").value("FEMALE"))
                .andExpect(jsonPath("$.data[2]").value("Fever")) // cleaned from Fever (बुखार / ताप)
                .andExpect(jsonPath("$.data[3]").value("SEVERE"))
                .andExpect(jsonPath("$.data[4]").value(3))
                .andExpect(jsonPath("$.data[5]").value("Cough")) // cleaned from Cough (खांसी / खोकला)
                .andExpect(jsonPath("$.data[6]").value("MODERATE"))
                .andExpect(jsonPath("$.data[7]").value(4))
                .andExpect(jsonPath("$.data[8]").value("Headache")) // cleaned from Headache (सिरदर्द / डोकेदुखी)
                .andExpect(jsonPath("$.data[9]").value("MILD"))
                .andExpect(jsonPath("$.data[10]").value(2))
                .andExpect(jsonPath("$.data[11]").value(org.hamcrest.Matchers.containsString("Chest Pain")))
                .andRespond(withSuccess("{\"event_id\": \"evt-multisymptom-test\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:9000/gradio_api/call/run_clinical_triage/evt-multisymptom-test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ssePayload, MediaType.TEXT_EVENT_STREAM));

        AiPredictionResponse response = client.predict(request);

        assertNotNull(response);
        assertEquals(caseId, response.getCaseId());
        assertEquals(RiskLevel.HIGH, response.getRiskAssessment().getRiskLevel());
        assertEquals(78.0, response.getRiskAssessment().getRiskScore());
        assertEquals(UrgencyLevel.URGENT, response.getUrgency().getLevel());
        assertEquals(1, response.getPossibleDiseases().size());
        assertEquals("Lower Respiratory Tract Infection", response.getPossibleDiseases().get(0).getCategory());
        mockServer.verify();
    }
}
