package com.ruralhealthcare.calling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 30)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 10)
    private Language language;

    @Column(name = "twilio_call_sid", length = 100)
    private String twilioCallSid;

    @Column(name = "ultravox_call_id", length = 100)
    private String ultravoxCallId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CallStatus status = CallStatus.INITIATED;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected CallSession() {
    }

    public CallSession(String phoneNumber, CallType callType, Language language) {
        this.id = UUID.randomUUID();
        this.phoneNumber = phoneNumber;
        this.callType = callType;
        this.language = language;
        this.status = CallStatus.INITIATED;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getTwilioCallSid() {
        return twilioCallSid;
    }

    public void setTwilioCallSid(String twilioCallSid) {
        this.twilioCallSid = twilioCallSid;
    }

    public String getUltravoxCallId() {
        return ultravoxCallId;
    }

    public void setUltravoxCallId(String ultravoxCallId) {
        this.ultravoxCallId = ultravoxCallId;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
        if (status == CallStatus.IN_PROGRESS && this.startedAt == null) {
            this.startedAt = Instant.now();
        } else if ((status == CallStatus.COMPLETED || status == CallStatus.FAILED ||
                   status == CallStatus.BUSY || status == CallStatus.NO_ANSWER ||
                   status == CallStatus.CANCELLED) && this.endedAt == null) {
            this.endedAt = Instant.now();
        }
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
