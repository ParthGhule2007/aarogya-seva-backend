package com.ruralhealthcare.sos.service.impl;

import com.ruralhealthcare.sos.client.FacilityServiceClient;
import com.ruralhealthcare.sos.client.PatientServiceClient;
import com.ruralhealthcare.sos.client.UserLocationServiceClient;
import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.domain.NotificationStatus;
import com.ruralhealthcare.sos.domain.RecipientNotificationStatus;
import com.ruralhealthcare.sos.domain.RecipientType;
import com.ruralhealthcare.sos.domain.SosEvent;
import com.ruralhealthcare.sos.domain.SosEventStatus;
import com.ruralhealthcare.sos.domain.SosNotification;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Asynchronous processor for resolving SOS recipients and dispatching FCM notifications.
 * Ensures strict fault isolation across target groups and recipients.
 */
@Service
public class SosAsyncProcessor {

    private static final Logger log = LoggerFactory.getLogger(SosAsyncProcessor.class);

    private final SosEventRepository sosEventRepository;
    private final SosRecipientRepository sosRecipientRepository;
    private final SosNotificationRepository sosNotificationRepository;
    private final DeviceRegistrationService deviceRegistrationService;
    private final PatientServiceClient patientServiceClient;
    private final FacilityServiceClient facilityServiceClient;
    private final UserLocationServiceClient userLocationServiceClient;
    private final NotificationService notificationService;
    private final SosProperties sosProperties;

    public SosAsyncProcessor(
            SosEventRepository sosEventRepository,
            SosRecipientRepository sosRecipientRepository,
            SosNotificationRepository sosNotificationRepository,
            DeviceRegistrationService deviceRegistrationService,
            PatientServiceClient patientServiceClient,
            FacilityServiceClient facilityServiceClient,
            UserLocationServiceClient userLocationServiceClient,
            NotificationService notificationService,
            SosProperties sosProperties) {
        this.sosEventRepository = sosEventRepository;
        this.sosRecipientRepository = sosRecipientRepository;
        this.sosNotificationRepository = sosNotificationRepository;
        this.deviceRegistrationService = deviceRegistrationService;
        this.patientServiceClient = patientServiceClient;
        this.facilityServiceClient = facilityServiceClient;
        this.userLocationServiceClient = userLocationServiceClient;
        this.notificationService = notificationService;
        this.sosProperties = sosProperties;
    }

    /**
     * Executes asynchronous resolution of targets and notification delivery.
     * Isolated thread execution with comprehensive exception capture.
     */
    @Async("sosNotificationExecutor")
    @Transactional
    public void processSosEventAsync(UUID sosId) {
        log.info("[ASYNC-SOS] Starting async notification processing for SOS event: {}", sosId);

        Optional<SosEvent> eventOpt = sosEventRepository.findById(sosId);
        if (eventOpt.isEmpty()) {
            for (int i = 0; i < 5 && eventOpt.isEmpty(); i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                eventOpt = sosEventRepository.findById(sosId);
            }
        }

        if (eventOpt.isEmpty()) {
            log.error("[ASYNC-SOS] SOS event {} not found in database after retries", sosId);
            return;
        }

        SosEvent event = eventOpt.get();
        List<TargetType> targets = event.getTargetTypes();
        List<SosRecipient> recipients = new ArrayList<>();

        // 1. Process CAREGIVER target if selected
        if (targets.contains(TargetType.CAREGIVER)) {
            processCaregiverTarget(event, recipients);
        }

        // 2. Process FACILITY target if selected
        if (targets.contains(TargetType.FACILITY)) {
            processFacilityTarget(event, recipients);
        }

        // 3. Process NEARBY_PEOPLE target if selected
        if (targets.contains(TargetType.NEARBY_PEOPLE)) {
            processNearbyPeopleTarget(event, recipients);
        }

        // 4. Update overall SOS event status based on recipient delivery results
        updateEventFinalStatus(event, recipients);
    }

    private void processCaregiverTarget(SosEvent event, List<SosRecipient> recipients) {
        log.info("[ASYNC-SOS] Resolving caregiver for patient: {}", event.getPatientId());
        try {
            Optional<CaregiverDto> caregiverOpt = patientServiceClient.getCaregiverForPatient(event.getPatientId());

            if (caregiverOpt.isPresent()) {
                CaregiverDto caregiver = caregiverOpt.get();
                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(caregiver.getCaregiverId())
                        .recipientType(RecipientType.CAREGIVER)
                        .notificationStatus(RecipientNotificationStatus.PENDING)
                        .createdAt(Instant.now())
                        .build();
                recipient = sosRecipientRepository.save(recipient);
                recipients.add(recipient);

                dispatchNotifications(event, recipient, "🚨 Emergency SOS",
                        "A patient associated with you has triggered an emergency SOS and needs immediate assistance.",
                        Map.of("sosId", event.getId().toString(), "targetType", "CAREGIVER"));
            } else {
                log.warn("[ASYNC-SOS] No caregiver found for patient: {}", event.getPatientId());
                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(UUID.randomUUID()) // placeholder for unresolvable recipient
                        .recipientType(RecipientType.CAREGIVER)
                        .notificationStatus(RecipientNotificationStatus.FAILED)
                        .createdAt(Instant.now())
                        .build();
                sosRecipientRepository.save(recipient);
                recipients.add(recipient);
            }
        } catch (Exception e) {
            log.error("[ASYNC-SOS] Error resolving caregiver for SOS {}: {}", event.getId(), e.getMessage());
            SosRecipient recipient = SosRecipient.builder()
                    .sosEvent(event)
                    .recipientId(UUID.randomUUID())
                    .recipientType(RecipientType.CAREGIVER)
                    .notificationStatus(RecipientNotificationStatus.FAILED)
                    .createdAt(Instant.now())
                    .build();
            sosRecipientRepository.save(recipient);
            recipients.add(recipient);
        }
    }

    private void processFacilityTarget(SosEvent event, List<SosRecipient> recipients) {
        if (event.getFacilityId() == null) {
            log.error("[ASYNC-SOS] Facility target selected but facilityId is missing for SOS: {}", event.getId());
            return;
        }

        log.info("[ASYNC-SOS] Validating selected facility: {}", event.getFacilityId());
        try {
            Optional<FacilityContactDto> facilityOpt = facilityServiceClient.getFacilityContact(event.getFacilityId());

            if (facilityOpt.isPresent()) {
                FacilityContactDto facility = facilityOpt.get();
                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(facility.getFacilityId())
                        .recipientType(RecipientType.FACILITY)
                        .notificationStatus(RecipientNotificationStatus.PENDING)
                        .createdAt(Instant.now())
                        .build();
                recipient = sosRecipientRepository.save(recipient);
                recipients.add(recipient);

                // Send to facility tokens or authorized recipients
                List<UUID> recipientIds = (facility.getAuthorizedRecipientIds() != null && !facility.getAuthorizedRecipientIds().isEmpty())
                        ? facility.getAuthorizedRecipientIds()
                        : List.of(facility.getFacilityId());

                List<String> tokens = new ArrayList<>();
                for (UUID id : recipientIds) {
                    tokens.addAll(deviceRegistrationService.getActiveTokensForUser(id));
                }

                dispatchTokens(event, recipient, tokens, "🚨 Emergency Alert",
                        "An emergency SOS has been triggered near your facility.",
                        Map.of("sosId", event.getId().toString(), "targetType", "FACILITY"));
            } else {
                log.warn("[ASYNC-SOS] Facility {} could not be verified", event.getFacilityId());
                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(event.getFacilityId())
                        .recipientType(RecipientType.FACILITY)
                        .notificationStatus(RecipientNotificationStatus.FAILED)
                        .createdAt(Instant.now())
                        .build();
                sosRecipientRepository.save(recipient);
                recipients.add(recipient);
            }
        } catch (Exception e) {
            log.error("[ASYNC-SOS] Error resolving facility for SOS {}: {}", event.getId(), e.getMessage());
            SosRecipient recipient = SosRecipient.builder()
                    .sosEvent(event)
                    .recipientId(event.getFacilityId())
                    .recipientType(RecipientType.FACILITY)
                    .notificationStatus(RecipientNotificationStatus.FAILED)
                    .createdAt(Instant.now())
                    .build();
            sosRecipientRepository.save(recipient);
            recipients.add(recipient);
        }
    }

    private void processNearbyPeopleTarget(SosEvent event, List<SosRecipient> recipients) {
        double radiusKm = sosProperties.getNearbyRadiusKm();
        log.info("[ASYNC-SOS] Discovering opted-in nearby helpers within server-enforced {} km (lat={}, lon={})",
                radiusKm, event.getLatitude(), event.getLongitude());

        try {
            List<NearbyHelperDto> nearbyHelpers = userLocationServiceClient.findNearbyEmergencyHelpers(
                    event.getLatitude(),
                    event.getLongitude(),
                    radiusKm,
                    event.getPatientId()
            );

            log.info("[ASYNC-SOS] Discovered {} eligible nearby helpers for SOS {}", nearbyHelpers.size(), event.getId());

            if (nearbyHelpers.isEmpty()) {
                log.warn("[ASYNC-SOS] No nearby helpers found within {} km for SOS {}", radiusKm, event.getId());
                // No nearby helpers found: create one record indicating 0 nearby reached
                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(UUID.randomUUID())
                        .recipientType(RecipientType.NEARBY_PEOPLE)
                        .distanceKm(0.0)
                        .notificationStatus(RecipientNotificationStatus.FAILED)
                        .createdAt(Instant.now())
                        .build();
                sosRecipientRepository.save(recipient);
                recipients.add(recipient);
                return;
            }

            for (NearbyHelperDto helper : nearbyHelpers) {
                // Ensure patient themselves is excluded (double check)
                if (helper.getUserId().equals(event.getPatientId())) {
                    continue;
                }

                SosRecipient recipient = SosRecipient.builder()
                        .sosEvent(event)
                        .recipientId(helper.getUserId())
                        .recipientType(RecipientType.NEARBY_PEOPLE)
                        .distanceKm(helper.getDistanceKm())
                        .notificationStatus(RecipientNotificationStatus.PENDING)
                        .createdAt(Instant.now())
                        .build();
                recipient = sosRecipientRepository.save(recipient);
                recipients.add(recipient);

                dispatchNotifications(event, recipient, "🚨 Someone Nearby Needs Help",
                        "A person near your location has triggered an emergency alert.",
                        Map.of("sosId", event.getId().toString(), "targetType", "NEARBY_PEOPLE"));
            }
        } catch (Exception e) {
            log.error("[ASYNC-SOS] Error discovering nearby helpers for SOS {}: {}", event.getId(), e.getMessage());
            SosRecipient recipient = SosRecipient.builder()
                    .sosEvent(event)
                    .recipientId(UUID.randomUUID())
                    .recipientType(RecipientType.NEARBY_PEOPLE)
                    .distanceKm(0.0)
                    .notificationStatus(RecipientNotificationStatus.FAILED)
                    .createdAt(Instant.now())
                    .build();
            sosRecipientRepository.save(recipient);
            recipients.add(recipient);
        }
    }

    private void dispatchNotifications(SosEvent event, SosRecipient recipient, String title, String body, Map<String, String> data) {
        List<String> tokens = deviceRegistrationService.getActiveTokensForUser(recipient.getRecipientId());
        dispatchTokens(event, recipient, tokens, title, body, data);
    }

    private void dispatchTokens(SosEvent event, SosRecipient recipient, List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("[ASYNC-SOS] No active device tokens found for recipient {}", recipient.getRecipientId());
            recipient.setNotificationStatus(RecipientNotificationStatus.FAILED);
            sosRecipientRepository.save(recipient);
            return;
        }

        boolean anySuccess = false;
        for (String token : tokens) {
            NotificationMessage message = NotificationMessage.builder()
                    .targetToken(token)
                    .title(title)
                    .body(body)
                    .data(data)
                    .build();

            NotificationResult result = notificationService.sendNotification(message);

            SosNotification notification = SosNotification.builder()
                    .sosEvent(event)
                    .recipient(recipient)
                    .channel("FCM")
                    .status(result.isSuccessful() ? NotificationStatus.SENT : NotificationStatus.FAILED)
                    .fcmMessageId(result.getMessageId())
                    .failureReason(result.getFailureReason())
                    .sentAt(result.isSuccessful() ? Instant.now() : null)
                    .createdAt(Instant.now())
                    .build();
            sosNotificationRepository.save(notification);

            if (result.isSuccessful()) {
                anySuccess = true;
            }
        }

        recipient.setNotificationStatus(anySuccess ? RecipientNotificationStatus.SENT : RecipientNotificationStatus.FAILED);
        recipient.setNotifiedAt(anySuccess ? Instant.now() : null);
        sosRecipientRepository.save(recipient);
    }

    private void updateEventFinalStatus(SosEvent event, List<SosRecipient> recipients) {
        // Keep in-app emergency alert lifecycle active (PROCESSING) unless already RESOLVED.
        // Delivery status of external SMS/FCM notifications is tracked per recipient and in recipient counts.
        if (event.getStatus() != SosEventStatus.RESOLVED) {
            long sentCount = recipients.stream().filter(r -> r.getNotificationStatus() == RecipientNotificationStatus.SENT).count();
            if (sentCount > 0) {
                event.setStatus(SosEventStatus.NOTIFICATIONS_SENT);
            } else {
                event.setStatus(SosEventStatus.PROCESSING);
            }
        }

        event.setUpdatedAt(Instant.now());
        sosEventRepository.save(event);
        log.info("[ASYNC-SOS] Finished processing SOS event {}. In-app alert status: {}", event.getId(), event.getStatus());
    }
}
