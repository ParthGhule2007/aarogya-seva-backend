package com.ruralhealthcare.sos.notification;

/**
 * Result of sending a notification via a notification channel.
 */
public class NotificationResult {

    private final boolean successful;
    private final String messageId;
    private final String failureReason;

    private NotificationResult(boolean successful, String messageId, String failureReason) {
        this.successful = successful;
        this.messageId = messageId;
        this.failureReason = failureReason;
    }

    public static NotificationResult success(String messageId) {
        return new NotificationResult(true, messageId, null);
    }

    public static NotificationResult failure(String failureReason) {
        return new NotificationResult(false, null, failureReason);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
