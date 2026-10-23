package com.ruralhealthcare.sos.notification.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ruralhealthcare.sos.notification.NotificationMessage;
import com.ruralhealthcare.sos.notification.NotificationResult;
import com.ruralhealthcare.sos.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Production implementation of NotificationService using Firebase Cloud Messaging (FCM).
 * Integrates directly with the official Firebase Admin SDK for Java.
 */
@Service
@ConditionalOnProperty(name = "sos.notification.provider", havingValue = "fcm")
public class FcmNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

    private final FirebaseApp firebaseApp;

    public FcmNotificationService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    public NotificationResult sendNotification(NotificationMessage message) {
        String token = message.getTargetToken();
        String maskedToken = token != null && token.length() > 8
                ? token.substring(0, 4) + "..." + token.substring(token.length() - 4)
                : "unknown";

        try {
            Notification notification = Notification.builder()
                    .setTitle(message.getTitle())
                    .setBody(message.getBody())
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            if (message.getData() != null && !message.getData().isEmpty()) {
                messageBuilder.putAllData(message.getData());
            }

            Message fcmMessage = messageBuilder.build();
            String response = FirebaseMessaging.getInstance(firebaseApp).send(fcmMessage);

            log.info("Successfully sent FCM notification to device [{}]. Response message ID: {}", maskedToken, response);
            return NotificationResult.success(response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM dispatch error to device [{}]: errorCode={}, message={}",
                    maskedToken, e.getMessagingErrorCode(), e.getMessage());
            return NotificationResult.failure(e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending FCM notification to device [{}]: {}", maskedToken, e.getMessage());
            return NotificationResult.failure("Unexpected FCM error: " + e.getMessage());
        }
    }
}
