package com.ruralhealthcare.calling.dto;

import com.ruralhealthcare.calling.domain.CallSession;
import com.ruralhealthcare.calling.domain.CallStatus;
import com.ruralhealthcare.calling.domain.CallType;
import com.ruralhealthcare.calling.domain.Language;

import java.time.Instant;
import java.util.UUID;

public class CallResponse {

    private UUID callSessionId;
    private String phoneNumber;
    private CallType callType;
    private Language language;
    private CallStatus status;
    private String twilioCallSid;
    private String ultravoxCallId;
    private String message;
    private Instant createdAt;

    public CallResponse() {
    }

    public static CallResponse fromSession(CallSession session, String message) {
        CallResponse response = new CallResponse();
        response.callSessionId = session.getId();
        response.phoneNumber = session.getPhoneNumber();
        response.callType = session.getCallType();
        response.language = session.getLanguage();
        response.status = session.getStatus();
        response.twilioCallSid = session.getTwilioCallSid();
        response.ultravoxCallId = session.getUltravoxCallId();
        response.message = message;
        response.createdAt = session.getCreatedAt();
        return response;
    }

    public UUID getCallSessionId() {
        return callSessionId;
    }

    public UUID getSessionId() {
        return callSessionId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public CallType getCallType() {
        return callType;
    }

    public Language getLanguage() {
        return language;
    }

    public CallStatus getStatus() {
        return status;
    }

    public String getTwilioCallSid() {
        return twilioCallSid;
    }

    public String getUltravoxCallId() {
        return ultravoxCallId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
