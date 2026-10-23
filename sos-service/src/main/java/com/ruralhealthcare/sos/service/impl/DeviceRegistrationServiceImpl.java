package com.ruralhealthcare.sos.service.impl;

import com.ruralhealthcare.sos.domain.DeviceToken;
import com.ruralhealthcare.sos.dto.DeviceRegistrationRequest;
import com.ruralhealthcare.sos.dto.DeviceRegistrationResponse;
import com.ruralhealthcare.sos.exception.ResourceNotFoundException;
import com.ruralhealthcare.sos.repository.DeviceTokenRepository;
import com.ruralhealthcare.sos.service.DeviceRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of DeviceRegistrationService.
 * Manages device tokens, user mappings, and opt-in settings.
 */
@Service
@Transactional
public class DeviceRegistrationServiceImpl implements DeviceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationServiceImpl.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceRegistrationServiceImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        log.info("Registering device token for user: {}, userType: {}, optIn: {}",
                request.getUserId(), request.getUserType(), request.isEmergencyAlertOptIn());

        Optional<DeviceToken> existingTokenOpt = deviceTokenRepository.findByFcmToken(request.getFcmToken());

        DeviceToken deviceToken;
        if (existingTokenOpt.isPresent()) {
            deviceToken = existingTokenOpt.get();
            deviceToken.setUserId(request.getUserId());
            deviceToken.setUserType(request.getUserType());
            deviceToken.setEmergencyAlertOptIn(request.isEmergencyAlertOptIn());
            deviceToken.setActive(true);
            if (request.getLatitude() != null) {
                deviceToken.setLastLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                deviceToken.setLastLongitude(request.getLongitude());
            }
            deviceToken.setUpdatedAt(Instant.now());
            log.info("Updated existing device token registration id: {}", deviceToken.getId());
        } else {
            deviceToken = DeviceToken.builder()
                    .userId(request.getUserId())
                    .userType(request.getUserType())
                    .fcmToken(request.getFcmToken())
                    .emergencyAlertOptIn(request.isEmergencyAlertOptIn())
                    .lastLatitude(request.getLatitude())
                    .lastLongitude(request.getLongitude())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            deviceToken = deviceTokenRepository.save(deviceToken);
            log.info("Created new device token registration id: {}", deviceToken.getId());
        }

        return DeviceRegistrationResponse.builder()
                .tokenId(deviceToken.getId())
                .deviceId(deviceToken.getId())
                .userId(deviceToken.getUserId())
                .userType(deviceToken.getUserType())
                .emergencyAlertOptIn(deviceToken.isEmergencyAlertOptIn())
                .active(deviceToken.isActive())
                .registeredAt(deviceToken.getCreatedAt())
                .updatedAt(deviceToken.getUpdatedAt())
                .message("Device registered successfully for emergency notifications")
                .build();
    }

    @Override
    public void deactivateDeviceToken(String fcmToken) {
        log.info("Deactivating device token");
        Optional<DeviceToken> tokenOpt = deviceTokenRepository.findByFcmToken(fcmToken);
        if (tokenOpt.isEmpty()) {
            throw new ResourceNotFoundException("Device token not found or already deactivated");
        }

        DeviceToken token = tokenOpt.get();
        token.setActive(false);
        token.setUpdatedAt(Instant.now());
        deviceTokenRepository.save(token);
        log.info("Device token deactivated successfully: id={}", token.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveTokensForUser(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(DeviceToken::getFcmToken)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveTokensForOptedInUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceTokenRepository.findByUserIdInAndActiveTrueAndEmergencyAlertOptInTrue(userIds).stream()
                .map(DeviceToken::getFcmToken)
                .collect(Collectors.toList());
    }
}
