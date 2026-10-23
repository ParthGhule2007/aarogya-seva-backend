package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.domain.*;
import com.ruralhealthcare.medicine.repository.CaregiverEscalationLogRepository;
import com.ruralhealthcare.medicine.repository.CaregiverRepository;
import com.ruralhealthcare.medicine.repository.MedicationRepository;
import com.ruralhealthcare.medicine.service.CaregiverNotificationService;
import com.ruralhealthcare.medicine.service.channel.ChannelSender;
import com.ruralhealthcare.medicine.service.channel.DeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaregiverNotificationServiceImpl implements CaregiverNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverNotificationServiceImpl.class);

    private final CaregiverRepository caregiverRepository;
    private final CaregiverEscalationLogRepository escalationLogRepository;
    private final MedicationRepository medicationRepository;
    private final Map<NotificationChannel, ChannelSender> sendersByChannel;

    public CaregiverNotificationServiceImpl(CaregiverRepository caregiverRepository,
                                           CaregiverEscalationLogRepository escalationLogRepository,
                                           MedicationRepository medicationRepository,
                                           List<ChannelSender> channelSenders) {
        this.caregiverRepository = caregiverRepository;
        this.escalationLogRepository = escalationLogRepository;
        this.medicationRepository = medicationRepository;
        this.sendersByChannel = channelSenders.stream()
                .collect(Collectors.toMap(ChannelSender::getChannel, s -> s, (s1, s2) -> s1));
    }

    @Override
    public void notifyCaregiverConsecutiveMissedDoses(UUID patientId, UUID medicationId, int consecutiveMissedCount) {
        String medicationName = medicationRepository.findById(medicationId)
                .map(Medication::getName)
                .orElse("Prescribed Medicine");
        notifyCaregiverConsecutiveMissedDoses(patientId, medicationId, medicationName,
                UUID.randomUUID(), UUID.randomUUID(), consecutiveMissedCount);
    }

    @Override
    public void notifyCaregiverConsecutiveMissedDoses(UUID patientId, UUID medicationId, String medicationName,
                                                       UUID streakStartDoseId, UUID triggeringDoseId, int consecutiveMissedCount) {
        log.warn("🚨 [CAREGIVER ESCALATION] Patient [{}] reached {} consecutive missed doses for medication [{}] ('{}').",
                patientId, consecutiveMissedCount, medicationId, medicationName);

        // Deduplication: Verify streak has not already been escalated
        if (streakStartDoseId != null && (escalationLogRepository.existsByStreakStartDoseId(streakStartDoseId)
                || escalationLogRepository.existsByMedicationIdAndStreakStartDoseId(medicationId, streakStartDoseId))) {
            log.info("Caregiver escalation alert already dispatched for streak start dose [{}]. Skipping duplicate alert.",
                    streakStartDoseId);
            return;
        }

        List<Caregiver> caregivers = caregiverRepository.findByPatientIdAndIsActiveTrue(patientId);
        String safeMedName = (medicationName != null && !medicationName.isBlank()) ? medicationName : "Prescribed Medicine";
        String alertMessage = String.format("Aarogya Seva Alert: Patient has missed %d consecutive doses of '%s'. Please check in on them.",
                consecutiveMissedCount, safeMedName);

        if (caregivers.isEmpty()) {
            log.warn("No active caregivers found for patient [{}]. Recording unassigned escalation audit log.", patientId);
            CaregiverEscalationLog auditLog = CaregiverEscalationLog.builder()
                    .patientId(patientId)
                    .medicationId(medicationId)
                    .medicationName(safeMedName)
                    .streakStartDoseId(streakStartDoseId != null ? streakStartDoseId : UUID.randomUUID())
                    .triggeringDoseId(triggeringDoseId != null ? triggeringDoseId : UUID.randomUUID())
                    .consecutiveMissedCount(consecutiveMissedCount)
                    .channel(NotificationChannel.SMS)
                    .recipient("NO_ACTIVE_CAREGIVER")
                    .deliveryStatus(DeliveryStatus.GATEWAY_FAILED)
                    .messageContent(alertMessage)
                    .providerReference("UNASSIGNED-AUDIT")
                    .deliveryAttemptedAt(Instant.now())
                    .build();
            escalationLogRepository.save(auditLog);
            return;
        }

        List<String> recipients = new java.util.ArrayList<>();
        List<String> providerRefs = new java.util.ArrayList<>();
        DeliveryStatus overallStatus = DeliveryStatus.GATEWAY_ACCEPTED;
        NotificationChannel primaryChannel = NotificationChannel.SMS;

        for (Caregiver caregiver : caregivers) {
            NotificationChannel channel = caregiver.getPreferredChannel() != null ? caregiver.getPreferredChannel() : NotificationChannel.SMS;
            String recipient = resolveRecipient(caregiver, channel);

            ChannelSender sender = sendersByChannel.get(channel);
            DeliveryResult result;
            if (sender != null) {
                result = sender.send(recipient, alertMessage);
            } else {
                String fallbackRef = "SIMULATED-" + channel + "-" + UUID.randomUUID().toString().substring(0, 8);
                result = DeliveryResult.simulated(fallbackRef);
                log.info("Simulated fallback dispatch via [{}] to [{}] (Ref: {})", channel, recipient, fallbackRef);
            }

            recipients.add(recipient);
            if (result.getProviderReference() != null) {
                providerRefs.add(result.getProviderReference());
            }
            if (result.getStatus() == DeliveryStatus.GATEWAY_FAILED) {
                overallStatus = DeliveryStatus.GATEWAY_FAILED;
            } else if (result.getStatus() == DeliveryStatus.SIMULATED_DISPATCH && overallStatus != DeliveryStatus.GATEWAY_FAILED) {
                overallStatus = DeliveryStatus.SIMULATED_DISPATCH;
            }
            primaryChannel = channel;
        }

        String recipientSummary = String.join(", ", recipients);
        if (recipientSummary.length() > 150) {
            recipientSummary = recipientSummary.substring(0, 147) + "...";
        }
        String providerRefSummary = String.join(", ", providerRefs);
        if (providerRefSummary.length() > 255) {
            providerRefSummary = providerRefSummary.substring(0, 252) + "...";
        }

        CaregiverEscalationLog escalationLog = CaregiverEscalationLog.builder()
                .patientId(patientId)
                .medicationId(medicationId)
                .medicationName(safeMedName)
                .streakStartDoseId(streakStartDoseId != null ? streakStartDoseId : UUID.randomUUID())
                .triggeringDoseId(triggeringDoseId != null ? triggeringDoseId : UUID.randomUUID())
                .consecutiveMissedCount(consecutiveMissedCount)
                .channel(primaryChannel)
                .recipient(recipientSummary)
                .deliveryStatus(overallStatus)
                .messageContent(alertMessage)
                .providerReference(providerRefSummary.isEmpty() ? null : providerRefSummary)
                .deliveryAttemptedAt(Instant.now())
                .build();

        try {
            escalationLogRepository.save(escalationLog);
            log.info("Persisted caregiver escalation log [{}] for medication [{}] (streak: {}) to recipients [{}]",
                    escalationLog.getId(), medicationId, streakStartDoseId, recipientSummary);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("Caregiver escalation record already exists for medication [{}] and streak start dose [{}]. Skipped duplicate insert.",
                    medicationId, streakStartDoseId);
        }
    }

    private String resolveRecipient(Caregiver caregiver, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL) {
            if (caregiver.getEmail() != null && !caregiver.getEmail().isBlank()) {
                return caregiver.getEmail();
            }
            return caregiver.getPhoneNumber() != null ? caregiver.getPhoneNumber() : "unknown-recipient";
        }
        if (caregiver.getPhoneNumber() != null && !caregiver.getPhoneNumber().isBlank()) {
            return caregiver.getPhoneNumber();
        }
        return caregiver.getEmail() != null ? caregiver.getEmail() : "unknown-recipient";
    }
}
