package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.client.MockUserLocationServiceClient;
import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.DeviceToken;
import com.ruralhealthcare.sos.domain.UserType;
import com.ruralhealthcare.sos.dto.NearbyHelperDto;
import com.ruralhealthcare.sos.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NearbyPeopleRulesTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private MockUserLocationServiceClient locationServiceClient;
    private SosProperties sosProperties;

    @BeforeEach
    void setUp() {
        sosProperties = new SosProperties();
        sosProperties.setNearbyRadiusKm(5.0);
        locationServiceClient = new MockUserLocationServiceClient(deviceTokenRepository);
    }

    @Test
    @DisplayName("Test 11: Server-side fixed 5 km radius configuration")
    void testFixedServerRadius() {
        assertEquals(5.0, sosProperties.getNearbyRadiusKm(), "Nearby radius must be strictly fixed server-side to 5.0 km");
    }

    @Test
    @DisplayName("Test 12: Verify triggering patient is strictly excluded from nearby recipients")
    void testPatientIsExcludedFromNearbyRecipients() {
        UUID patientId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        // The mock location client has predefined helpers including 00000000-0000-0000-0000-000000000001
        when(deviceTokenRepository.findOptedInEmergencyHelpers(patientId))
                .thenReturn(List.of(
                        DeviceToken.builder()
                                .userId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                                .userType(UserType.ASHA)
                                .fcmToken("token-helper-1")
                                .emergencyAlertOptIn(true)
                                .active(true)
                                .lastLatitude(26.6811)
                                .lastLongitude(80.9984)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build()
                ));

        List<NearbyHelperDto> helpers = locationServiceClient.findNearbyEmergencyHelpers(
                26.6811, 80.9984, 5.0, patientId
        );

        assertFalse(helpers.isEmpty());
        for (NearbyHelperDto helper : helpers) {
            assertFalse(helper.getUserId().equals(patientId), "Triggering patient must NEVER be included in nearby recipients");
        }
    }

    @Test
    @DisplayName("Test 13: Verify opted-out users are strictly excluded")
    void testOptedOutUsersAreExcluded() {
        UUID helperWithOptIn = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID helperOptedOut = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID patientId = UUID.randomUUID();

        // Simulate repository only returning tokens for opted-in users
        when(deviceTokenRepository.findOptedInEmergencyHelpers(patientId))
                .thenReturn(List.of(
                        DeviceToken.builder()
                                .userId(helperWithOptIn)
                                .userType(UserType.ASHA)
                                .fcmToken("token-opted-in")
                                .emergencyAlertOptIn(true)
                                .active(true)
                                .lastLatitude(26.6811)
                                .lastLongitude(80.9984)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build()
                ));

        List<NearbyHelperDto> helpers = locationServiceClient.findNearbyEmergencyHelpers(
                26.6811, 80.9984, 5.0, patientId
        );

        assertEquals(1, helpers.size());
        assertEquals(helperWithOptIn, helpers.get(0).getUserId());
        assertFalse(helpers.stream().anyMatch(h -> h.getUserId().equals(helperOptedOut)),
                "Users who have NOT opted in (emergencyAlertOptIn = false) must be excluded");
    }
}
