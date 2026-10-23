package com.ruralhealthcare.sos.service.impl;

import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.RecipientNotificationStatus;
import com.ruralhealthcare.sos.domain.SosEvent;
import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.SosRecipient;
import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.domain.UserType;
import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.dto.NearbyAlertDto;
import com.ruralhealthcare.sos.dto.SosResponse;
import com.ruralhealthcare.sos.dto.SosStatusResponse;
import com.ruralhealthcare.sos.exception.ResourceNotFoundException;
import com.ruralhealthcare.sos.repository.DeviceTokenRepository;
import com.ruralhealthcare.sos.repository.SosEventRepository;
import com.ruralhealthcare.sos.repository.SosRecipientRepository;
import com.ruralhealthcare.sos.security.AuthorizationService;
import com.ruralhealthcare.sos.service.SosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of SosService.
 * Coordinates validation, idempotency checks, event persistence, and asynchronous notification dispatches.
 */
@Service
@Transactional
public class SosServiceImpl implements SosService {

    private static final Logger log = LoggerFactory.getLogger(SosServiceImpl.class);

    private final SosEventRepository sosEventRepository;
    private final SosRecipientRepository sosRecipientRepository;
    private final SosAsyncProcessor sosAsyncProcessor;
    private final AuthorizationService authorizationService;
    private final SosProperties sosProperties;
    private final DeviceTokenRepository deviceTokenRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public SosServiceImpl(
            SosEventRepository sosEventRepository,
            SosRecipientRepository sosRecipientRepository,
            SosAsyncProcessor sosAsyncProcessor,
            AuthorizationService authorizationService,
            SosProperties sosProperties,
            DeviceTokenRepository deviceTokenRepository) {
        this.sosEventRepository = sosEventRepository;
        this.sosRecipientRepository = sosRecipientRepository;
        this.sosAsyncProcessor = sosAsyncProcessor;
        this.authorizationService = authorizationService;
        this.sosProperties = sosProperties;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public SosServiceImpl(
            SosEventRepository sosEventRepository,
            SosRecipientRepository sosRecipientRepository,
            SosAsyncProcessor sosAsyncProcessor,
            AuthorizationService authorizationService,
            SosProperties sosProperties) {
        this(sosEventRepository, sosRecipientRepository, sosAsyncProcessor, authorizationService, sosProperties, null);
    }

    @Override
    public SosResponse triggerSos(CreateSosRequest request, String idempotencyKey) {
        log.info("Received emergency SOS trigger request for patient: {}, targets: {}",
                request.getPatientId(), request.getTargets());

        // 1. Validation & normalization
        request.normalizeAndValidate();

        // 2. Authorization check
        authorizationService.verifyAuthorization(request.getPatientId());

        // 3. Idempotency Key check
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<SosEvent> existingEventOpt = sosEventRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existingEventOpt.isPresent()) {
                SosEvent existing = existingEventOpt.get();
                log.info("Duplicate SOS request detected with Idempotency-Key [{}]. Returning existing event {}",
                        idempotencyKey, existing.getId());
                return SosResponse.builder()
                        .sosId(existing.getId())
                        .status(existing.getStatus())
                        .triggeredAt(existing.getTriggeredAt())
                        .targets(existing.getTargetList())
                        .message("Existing SOS returned (idempotency key matched)")
                        .build();
            }
        }

        // 4. Server-side short deduplication window check for accidental repeated SOS
        int windowMinutes = sosProperties.getDeduplicationWindowMinutes();
        if (windowMinutes > 0) {
            Optional<SosEvent> lastEventOpt = sosEventRepository.findTopByPatientIdOrderByTriggeredAtDesc(request.getPatientId());
            if (lastEventOpt.isPresent()) {
                SosEvent lastEvent = lastEventOpt.get();
                Instant windowThreshold = Instant.now().minus(Duration.ofMinutes(windowMinutes));
                if (lastEvent.getTriggeredAt().isAfter(windowThreshold) &&
                        (lastEvent.getStatus() == SosEventStatus.TRIGGERED ||
                         lastEvent.getStatus() == SosEventStatus.PROCESSING ||
                         lastEvent.getStatus() == SosEventStatus.NOTIFICATIONS_SENT ||
                         lastEvent.getStatus() == SosEventStatus.PARTIALLY_SENT)) {
                    log.info("Patient {} triggered SOS within deduplication window of {} minutes. Returning existing event {}",
                            request.getPatientId(), windowMinutes, lastEvent.getId());
                    return SosResponse.builder()
                            .sosId(lastEvent.getId())
                            .status(lastEvent.getStatus())
                            .triggeredAt(lastEvent.getTriggeredAt())
                            .targets(lastEvent.getTargetList())
                            .message("Existing active SOS returned (deduplication window active)")
                            .build();
                }
            }
        }

        // 5. Build and persist SOS event
        UUID facilityId = request.getTargets().contains(TargetType.FACILITY) ? request.getFacilityId() : null;

        SosEvent event = SosEvent.builder()
                .patientId(request.getPatientId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .targetList(request.getTargets())
                .facilityId(facilityId)
                .message(request.getMessage())
                .idempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null)
                .status(SosEventStatus.PROCESSING)
                .triggeredAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        event = sosEventRepository.save(event);
        log.info("Persisted new SOS event {} with status {}", event.getId(), event.getStatus());

        // 6. Trigger asynchronous dispatch after database transaction commits
        final UUID finalSosId = event.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sosAsyncProcessor.processSosEventAsync(finalSosId);
                }
            });
        } else {
            sosAsyncProcessor.processSosEventAsync(finalSosId);
        }

        // 7. Return fast HTTP 202 ACCEPTED response
        return SosResponse.builder()
                .sosId(event.getId())
                .status(event.getStatus())
                .triggeredAt(event.getTriggeredAt())
                .targets(event.getTargetList())
                .message("Emergency SOS triggered successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SosStatusResponse getSosStatus(UUID sosId) {
        SosEvent event = sosEventRepository.findById(sosId)
                .orElseThrow(() -> new ResourceNotFoundException("SOS event not found with id: " + sosId));

        return toStatusResponse(event);
    }

    private SosStatusResponse toStatusResponse(SosEvent event) {
        List<SosRecipient> recipients = sosRecipientRepository.findBySosEventId(event.getId());

        long notifiedCount = recipients.stream()
                .filter(r -> r.getNotificationStatus() == RecipientNotificationStatus.SENT)
                .count();

        long failedCount = recipients.stream()
                .filter(r -> r.getNotificationStatus() == RecipientNotificationStatus.FAILED)
                .count();

        return SosStatusResponse.builder()
                .sosId(event.getId())
                .patientId(event.getPatientId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .facilityId(event.getFacilityId())
                .message(event.getMessage())
                .status(event.getStatus())
                .triggeredAt(event.getTriggeredAt())
                .resolvedAt(event.getResolvedAt())
                .targets(event.getTargetList())
                .totalRecipients((int) recipients.size())
                .recipientsNotified(notifiedCount)
                .recipientsFailed(failedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SosStatusResponse> getRecentSosEvents() {
        return sosEventRepository.findTop50ByOrderByTriggeredAtDesc()
                .stream()
                .filter(e -> e.getStatus() != SosEventStatus.RESOLVED)
                .map(this::toStatusResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SosStatusResponse> getSosEventsByPatient(UUID patientId) {
        return sosEventRepository.findByPatientIdOrderByTriggeredAtDesc(patientId)
                .stream()
                .map(this::toStatusResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SosStatusResponse> getSosEventsByFacility(UUID facilityId) {
        return sosEventRepository.findByFacilityIdOrderByTriggeredAtDesc(facilityId)
                .stream()
                .map(this::toStatusResponse)
                .toList();
    }

    @Override
    public SosStatusResponse resolveSos(UUID sosId) {
        SosEvent event = sosEventRepository.findById(sosId)
                .orElseThrow(() -> new ResourceNotFoundException("SOS event not found with id: " + sosId));

        event.setStatus(SosEventStatus.RESOLVED);
        event.setResolvedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        event = sosEventRepository.save(event);
        log.info("SOS event {} marked as RESOLVED", sosId);

        return getSosStatus(sosId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyAlertDto> getActiveNearbyAlertsForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        // Doctors should never receive or see nearby community emergency SOS alerts
        if (deviceTokenRepository != null) {
            List<com.ruralhealthcare.sos.domain.DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
            if (!tokens.isEmpty() && tokens.stream().anyMatch(t -> t.getUserType() == UserType.DOCTOR)) {
                return List.of();
            }
        }
        Instant tenMinutesAgo = Instant.now().minus(Duration.ofMinutes(10));
        List<SosRecipient> recipients = sosRecipientRepository.findByRecipientIdOrderByCreatedAtDesc(userId);

        List<NearbyAlertDto> alerts = new ArrayList<>();
        for (SosRecipient r : recipients) {
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(tenMinutesAgo)) {
                Optional<SosEvent> eventOpt = sosEventRepository.findById(r.getSosEventId());
                if (eventOpt.isPresent()) {
                    SosEvent event = eventOpt.get();
                    if (event.getStatus() != SosEventStatus.RESOLVED && !event.getPatientId().equals(userId)) {
                        alerts.add(NearbyAlertDto.builder()
                                .sosId(event.getId())
                                .patientId(event.getPatientId())
                                .latitude(event.getLatitude())
                                .longitude(event.getLongitude())
                                .distanceKm(r.getDistanceKm() != null ? r.getDistanceKm() : 1.5)
                                .message(event.getMessage() != null ? event.getMessage() : "Someone near you needs emergency help")
                                .triggeredAt(event.getTriggeredAt())
                                .status(event.getStatus().name())
                                .build());
                    }
                }
            }
        }
        return alerts;
    }
}
