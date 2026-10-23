package com.ruralhealthcare.sos.service;

import com.ruralhealthcare.sos.dto.DeviceRegistrationRequest;
import com.ruralhealthcare.sos.dto.DeviceRegistrationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for registering and managing FCM device tokens.
 */
public interface DeviceRegistrationService {

    /**
     * Registers or updates an FCM device token for a user.
     *
     * @param request device registration details
     * @return registration confirmation
     */
    DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request);

    /**
     * Deactivates or removes a registered FCM device token.
     *
     * @param fcmToken device token to deactivate
     */
    void deactivateDeviceToken(String fcmToken);

    /**
     * Retrieves active FCM tokens for a specific user.
     *
     * @param userId user identifier
     * @return list of active token strings
     */
    List<String> getActiveTokensForUser(UUID userId);

    /**
     * Retrieves active FCM tokens for a list of opted-in users.
     *
     * @param userIds candidate user identifiers
     * @return map or list of tokens for users with emergencyAlertOptIn = true
     */
    List<String> getActiveTokensForOptedInUsers(List<UUID> userIds);
}
