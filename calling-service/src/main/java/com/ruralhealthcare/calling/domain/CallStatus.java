package com.ruralhealthcare.calling.domain;

public enum CallStatus {
    INITIATED,
    RINGING,
    IN_PROGRESS,
    COMPLETED,
    BUSY,
    NO_ANSWER,
    FAILED,
    CANCELLED;

    public boolean isActive() {
        return this == INITIATED || this == RINGING || this == IN_PROGRESS;
    }
}
