package com.ruralhealthcare.calling.dto;

import com.ruralhealthcare.calling.domain.CallStatus;

import java.util.UUID;

public class CallStatusResponse {

    private UUID callSessionId;
    private CallStatus status;
    private String twilioCallSid;
    private String twilioStatus;
    private Integer errorCode;
    private String errorMessage;
    private String readableStatus;
    private boolean active;

    public CallStatusResponse() {
    }

    public CallStatusResponse(UUID callSessionId, CallStatus status, String twilioCallSid, String twilioStatus,
                              Integer errorCode, String errorMessage, String readableStatus, boolean active) {
        this.callSessionId = callSessionId;
        this.status = status;
        this.twilioCallSid = twilioCallSid;
        this.twilioStatus = twilioStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.readableStatus = readableStatus;
        this.active = active;
    }

    public UUID getCallSessionId() {
        return callSessionId;
    }

    public void setCallSessionId(UUID callSessionId) {
        this.callSessionId = callSessionId;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public String getTwilioCallSid() {
        return twilioCallSid;
    }

    public void setTwilioCallSid(String twilioCallSid) {
        this.twilioCallSid = twilioCallSid;
    }

    public String getTwilioStatus() {
        return twilioStatus;
    }

    public void setTwilioStatus(String twilioStatus) {
        this.twilioStatus = twilioStatus;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getReadableStatus() {
        return readableStatus;
    }

    public void setReadableStatus(String readableStatus) {
        this.readableStatus = readableStatus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
