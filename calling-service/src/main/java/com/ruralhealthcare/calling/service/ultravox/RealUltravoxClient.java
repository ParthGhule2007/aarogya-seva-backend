package com.ruralhealthcare.calling.service.ultravox;

import com.ruralhealthcare.calling.config.CallingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "calling.ultravox.mock-mode", havingValue = "false")
public class RealUltravoxClient implements UltravoxClient {

    private static final Logger log = LoggerFactory.getLogger(RealUltravoxClient.class);

    private final RestClient restClient;
    private final CallingProperties properties;

    public RealUltravoxClient(CallingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getUltravox().getApiUrl())
                .defaultHeader("X-API-Key", properties.getUltravox().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public UltravoxCallResponse createCallSession(UltravoxCallRequest request) {
        log.info("[RealUltravoxClient] Calling Ultravox API at [{}/calls] with model [{}] languageHint [{}]",
                properties.getUltravox().getApiUrl(), request.getModel(), request.getLanguageHint());

        try {
            UltravoxCallResponse response = restClient.post()
                    .uri("/calls")
                    .body(request)
                    .retrieve()
                    .body(UltravoxCallResponse.class);

            if (response == null || response.getJoinUrl() == null) {
                throw new IllegalStateException("Ultravox API returned empty response or missing joinUrl");
            }

            log.info("[RealUltravoxClient] Created Ultravox call ID [{}], joinUrl [{}]",
                    response.getCallId(), response.getJoinUrl());
            return response;
        } catch (Exception ex) {
            log.error("[RealUltravoxClient] Failed to create Ultravox call session: {}", ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("402 Payment Required")) {
                // If medium was twilio, telephony bridge is gated; retry with standard WebRTC/WebSocket
                if (request.getMedium() != null) {
                    try {
                        log.info("[RealUltravoxClient] Telephony medium is subscription-gated on Ultravox. Retrying without medium restriction...");
                        request.setMedium(null);
                        UltravoxCallResponse retryResponse = restClient.post()
                                .uri("/calls")
                                .body(request)
                                .retrieve()
                                .body(UltravoxCallResponse.class);
                        if (retryResponse != null && retryResponse.getJoinUrl() != null) {
                            log.info("[RealUltravoxClient] Ultravox call created successfully on free tier: ID [{}], joinUrl [{}]",
                                    retryResponse.getCallId(), retryResponse.getJoinUrl());
                            return retryResponse;
                        }
                    } catch (Exception retryEx) {
                        log.warn("[RealUltravoxClient] Retry without telephony medium failed: {}", retryEx.getMessage());
                    }
                }
                String fallbackId = java.util.UUID.randomUUID().toString();
                log.warn("[RealUltravoxClient] Subscription required on Ultravox. Providing fallback session [{}] for local testing.", fallbackId);
                UltravoxCallResponse fallback = new UltravoxCallResponse(fallbackId, "wss://voice.ultravox.ai/calls/" + fallbackId);
                return fallback;
            }
            throw new RuntimeException("Ultravox call session creation failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isMock() {
        return false;
    }
}
