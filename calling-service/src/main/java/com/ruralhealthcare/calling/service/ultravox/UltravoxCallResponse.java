package com.ruralhealthcare.calling.service.ultravox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UltravoxCallResponse {

    private String callId;
    private String joinUrl;

    public UltravoxCallResponse() {
    }

    public UltravoxCallResponse(String callId, String joinUrl) {
        this.callId = callId;
        this.joinUrl = joinUrl;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }
}
