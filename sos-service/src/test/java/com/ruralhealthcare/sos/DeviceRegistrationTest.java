package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.domain.DeviceToken;
import com.ruralhealthcare.sos.domain.UserType;
import com.ruralhealthcare.sos.dto.DeviceRegistrationRequest;
import com.ruralhealthcare.sos.dto.DeviceRegistrationResponse;
import com.ruralhealthcare.sos.exception.ResourceNotFoundException;
import com.ruralhealthcare.sos.repository.DeviceTokenRepository;
import com.ruralhealthcare.sos.service.impl.DeviceRegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceRegistrationTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private DeviceRegistrationServiceImpl deviceRegistrationService;

    @BeforeEach
    void setUp() {
        deviceRegistrationService = new DeviceRegistrationServiceImpl(deviceTokenRepository);
    }

    @Test
    @DisplayName("Test 19: Verify device registration creates new token mapping with emergencyAlertOptIn")
    void testRegisterNewDevice() {
        UUID userId = UUID.randomUUID();
        String fcmToken = "sample-firebase-token-xyz-12345";

        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setUserId(userId);
        request.setUserType(UserType.ASHA);
        request.setFcmToken(fcmToken);
        request.setEmergencyAlertOptIn(true);

        when(deviceTokenRepository.findByFcmToken(fcmToken)).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> {
            DeviceToken token = inv.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(UserType.ASHA, response.getUserType());
        assertTrue(response.isEmergencyAlertOptIn());
        assertTrue(response.isActive());

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();
        assertEquals(fcmToken, saved.getFcmToken());
        assertTrue(saved.isEmergencyAlertOptIn());
    }

    @Test
    @DisplayName("Test 20: Verify device deletion/deactivation updates active flag to false")
    void testDeactivateDeviceToken() {
        String tokenString = "existing-active-token-98765";
        DeviceToken existingToken = DeviceToken.builder()
                .userId(UUID.randomUUID())
                .userType(UserType.PATIENT)
                .fcmToken(tokenString)
                .emergencyAlertOptIn(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        existingToken.setId(UUID.randomUUID());

        when(deviceTokenRepository.findByFcmToken(tokenString)).thenReturn(Optional.of(existingToken));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceRegistrationService.deactivateDeviceToken(tokenString);

        assertFalse(existingToken.isActive(), "Device token must be deactivated");
        verify(deviceTokenRepository).save(existingToken);
    }

    @Test
    @DisplayName("Deactivating non-existent token throws ResourceNotFoundException")
    void testDeactivateNonExistentTokenThrows() {
        when(deviceTokenRepository.findByFcmToken("non-existent")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> deviceRegistrationService.deactivateDeviceToken("non-existent"));
    }
}
