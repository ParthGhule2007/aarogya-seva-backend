package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.SosEvent;
import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.dto.SosResponse;
import com.ruralhealthcare.sos.exception.AccessDeniedException;
import com.ruralhealthcare.sos.repository.SosEventRepository;
import com.ruralhealthcare.sos.repository.SosRecipientRepository;
import com.ruralhealthcare.sos.security.AuthenticationContext;
import com.ruralhealthcare.sos.security.AuthorizationService;
import com.ruralhealthcare.sos.security.UserPrincipal;
import com.ruralhealthcare.sos.service.impl.SosAsyncProcessor;
import com.ruralhealthcare.sos.service.impl.SosServiceImpl;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAndSecurityTest {

    @Mock
    private SosEventRepository sosEventRepository;

    @Mock
    private SosRecipientRepository sosRecipientRepository;

    @Mock
    private SosAsyncProcessor sosAsyncProcessor;

    private AuthorizationService authorizationService;
    private SosProperties sosProperties;
    private SosServiceImpl sosService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        sosProperties = new SosProperties();
        sosProperties.setDeduplicationWindowMinutes(5);

        sosService = new SosServiceImpl(
                sosEventRepository,
                sosRecipientRepository,
                sosAsyncProcessor,
                authorizationService,
                sosProperties
        );
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    @DisplayName("Test 16: Verify idempotency - duplicate request with same Idempotency-Key returns existing SOS without creating new event")
    void testIdempotencyKeyDeduplication() {
        UUID patientId = UUID.randomUUID();
        String idempotencyKey = "client-key-12345";

        AuthenticationContext.setCurrentUser(new UserPrincipal(patientId, "PATIENT"));

        UUID existingSosId = UUID.randomUUID();
        SosEvent existingEvent = SosEvent.builder()
                .patientId(patientId)
                .latitude(26.6811)
                .longitude(80.9984)
                .targetList(List.of(TargetType.CAREGIVER))
                .idempotencyKey(idempotencyKey)
                .status(SosEventStatus.PROCESSING)
                .triggeredAt(Instant.now().minusSeconds(10))
                .createdAt(Instant.now().minusSeconds(10))
                .updatedAt(Instant.now().minusSeconds(10))
                .build();
        existingEvent.setId(existingSosId);

        when(sosEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEvent));

        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(patientId);
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER));

        SosResponse response = sosService.triggerSos(request, idempotencyKey);

        assertNotNull(response);
        assertEquals(existingSosId, response.getSosId());
        assertTrue(response.getMessage().contains("idempotency key matched"));
        verify(sosEventRepository, never()).save(any(SosEvent.class));
        verify(sosAsyncProcessor, never()).processSosEventAsync(any());
    }

    @Test
    @DisplayName("Test 17: Verify unauthorized patient access is rejected when caller tries to trigger SOS for a different patient")
    void testUnauthorizedPatientAccessRejected() {
        UUID authenticatedPatientId = UUID.randomUUID();
        UUID victimPatientId = UUID.randomUUID();

        // Authenticated as patient A
        AuthenticationContext.setCurrentUser(new UserPrincipal(authenticatedPatientId, "PATIENT"));

        // Attempting to trigger SOS on behalf of patient B
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(victimPatientId);
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER));

        assertThrows(AccessDeniedException.class, () -> sosService.triggerSos(request, null));
    }

    @Test
    @DisplayName("Authorized ASHA worker can trigger SOS on behalf of a patient")
    void testAuthorizedAshaCanTriggerForPatient() {
        UUID ashaId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        // Authenticated as ASHA
        AuthenticationContext.setCurrentUser(new UserPrincipal(ashaId, "ASHA"));

        when(sosEventRepository.save(any(SosEvent.class))).thenAnswer(inv -> {
            SosEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(patientId);
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER));

        SosResponse response = sosService.triggerSos(request, null);
        assertNotNull(response);
        assertEquals(SosEventStatus.PROCESSING, response.getStatus());
    }
}
