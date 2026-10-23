package com.ruralhealthcare.calling.service.ultravox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "calling.ultravox.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockUltravoxClient implements UltravoxClient {

    private static final Logger log = LoggerFactory.getLogger(MockUltravoxClient.class);
    private volatile UltravoxCallRequest lastRequest;

    @Override
    public UltravoxCallResponse createCallSession(UltravoxCallRequest request) {
        this.lastRequest = request;
        String mockCallId = UUID.randomUUID().toString();
        String mockJoinUrl = "wss://mock.ultravox.ai/api/call/" + mockCallId + "/join";

        log.info("[MockUltravoxClient] MOCK ULTRAVOX SESSION: model [{}], lang [{}], voice [{}], vad [{}], tools [{}]. Created mock Call ID [{}] with joinUrl [{}]",
                request.getModel(), request.getLanguageHint(), request.getVoice(),
                request.getVadSettings(), request.getSelectedTools() != null ? request.getSelectedTools().size() : 0,
                mockCallId, mockJoinUrl);

        return new UltravoxCallResponse(mockCallId, mockJoinUrl);
    }

    public UltravoxCallRequest getLastRequest() {
        return lastRequest;
    }

    @Override
    public boolean isMock() {
        return true;
    }
}
