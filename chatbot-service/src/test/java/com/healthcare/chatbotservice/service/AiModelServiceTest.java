package com.healthcare.chatbotservice.service;

import com.healthcare.chatbotservice.dto.request.AiChatRequest;
import com.healthcare.chatbotservice.exception.AiServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiModelServiceTest {

    private MockWebServer mockWebServer;
    private AiModelService aiModelService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        // 2 second timeout for testing
        aiModelService = new AiModelService(webClient, 2);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("9. AI service success: parses response and returns text")
    void testGetAiResponse_success() throws InterruptedException {
        String jsonResponse = """
                {
                  "success": true,
                  "data": {
                    "response": "Can you tell me how long you have been experiencing the pain?"
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        List<AiChatRequest.AiHistoryItem> history = List.of(
                new AiChatRequest.AiHistoryItem("USER", "Feeling dizzy"),
                new AiChatRequest.AiHistoryItem("ASSISTANT", "Did you drink water?")
        );

        String result = aiModelService.getAiResponse(24, "I have stomach pain", history);

        assertThat(result).isEqualTo("Can you tell me how long you have been experiencing the pain?");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/ai/chat");
        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("\"age\":24");
        assertThat(requestBody).contains("\"message\":\"I have stomach pain\"");
        assertThat(requestBody).contains("\"role\":\"USER\"");
        assertThat(requestBody).contains("\"content\":\"Feeling dizzy\"");
    }

    @Test
    @DisplayName("10a. AI service failure: HTTP 500 error throws AiServiceException")
    void testGetAiResponse_serverError_throwsAiServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Model inference failed\"}"));

        assertThatThrownBy(() -> aiModelService.getAiResponse(30, "Help", List.of()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("AI service returned error HTTP status 500");
    }

    @Test
    @DisplayName("10b. AI service timeout throws AiServiceException")
    void testGetAiResponse_timeout_throwsAiServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\": true, \"data\": {\"response\": \"Late response\"}}")
                .setBodyDelay(4, TimeUnit.SECONDS)); // delay exceeds 2 second timeout

        assertThatThrownBy(() -> aiModelService.getAiResponse(30, "Help", List.of()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("AI service communication failed or timed out");
    }

    @Test
    @DisplayName("10c. AI service returns invalid response (success=false) throws AiServiceException")
    void testGetAiResponse_invalidResponse_throwsAiServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\": false, \"message\": \"Inference failed\"}"));

        assertThatThrownBy(() -> aiModelService.getAiResponse(30, "Help", List.of()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("invalid or unsuccessful response");
    }

    @Test
    @DisplayName("AI service in mockMode returns simulated response without external HTTP call")
    void testGetAiResponse_mockMode() {
        AiModelService mockModeService = new AiModelService(null, 5, true);
        String response = mockModeService.getAiResponse(24, "stomach pain", List.of());
        assertThat(response).contains("24");
        assertThat(response).contains("stomach pain");
    }
}
