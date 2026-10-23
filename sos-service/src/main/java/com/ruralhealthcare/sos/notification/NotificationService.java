package com.ruralhealthcare.sos.notification;

/**
 * Service abstraction for sending push notifications.
 * Concrete implementations handle Firebase Cloud Messaging (FCM) or Mock dispatch.
 */
public interface NotificationService {

    /**
     * Dispatches a push notification to a device token.
     *
     * @param message notification message containing recipient token, title, body, and payload
     * @return result indicating success or failure reason
     */
    NotificationResult sendNotification(NotificationMessage message);
}
