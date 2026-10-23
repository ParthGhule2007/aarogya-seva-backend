package com.healthcare.chatbotservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.chatbotservice.dto.request.AiChatRequest;
import com.healthcare.chatbotservice.dto.response.AiChatResponse;
import com.healthcare.chatbotservice.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);
    private static final String AI_CHAT_ENDPOINT = "/api/v1/ai/chat";
    private static final String GRADIO_CHAT_ENDPOINT = "/gradio_api/call/chat_with_clinical_ai";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient aiWebClient;
    private final int timeoutSeconds;
    private final boolean mockMode;

    public AiModelService(WebClient aiWebClient, @Value("${ai.timeout-seconds:10}") int timeoutSeconds) {
        this(aiWebClient, timeoutSeconds, false);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiModelService(WebClient aiWebClient,
                          @Value("${ai.timeout-seconds:10}") int timeoutSeconds,
                          @Value("${ai.mock-mode:false}") boolean mockMode) {
        this.aiWebClient = aiWebClient;
        this.timeoutSeconds = timeoutSeconds;
        this.mockMode = mockMode;
    }

    /**
     * Sends the user's age, current message, and conversation history to the AI Model Service
     * and returns the generated text response.
     *
     * @param age                 the user's age (required for model interpretation)
     * @param currentMessage      the user's current message
     * @param conversationHistory previous conversation messages in chronological order
     * @return AI text response
     */
    public String getAiResponse(Integer age, String currentMessage, List<AiChatRequest.AiHistoryItem> conversationHistory) {
        if (mockMode) {
            log.info("Mock AI mode active: Generating simulated response for age={}, message='{}'", age, currentMessage);
            return String.format(
                    "Thank you for sharing your symptoms. Given your age (%d), can you tell me how long you have been experiencing '%s' and if you have any other symptoms like nausea, fever, or dizziness?",
                    age, currentMessage);
        }

        AiChatRequest request = AiChatRequest.builder()
                .age(age)
                .message(currentMessage)
                .conversationHistory(conversationHistory)
                .build();

        log.debug("Sending chat request to AI Model Service at {}: age={}, historySize={}",
                AI_CHAT_ENDPOINT, age, conversationHistory != null ? conversationHistory.size() : 0);

        try {
            AiChatResponse response = aiWebClient.post()
                    .uri(AI_CHAT_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null || !response.isSuccess() || response.getData() == null || response.getData().getResponse() == null) {
                log.error("AI service returned an invalid or unsuccessful response: {}", response);
                throw new AiServiceException("AI service returned an invalid or unsuccessful response");
            }

            return response.getData().getResponse();
        } catch (AiServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 405) {
                log.info("Direct {} returned {}, calling Hugging Face Gradio endpoint {}...",
                        AI_CHAT_ENDPOINT, e.getStatusCode().value(), GRADIO_CHAT_ENDPOINT);
                return callGradioChat(age, currentMessage, conversationHistory);
            }
            log.error("AI service returned error HTTP status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException("AI service returned error HTTP status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (WebClientRequestException e) {
            log.error("AI service is unreachable or connection failed: {}", e.getMessage());
            throw new AiServiceException("AI service is unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error communicating with AI service: {}", e.getMessage());
            throw new AiServiceException("AI service communication failed or timed out: " + e.getMessage(), e);
        }
    }

    private String callGradioChat(Integer age, String currentMessage, List<AiChatRequest.AiHistoryItem> conversationHistory) {
        try {
            List<Map<String, Object>> gradioHistory = new ArrayList<>();
            if (conversationHistory != null) {
                for (AiChatRequest.AiHistoryItem item : conversationHistory) {
                    if (item.getRole() == null || item.getContent() == null) continue;
                    Map<String, Object> msg = new LinkedHashMap<>();
                    msg.put("role", item.getRole().toLowerCase(Locale.ROOT));
                    msg.put("metadata", null);
                    Map<String, String> textPart = new LinkedHashMap<>();
                    textPart.put("text", item.getContent());
                    textPart.put("type", "text");
                    msg.put("content", Collections.singletonList(textPart));
                    msg.put("options", null);
                    gradioHistory.add(msg);
                }
            }

            List<Object> dataPayload = Arrays.asList(
                    currentMessage,
                    gradioHistory,
                    "Auto-Detect",
                    "General Emergency & Tele-Triage"
            );
            Map<String, Object> gradioReq = Collections.singletonMap("data", dataPayload);

            String eventJson = aiWebClient.post()
                    .uri(GRADIO_CHAT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(gradioReq)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (eventJson == null || eventJson.isBlank()) {
                throw new AiServiceException("Gradio chat API returned empty event response");
            }

            JsonNode eventNode = objectMapper.readTree(eventJson);
            if (!eventNode.has("event_id")) {
                throw new AiServiceException("Gradio chat API did not return event_id: " + eventJson);
            }
            String eventId = eventNode.get("event_id").asText();
            log.info("Received Gradio chat event_id [{}], reading SSE stream...", eventId);

            String sseStream = aiWebClient.get()
                    .uri(GRADIO_CHAT_ENDPOINT + "/" + eventId)
                    .accept(MediaType.TEXT_EVENT_STREAM, MediaType.ALL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (sseStream == null || sseStream.isBlank()) {
                throw new AiServiceException("Empty SSE stream from Gradio chat endpoint");
            }

            String lastDataJson = null;
            for (String line : sseStream.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("data:")) {
                    String payload = trimmed.substring(5).trim();
                    if (!payload.isEmpty() && !payload.equalsIgnoreCase("null")) {
                        lastDataJson = payload;
                    }
                }
            }

            if (lastDataJson == null) {
                throw new AiServiceException("No data frame found in Gradio chat SSE response");
            }

            JsonNode dataArr = objectMapper.readTree(lastDataJson);
            // Gradio returns ["", [ {role:"user",...}, {role:"assistant", content:[{"text":"..."}]} ]]
            if (dataArr.isArray() && dataArr.size() >= 2 && dataArr.get(1).isArray()) {
                JsonNode chatHistoryNode = dataArr.get(1);
                for (int i = chatHistoryNode.size() - 1; i >= 0; i--) {
                    JsonNode turn = chatHistoryNode.get(i);
                    String role = turn.has("role") ? turn.get("role").asText("") : "";
                    if ("assistant".equalsIgnoreCase(role) && turn.has("content")) {
                        JsonNode contentNode = turn.get("content");
                        if (contentNode.isTextual()) {
                            return contentNode.asText();
                        } else if (contentNode.isArray() && contentNode.size() > 0) {
                            JsonNode firstBlock = contentNode.get(0);
                            if (firstBlock.isTextual()) {
                                return firstBlock.asText();
                            } else if (firstBlock.has("text")) {
                                return firstBlock.get("text").asText();
                            }
                        }
                    }
                }
            }

            throw new AiServiceException("Could not parse assistant message from Gradio chat output");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gradio chat communication failed: {}", e.getMessage(), e);
            throw new AiServiceException("Gradio chat communication failed: " + e.getMessage(), e);
        }
    }
}
