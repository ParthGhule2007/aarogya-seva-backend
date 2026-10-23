package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.client.FacilityServiceClient;
import com.ruralhealthcare.sos.client.PatientServiceClient;
import com.ruralhealthcare.sos.client.UserLocationServiceClient;
import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.SosEvent;
import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.SosRecipient;
import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.dto.CaregiverDto;
import com.ruralhealthcare.sos.dto.FacilityContactDto;
import com.ruralhealthcare.sos.notification.NotificationMessage;
import com.ruralhealthcare.sos.notification.NotificationResult;
import com.ruralhealthcare.sos.notification.impl.MockNotificationService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFailureIsolationTest {

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

    private MockNotificationService mockNotificationService;
    private SosAsyncProcessor asyncProcessor;

    private UUID patientId;
    private UUID caregiverId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        SosProperties properties = new SosProperties();
        mockNotificationService = new MockNotificationService();

        asyncProcessor = new SosAsyncProcessor(
                sosEventRepository,
                sosRecipientRepository,
                sosNotificationRepository,
                deviceRegistrationService,
                patientServiceClient,
                facilityServiceClient,
                userLocationServiceClient,
                mockNotificationService,
                properties
        );

        patientId = UUID.randomUUID();
        caregiverId = UUID.randomUUID();
        facilityId = UUID.randomUUID();

        lenient().when(sosRecipientRepository.save(any(SosRecipient.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sosEventRepository.save(any(SosEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Test 14 & 15: Verify notification failure for one recipient does not stop others, resulting in PARTIALLY_SENT")
    void testPartialDeliverySuccess() {
        UUID sosId = UUID.randomUUID();
        SosEvent event = SosEvent.builder()
                .patientId(patientId)
                .latitude(26.6811)
                .longitude(80.9984)
                .targetList(List.of(TargetType.CAREGIVER, TargetType.FACILITY))
                .facilityId(facilityId)
                .status(SosEventStatus.PROCESSING)
                .triggeredAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        event.setId(sosId);

        when(sosEventRepository.findById(sosId)).thenReturn(Optional.of(event));

        // Caregiver is resolvable with a valid token
        when(patientServiceClient.getCaregiverForPatient(patientId))
                .thenReturn(Optional.of(CaregiverDto.builder().caregiverId(caregiverId).build()));
        when(deviceRegistrationService.getActiveTokensForUser(caregiverId))
                .thenReturn(List.of("valid-caregiver-token"));

        // Facility service throws an exception (external service outage)
        when(facilityServiceClient.getFacilityContact(facilityId))
                .thenThrow(new RuntimeException("Facility service timeout"));

        // Process asynchronous notification
        asyncProcessor.processSosEventAsync(sosId);

        // Verify: Caregiver succeeded despite facility failure
        assertEquals(SosEventStatus.PARTIALLY_SENT, event.getStatus(),
                "Overall SOS status must be PARTIALLY_SENT when one recipient succeeds and one fails");
        assertEquals(1, mockNotificationService.getRecordedMessages().size());
        assertEquals("valid-caregiver-token", mockNotificationService.getRecordedMessages().get(0).getTargetToken());
    }

    @Test
    @DisplayName("Test 18: Verify MockNotificationService behaves correctly for tests and local dev without real Firebase")
    void testMockNotificationServiceExecution() {
        NotificationMessage msg = NotificationMessage.builder()
                .targetToken("sample-token-12345678")
                .title("Test Alert")
                .body("Test message body")
                .build();

        NotificationResult result = mockNotificationService.sendNotification(msg);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("mock-fcm-"));
        assertEquals(1, mockNotificationService.getRecordedMessages().size());
    }
}
