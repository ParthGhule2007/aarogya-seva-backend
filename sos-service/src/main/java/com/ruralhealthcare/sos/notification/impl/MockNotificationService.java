package com.ruralhealthcare.sos.notification.impl;

import com.ruralhealthcare.sos.notification.NotificationMessage;
import com.ruralhealthcare.sos.notification.NotificationResult;
import com.ruralhealthcare.sos.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mock implementation of NotificationService for local development and automated testing.
 * Simulates push notification dispatch without calling real external Firebase services.
 */
@Service
@ConditionalOnProperty(name = "sos.notification.provider", havingValue = "mock", matchIfMissing = true)
public class MockNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationService.class);

    private final List<NotificationMessage> recordedMessages = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean shouldFail = false;
    private volatile String failureReason = "Simulated delivery failure";

    @Override
    public NotificationResult sendNotification(NotificationMessage message) {
        String maskedToken = message.getTargetToken() != null && message.getTargetToken().length() > 8
                ? message.getTargetToken().substring(0, 4) + "..." + message.getTargetToken().substring(message.getTargetToken().length() - 4)
                : "invalid-token";

        if (shouldFail) {
            log.warn("[MOCK FCM] Simulated notification failure for token [{}]: {}", maskedToken, failureReason);
            return NotificationResult.failure(failureReason);
        }

        recordedMessages.add(message);
        String mockMessageId = "mock-fcm-" + UUID.randomUUID();
        log.info("[MOCK FCM] Dispatched push notification to [{}], title: '{}', messageId: {}",
                maskedToken, message.getTitle(), mockMessageId);

        return NotificationResult.success(mockMessageId);
    }

    public List<NotificationMessage> getRecordedMessages() {
        return Collections.unmodifiableList(new ArrayList<>(recordedMessages));
    }

    public void clearRecordedMessages() {
        recordedMessages.clear();
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
