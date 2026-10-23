package com.ruralhealthcare.calling.service.ultravox;

public interface UltravoxClient {

    /**
     * Creates an active Ultravox call session with Twilio media streaming medium configured.
     *
     * @param request configuration including prompt, language, voice, and tools
     * @return UltravoxCallResponse containing unique callId and WebSocket joinUrl (wss://...)
     */
    UltravoxCallResponse createCallSession(UltravoxCallRequest request);

    boolean isMock();
}
