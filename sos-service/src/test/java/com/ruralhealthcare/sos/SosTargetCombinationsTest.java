package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.client.FacilityServiceClient;
import com.ruralhealthcare.sos.client.PatientServiceClient;
import com.ruralhealthcare.sos.client.UserLocationServiceClient;
import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.RecipientNotificationStatus;
import com.ruralhealthcare.sos.domain.RecipientType;
import com.ruralhealthcare.sos.domain.SosEvent;
import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.SosRecipient;
import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.dto.CaregiverDto;
import com.ruralhealthcare.sos.dto.FacilityContactDto;
import com.ruralhealthcare.sos.dto.NearbyHelperDto;
import com.ruralhealthcare.sos.notification.NotificationMessage;
import com.ruralhealthcare.sos.notification.NotificationResult;
import com.ruralhealthcare.sos.notification.NotificationService;
import com.ruralhealthcare.sos.repository.SosEventRepository;
import com.ruralhealthcare.sos.repository.SosNotificationRepository;
import com.ruralhealthcare.sos.repository.SosRecipientRepository;
import com.ruralhealthcare.sos.service.DeviceRegistrationService;
import com.ruralhealthcare.sos.service.impl.SosAsyncProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosTargetCombinationsTest {

    @Mock
    private SosEventRepository sosEventRepository;

    @Mock
    private SosRecipientRepository sosRecipientRepository;

    @Mock
    private SosNotificationRepository sosNotificationRepository;

    @Mock
    private DeviceRegistrationService deviceRegistrationService;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private FacilityServiceClient facilityServiceClient;

    @Mock
    private UserLocationServiceClient userLocationServiceClient;

    @Mock
    private NotificationService notificationService;

    private SosProperties sosProperties;
    private SosAsyncProcessor asyncProcessor;

    private UUID patientId;
    private UUID facilityId;
    private UUID caregiverId;
    private UUID nearbyUserId;

    @BeforeEach
    void setUp() {
        sosProperties = new SosProperties();
        sosProperties.setNearbyRadiusKm(5.0);

        asyncProcessor = new SosAsyncProcessor(
                sosEventRepository,
                sosRecipientRepository,
                sosNotificationRepository,
                deviceRegistrationService,
                patientServiceClient,
                facilityServiceClient,
                userLocationServiceClient,
                notificationService,
                sosProperties
        );

        patientId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        caregiverId = UUID.randomUUID();
        nearbyUserId = UUID.randomUUID();

        // Default mock behaviors
        lenient().when(sosRecipientRepository.save(any(SosRecipient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sosEventRepository.save(any(SosEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationService.sendNotification(any(NotificationMessage.class)))
                .thenReturn(NotificationResult.success("fcm-msg-123"));
    }

    private SosEvent setupSosEvent(List<TargetType> targets) {
        UUID sosId = UUID.randomUUID();
        SosEvent event = SosEvent.builder()
                .patientId(patientId)
                .latitude(26.6811)
                .longitude(80.9984)
                .targetList(targets)
                .facilityId(targets.contains(TargetType.FACILITY) ? facilityId : null)
                .status(SosEventStatus.PROCESSING)
                .triggeredAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        event.setId(sosId);

        when(sosEventRepository.findById(sosId)).thenReturn(Optional.of(event));
        return event;
    }

    @Test
    @DisplayName("Test 1: Create SOS with CAREGIVER only")
    void testSosCaregiverOnly() {
        SosEvent event = setupSosEvent(List.of(TargetType.CAREGIVER));

        when(patientServiceClient.getCaregiverForPatient(patientId))
                .thenReturn(Optional.of(CaregiverDto.builder().caregiverId(caregiverId).name("Primary Caregiver").build()));
        when(deviceRegistrationService.getActiveTokensForUser(caregiverId))
                .thenReturn(List.of("caregiver-fcm-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, times(1)).getCaregiverForPatient(patientId);
        verify(facilityServiceClient, never()).getFacilityContact(any());
        verify(userLocationServiceClient, never()).findNearbyEmergencyHelpers(anyDouble(), anyDouble(), anyDouble(), any());
        verify(notificationService, times(1)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 2: Create SOS with FACILITY only")
    void testSosFacilityOnly() {
        SosEvent event = setupSosEvent(List.of(TargetType.FACILITY));

        when(facilityServiceClient.getFacilityContact(facilityId))
                .thenReturn(Optional.of(FacilityContactDto.builder().facilityId(facilityId).facilityName("District Hospital").build()));
        when(deviceRegistrationService.getActiveTokensForUser(facilityId))
                .thenReturn(List.of("facility-fcm-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, never()).getCaregiverForPatient(any());
        verify(facilityServiceClient, times(1)).getFacilityContact(facilityId);
        verify(userLocationServiceClient, never()).findNearbyEmergencyHelpers(anyDouble(), anyDouble(), anyDouble(), any());
        verify(notificationService, times(1)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 3: Create SOS with NEARBY_PEOPLE only")
    void testSosNearbyPeopleOnly() {
        SosEvent event = setupSosEvent(List.of(TargetType.NEARBY_PEOPLE));

        when(userLocationServiceClient.findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId))
                .thenReturn(List.of(NearbyHelperDto.builder().userId(nearbyUserId).userType("ASHA").distanceKm(1.5).build()));
        when(deviceRegistrationService.getActiveTokensForUser(nearbyUserId))
                .thenReturn(List.of("nearby-fcm-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, never()).getCaregiverForPatient(any());
        verify(facilityServiceClient, never()).getFacilityContact(any());
        verify(userLocationServiceClient, times(1)).findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId);
        verify(notificationService, times(1)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 4: Create SOS with CAREGIVER + FACILITY")
    void testSosCaregiverPlusFacility() {
        SosEvent event = setupSosEvent(List.of(TargetType.CAREGIVER, TargetType.FACILITY));

        when(patientServiceClient.getCaregiverForPatient(patientId))
                .thenReturn(Optional.of(CaregiverDto.builder().caregiverId(caregiverId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(caregiverId))
                .thenReturn(List.of("caregiver-token"));

        when(facilityServiceClient.getFacilityContact(facilityId))
                .thenReturn(Optional.of(FacilityContactDto.builder().facilityId(facilityId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(facilityId))
                .thenReturn(List.of("facility-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, times(1)).getCaregiverForPatient(patientId);
        verify(facilityServiceClient, times(1)).getFacilityContact(facilityId);
        verify(userLocationServiceClient, never()).findNearbyEmergencyHelpers(anyDouble(), anyDouble(), anyDouble(), any());
        verify(notificationService, times(2)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 5: Create SOS with CAREGIVER + NEARBY_PEOPLE")
    void testSosCaregiverPlusNearby() {
        SosEvent event = setupSosEvent(List.of(TargetType.CAREGIVER, TargetType.NEARBY_PEOPLE));

        when(patientServiceClient.getCaregiverForPatient(patientId))
                .thenReturn(Optional.of(CaregiverDto.builder().caregiverId(caregiverId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(caregiverId))
                .thenReturn(List.of("caregiver-token"));

        when(userLocationServiceClient.findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId))
                .thenReturn(List.of(NearbyHelperDto.builder().userId(nearbyUserId).distanceKm(2.0).build()));
        when(deviceRegistrationService.getActiveTokensForUser(nearbyUserId))
                .thenReturn(List.of("nearby-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, times(1)).getCaregiverForPatient(patientId);
        verify(facilityServiceClient, never()).getFacilityContact(any());
        verify(userLocationServiceClient, times(1)).findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId);
        verify(notificationService, times(2)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 6: Create SOS with FACILITY + NEARBY_PEOPLE")
    void testSosFacilityPlusNearby() {
        SosEvent event = setupSosEvent(List.of(TargetType.FACILITY, TargetType.NEARBY_PEOPLE));

        when(facilityServiceClient.getFacilityContact(facilityId))
                .thenReturn(Optional.of(FacilityContactDto.builder().facilityId(facilityId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(facilityId))
                .thenReturn(List.of("facility-token"));

        when(userLocationServiceClient.findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId))
                .thenReturn(List.of(NearbyHelperDto.builder().userId(nearbyUserId).distanceKm(2.0).build()));
        when(deviceRegistrationService.getActiveTokensForUser(nearbyUserId))
                .thenReturn(List.of("nearby-token"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, never()).getCaregiverForPatient(any());
        verify(facilityServiceClient, times(1)).getFacilityContact(facilityId);
        verify(userLocationServiceClient, times(1)).findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId);
        verify(notificationService, times(2)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }

    @Test
    @DisplayName("Test 7 & 10: Create SOS with all three & verify unselected targets are never processed")
    void testSosAllThreeTargets() {
        SosEvent event = setupSosEvent(List.of(TargetType.CAREGIVER, TargetType.FACILITY, TargetType.NEARBY_PEOPLE));

        when(patientServiceClient.getCaregiverForPatient(patientId))
                .thenReturn(Optional.of(CaregiverDto.builder().caregiverId(caregiverId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(caregiverId))
                .thenReturn(List.of("token-1"));

        when(facilityServiceClient.getFacilityContact(facilityId))
                .thenReturn(Optional.of(FacilityContactDto.builder().facilityId(facilityId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(facilityId))
                .thenReturn(List.of("token-2"));

        when(userLocationServiceClient.findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId))
                .thenReturn(List.of(NearbyHelperDto.builder().userId(nearbyUserId).distanceKm(3.0).build()));
        when(deviceRegistrationService.getActiveTokensForUser(nearbyUserId))
                .thenReturn(List.of("token-3"));

        asyncProcessor.processSosEventAsync(event.getId());

        verify(patientServiceClient, times(1)).getCaregiverForPatient(patientId);
        verify(facilityServiceClient, times(1)).getFacilityContact(facilityId);
        verify(userLocationServiceClient, times(1)).findNearbyEmergencyHelpers(26.6811, 80.9984, 5.0, patientId);
        verify(notificationService, times(3)).sendNotification(any());
        assertEquals(SosEventStatus.NOTIFICATIONS_SENT, event.getStatus());
    }
}
