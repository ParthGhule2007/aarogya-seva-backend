package com.ruralhealthcare.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.ai.config.AiConfigProperties;
import com.ruralhealthcare.ai.dto.AiPredictionRequest;
import com.ruralhealthcare.ai.dto.AiPredictionResponse;
import com.ruralhealthcare.ai.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production-ready HTTP client implementation of {@link AiPredictionClient} connecting
 * to the external AI/ML model screening API.
 * Supports both direct REST (/predict) and Gradio SSE tele-triage endpoints.
 */
public class ExternalAiModelClient implements AiPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalAiModelClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient;
    private final AiConfigProperties properties;

    public ExternalAiModelClient(AiConfigProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        RestClient.Builder builder = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        String token = resolveAuthToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    public ExternalAiModelClient(AiConfigProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * Securely resolves the Hugging Face / AI authentication token without exposing it in logs.
     * Looks first in {@link AiConfigProperties#getApiKey()}, then environment variable HF_TOKEN,
     * then system property HF_TOKEN.
     */
    private String resolveAuthToken() {
        if (properties != null && properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            return properties.getApiKey().trim();
        }
        String envToken = System.getenv("HF_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }
        String sysProp = System.getProperty("HF_TOKEN");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        return null;
    }

    @Override
    public AiPredictionResponse predict(AiPredictionRequest request) {
        String url = properties.getPredictPath();
        log.info("Sending AI screening request for case [{}] to external AI model at {}{}",
                request.getCaseId(), properties.getBaseUrl(), url);

        if (url != null && (url.contains("gradio") || url.contains("run_clinical_triage"))) {
            return callGradioTriage(request);
        }

        try {
            var postSpec = restClient.post()
                    .uri(url);
            String token = resolveAuthToken();
            if (token != null && !token.isBlank()) {
                postSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            AiPredictionResponse response = postSpec
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        log.warn("External AI model returned status [{}]: {}", res.getStatusCode(), body);
                        if (res.getStatusCode().value() == 404 || res.getStatusCode().value() == 405) {
                            throw new RestClientResponseException(
                                    "Endpoint not supported: " + res.getStatusCode(),
                                    res.getStatusCode().value(),
                                    res.getStatusText().toString(),
                                    res.getHeaders(),
                                    body.getBytes(),
                                    null
                            );
                        }
                        if (res.getStatusCode().is5xxServerError()) {
                            throw AiServiceException.serviceUnavailable(
                                     "External AI model service is currently unavailable: " + res.getStatusCode(), null);
                        } else {
                            throw AiServiceException.invalidResponse(
                                    "External AI model rejected request with status: " + res.getStatusCode() + " - " + body);
                        }
                    })
                    .body(AiPredictionResponse.class);

            if (response != null && response.getRiskAssessment() != null && response.getRiskAssessment().getRiskLevel() != null) {
                log.info("Received valid prediction from external AI model version [{}] for case [{}]",
                        response.getModelVersion(), request.getCaseId());
                return response;
            }

            // If response is null or missing fields, attempt Gradio fallback
            log.info("Direct prediction returned incomplete response, falling back to Gradio triage...");
            return callGradioTriage(request);

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 405) {
                log.info("Direct /predict returned status {}. Falling back to Gradio clinical triage API...", ex.getStatusCode());
                return callGradioTriage(request);
            }
            log.error("HTTP error response from external AI service: Status {}, Body {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw AiServiceException.invalidResponse(
                    "External AI model communication error: " + ex.getMessage());
        } catch (ResourceAccessException ex) {
            log.error("Network or timeout error while contacting external AI service at {}{}: {}",
                    properties.getBaseUrl(), url, ex.getMessage());
            throw AiServiceException.serviceUnavailable(
                    "Connection to external AI service timed out or failed: " + ex.getMessage(), ex);
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("405") || msg.contains("404") || msg.contains("HTML") || msg.contains("<!doctype") || msg.contains("text/html")) {
                log.info("Direct /predict failed with non-JSON response ({}). Falling back to Gradio clinical triage API...", msg);
                return callGradioTriage(request);
            }
            log.error("Unexpected error during external AI prediction for case [{}]: {}",
                    request.getCaseId(), ex.getMessage(), ex);
            throw AiServiceException.processingFailed(
                    "Unexpected failure during AI prediction execution: " + ex.getMessage(), ex);
        }
    }

    private AiPredictionResponse callGradioTriage(AiPredictionRequest request) {
        log.info("Executing Gradio clinical tele-triage for case [{}] at {}", request.getCaseId(), properties.getBaseUrl());

        int age = (request.getPatient() != null && request.getPatient().getAge() != null)
                ? request.getPatient().getAge() : 35;
        String gender = (request.getPatient() != null && request.getPatient().getGender() != null)
                ? request.getPatient().getGender().name() : "MALE";

        List<AiPredictionRequest.SymptomInfo> symptoms = request.getSymptoms() != null
                ? request.getSymptoms() : Collections.emptyList();

        String s1Name = symptoms.size() > 0 ? cleanSymptomName(symptoms.get(0).getName()) : "Fever";
        if (s1Name.isBlank()) s1Name = "Fever";
        String s1Sev = symptoms.size() > 0 && symptoms.get(0).getSeverity() != null ? symptoms.get(0).getSeverity().name() : "MODERATE";
        int s1Dur = symptoms.size() > 0 && symptoms.get(0).getDurationDays() > 0 ? symptoms.get(0).getDurationDays() : 2;

        String s2Name = symptoms.size() > 1 ? cleanSymptomName(symptoms.get(1).getName()) : "";
        String s2Sev = symptoms.size() > 1 && symptoms.get(1).getSeverity() != null ? symptoms.get(1).getSeverity().name() : "MILD";
        int s2Dur = symptoms.size() > 1 && symptoms.get(1).getDurationDays() > 0 ? symptoms.get(1).getDurationDays() : 0;

        String s3Name = symptoms.size() > 2 ? cleanSymptomName(symptoms.get(2).getName()) : "";
        String s3Sev = symptoms.size() > 2 && symptoms.get(2).getSeverity() != null ? symptoms.get(2).getSeverity().name() : "MILD";
        int s3Dur = symptoms.size() > 2 && symptoms.get(2).getDurationDays() > 0 ? symptoms.get(2).getDurationDays() : 0;

        StringBuilder notesBuilder = new StringBuilder();
        if (symptoms.size() > 3) {
            notesBuilder.append("Additional symptoms: ");
            for (int i = 3; i < symptoms.size(); i++) {
                AiPredictionRequest.SymptomInfo s = symptoms.get(i);
                if (i > 3) notesBuilder.append(", ");
                notesBuilder.append(cleanSymptomName(s.getName()))
                        .append(" (")
                        .append(s.getSeverity() != null ? s.getSeverity().name() : "MODERATE")
                        .append(", ")
                        .append(s.getDurationDays())
                        .append("d)");
            }
            notesBuilder.append(". ");
        }

        for (AiPredictionRequest.SymptomInfo s : symptoms) {
            if (s.getDescription() != null && !s.getDescription().isBlank()) {
                if (notesBuilder.length() > 0 && !notesBuilder.toString().endsWith(" ")) {
                    notesBuilder.append("; ");
                }
                notesBuilder.append(s.getDescription().trim());
            }
        }
        String notes = notesBuilder.toString().trim();
        if (notes.isBlank()) {
            notes = "Recorded clinical symptoms for tele-triage screening";
        }

        // Gradio /call/run_clinical_triage requires { "data": [age, gender, s1_name, s1_sev, s1_dur, s2_name, s2_sev, s2_dur, s3_name, s3_sev, s3_dur, notes] }
        List<Object> gradioData = Arrays.asList(
                age,
                gender != null ? gender : "MALE",
                s1Name,
                s1Sev,
                s1Dur,
                s2Name,
                s2Sev,
                s2Dur,
                s3Name,
                s3Sev,
                s3Dur,
                notes
        );
        Map<String, Object> gradioPayload = Collections.singletonMap("data", gradioData);

        try {
            String postEndpoint = (properties.getPredictPath() != null
                    && (properties.getPredictPath().contains("gradio") || properties.getPredictPath().contains("run_clinical_triage")))
                    ? properties.getPredictPath().trim()
                    : "/gradio_api/call/run_clinical_triage";
            String token = resolveAuthToken();

            var postSpec = restClient.post()
                    .uri(postEndpoint)
                    .contentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.isBlank()) {
                postSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            JsonNode eventResponse = postSpec
                    .body(gradioPayload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        log.error("Gradio triage POST request failed with status [{}]: {}", res.getStatusCode(), body);
                        if (res.getStatusCode().value() == 429 || body.contains("ZeroGPU") || body.contains("quota exceeded") || body.contains("runs limit")) {
                            throw AiServiceException.serviceUnavailable(
                                    "Hugging Face ZeroGPU quota exhausted on inference POST request: " + body, null);
                        }
                        if (res.getStatusCode().is5xxServerError()) {
                            throw AiServiceException.serviceUnavailable(
                                    "Gradio AI triage service is currently unavailable: " + res.getStatusCode(), null);
                        } else {
                            throw AiServiceException.invalidResponse(
                                    "Gradio AI triage rejected request with status: " + res.getStatusCode() + " - " + body);
                        }
                    })
                    .body(JsonNode.class);

            if (eventResponse == null || !eventResponse.has("event_id")) {
                throw AiServiceException.invalidResponse("Gradio API did not return an event_id: " + eventResponse);
            }

            String eventId = eventResponse.get("event_id").asText();
            if (eventId == null || eventId.isBlank()) {
                throw AiServiceException.invalidResponse("Gradio API returned empty event_id");
            }
            log.info("Received Gradio event_id [{}] for case [{}], reading SSE stream...", eventId, request.getCaseId());

            String sseEndpoint = postEndpoint + "/" + eventId;
            var sseSpec = restClient.get()
                    .uri(sseEndpoint)
                    .accept(MediaType.TEXT_EVENT_STREAM, MediaType.ALL);
            if (token != null && !token.isBlank()) {
                sseSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            String sseOutput = sseSpec
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        log.error("Gradio SSE stream request failed with status [{}]: {}", res.getStatusCode(), body);
                        if (res.getStatusCode().value() == 429 || body.contains("ZeroGPU") || body.contains("quota exceeded") || body.contains("runs limit")) {
                            throw AiServiceException.serviceUnavailable(
                                    "Hugging Face ZeroGPU quota exhausted on SSE stream request: " + body, null);
                        }
                        if (res.getStatusCode().is5xxServerError()) {
                            throw AiServiceException.serviceUnavailable(
                                    "Gradio AI SSE stream is currently unavailable: " + res.getStatusCode(), null);
                        } else {
                            throw AiServiceException.invalidResponse(
                                    "Gradio AI SSE stream returned status: " + res.getStatusCode() + " - " + body);
                        }
                    })
                    .body(String.class);

            if (sseOutput == null || sseOutput.isBlank()) {
                throw AiServiceException.invalidResponse("Empty SSE stream received from Gradio endpoint for event: " + eventId);
            }

            List<String> dataArray = extractGradioSseData(sseOutput);
            return GradioTriageParser.parseGradioSseData(request.getCaseId(), properties.getVersion(), dataArray);

        } catch (ResourceAccessException ex) {
            log.error("Network or timeout error while executing Gradio tele-triage for case [{}]: {}",
                    request.getCaseId(), ex.getMessage());
            throw AiServiceException.serviceUnavailable(
                    "Connection to Gradio AI service timed out or failed: " + ex.getMessage(), ex);
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Gradio tele-triage execution failed for case [{}]: {}", request.getCaseId(), ex.getMessage(), ex);
            throw AiServiceException.processingFailed("Gradio tele-triage execution failed: " + ex.getMessage(), ex);
        }
    }

    private List<String> extractGradioSseData(String sseOutput) {
        List<String> dataList = new ArrayList<>();
        String[] lines = sseOutput.split("\n");
        String lastDataJson = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String payload = trimmed.substring(5).trim();
                if (!payload.equalsIgnoreCase("null") && !payload.isEmpty()) {
                    lastDataJson = payload;
                }
            }
        }

        if (lastDataJson == null) {
            throw AiServiceException.invalidResponse("Could not extract triage data panels from Gradio SSE stream: missing data payload");
        }

        try {
            JsonNode root = objectMapper.readTree(lastDataJson);
            if (root.has("error")) {
                String errMsg = root.get("error").asText();
                if (errMsg.contains("ZeroGPU") || errMsg.contains("quota") || errMsg.contains("runs limit")) {
                    log.error("Hugging Face ZeroGPU quota exhausted in SSE stream: {}", errMsg);
                    throw AiServiceException.serviceUnavailable("Hugging Face ZeroGPU quota exhausted: " + errMsg, null);
                }
                log.error("Gradio SSE stream returned error payload: {}", errMsg);
                throw AiServiceException.serviceUnavailable("External AI model service error: " + errMsg, null);
            }
            if (root.isArray()) {
                for (JsonNode elem : root) {
                    dataList.add(elem.asText());
                }
            }
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gradio SSE data json: {}", lastDataJson, e);
            throw AiServiceException.invalidResponse("Malformed JSON in Gradio SSE data payload: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw AiServiceException.invalidResponse("Could not extract triage data panels from Gradio SSE stream");
        }
        return dataList;
    }

    private String cleanSymptomName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int paren = raw.indexOf('(');
        if (paren > 0) {
            return raw.substring(0, paren).trim();
        }
        return raw.trim();
    }
}
