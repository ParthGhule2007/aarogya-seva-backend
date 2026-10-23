package com.ruralhealthcare.sos.domain;

/**
 * Status lifecycle of an SOS Emergency event.
 */
public enum SosEventStatus {
    TRIGGERED,
    PROCESSING,
    NOTIFICATIONS_SENT,
    PARTIALLY_SENT,
    FAILED,
    RESOLVED;

    public boolean isTerminal() {
        return this == NOTIFICATIONS_SENT || this == PARTIALLY_SENT || this == FAILED || this == RESOLVED;
    }
}
