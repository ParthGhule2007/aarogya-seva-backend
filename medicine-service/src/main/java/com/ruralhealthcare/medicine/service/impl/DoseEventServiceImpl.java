package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.domain.*;
import com.ruralhealthcare.medicine.dto.DoseEventResponse;
import com.ruralhealthcare.medicine.exception.InvalidStateTransitionException;
import com.ruralhealthcare.medicine.exception.ResourceNotFoundException;
import com.ruralhealthcare.medicine.repository.CaregiverEscalationLogRepository;
import com.ruralhealthcare.medicine.repository.DoseEventRepository;
import com.ruralhealthcare.medicine.repository.MedicationRepository;
import com.ruralhealthcare.medicine.security.SecurityUtils;
import com.ruralhealthcare.medicine.service.CaregiverNotificationService;
import com.ruralhealthcare.medicine.service.DoseEventService;
import com.ruralhealthcare.medicine.service.NotificationService;
import com.ruralhealthcare.medicine.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DoseEventServiceImpl implements DoseEventService {

    private static final Logger log = LoggerFactory.getLogger(DoseEventServiceImpl.class);

    private final DoseEventRepository doseEventRepository;
    private final MedicationRepository medicationRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final CaregiverNotificationService caregiverNotificationService;
    private final CaregiverEscalationLogRepository escalationLogRepository;
    private final SecurityUtils securityUtils;
    private final MedicineProperties properties;

    public DoseEventServiceImpl(DoseEventRepository doseEventRepository,
                                MedicationRepository medicationRepository,
                                StorageService storageService,
                                NotificationService notificationService,
                                CaregiverNotificationService caregiverNotificationService,
                                CaregiverEscalationLogRepository escalationLogRepository,
                                SecurityUtils securityUtils,
                                MedicineProperties properties) {
        this.doseEventRepository = doseEventRepository;
        this.medicationRepository = medicationRepository;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.caregiverNotificationService = caregiverNotificationService;
        this.escalationLogRepository = escalationLogRepository;
        this.securityUtils = securityUtils;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoseEventResponse> getTodayDosesForPatient(UUID patientId) {
        securityUtils.assertPatientAccess(patientId);
        LocalDate today = LocalDate.now();
        List<DoseEvent> events = doseEventRepository.findByPatientIdAndScheduledDateOrderByScheduledTimeAsc(patientId, today);
        return events.stream()
                .filter(e -> e.getMedication() != null &&
                        e.getMedication().getStatus() != com.ruralhealthcare.medicine.domain.MedicationStatus.DEACTIVATED &&
                        e.getMedication().getStatus() != com.ruralhealthcare.medicine.domain.MedicationStatus.DISCONTINUED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoseEventResponse> getDoseHistoryForPatient(UUID patientId) {
        securityUtils.assertPatientAccess(patientId);
        List<DoseEvent> events = doseEventRepository.findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(patientId);
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public DoseEventResponse markDoseTaken(UUID doseEventId) {
        DoseEvent event = doseEventRepository.findById(doseEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose event not found with id: " + doseEventId));

        securityUtils.assertPatientAccess(event.getPatientId());

        if (event.getStatus() == DoseStatus.TAKEN) {
            log.info("Dose [{}] is already marked as TAKEN. Idempotent return.", doseEventId);
            return mapToResponse(event);
        }

        event.markTaken();
        DoseEvent saved = doseEventRepository.save(event);
        log.info("Dose [{}] for patient [{}] successfully marked as TAKEN at {}",
                doseEventId, saved.getPatientId(), saved.getTakenAt());

        return mapToResponse(saved);
    }

    @Override
    public DoseEventResponse markDoseMissed(UUID doseEventId) {
        DoseEvent event = doseEventRepository.findById(doseEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose event not found with id: " + doseEventId));

        securityUtils.assertPatientAccess(event.getPatientId());

        if (event.getStatus() == DoseStatus.TAKEN) {
            throw new InvalidStateTransitionException("Cannot mark a TAKEN dose as MISSED");
        }

        if (event.getStatus() == DoseStatus.MISSED) {
            return mapToResponse(event);
        }

        event.markMissed();
        DoseEvent saved = doseEventRepository.save(event);
        log.info("Dose [{}] for patient [{}] marked as MISSED at {}", doseEventId, saved.getPatientId(), saved.getMissedAt());

        // Check consecutive missed dose count for threshold escalation
        checkConsecutiveMissedThreshold(saved.getMedication(), saved.getPatientId(), saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public DoseEventResponse markDoseSkipped(UUID doseEventId) {
        DoseEvent event = doseEventRepository.findById(doseEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose event not found with id: " + doseEventId));

        securityUtils.assertPatientAccess(event.getPatientId());

        if (event.getStatus() == DoseStatus.TAKEN) {
            throw new InvalidStateTransitionException("Cannot mark a TAKEN dose as SKIPPED");
        }

        if (event.getStatus() == DoseStatus.SKIPPED) {
            log.info("Dose [{}] is already marked as SKIPPED. Idempotent return.", doseEventId);
            return mapToResponse(event);
        }

        event.setStatus(DoseStatus.SKIPPED);
        DoseEvent saved = doseEventRepository.save(event);
        log.info("Dose [{}] for patient [{}] intentionally SKIPPED", doseEventId, saved.getPatientId());

        return mapToResponse(saved);
    }

    @Override
    public void generateTodayDoseEvents() {
        LocalDate today = LocalDate.now();
        List<Medication> activeMeds = medicationRepository.findByStatus(MedicationStatus.ACTIVE);

        for (Medication med : activeMeds) {
            if (today.isBefore(med.getStartDate())) {
                continue;
            }
            if (med.getEndDate() != null && today.isAfter(med.getEndDate())) {
                continue;
            }

            for (MedicationReminderTime time : med.getReminderTimes()) {
                List<DoseEvent> existing = doseEventRepository.findByMedicationIdAndScheduledDateAndScheduledTime(
                        med.getId(), today, time.getReminderTime());

                if (existing.isEmpty()) {
                    DoseEvent event = DoseEvent.builder()
                            .medication(med)
                            .patientId(med.getPatientId())
                            .scheduledDate(today)
                            .scheduledTime(time.getReminderTime())
                            .status(DoseStatus.PENDING)
                            .notificationTriggered(false)
                            .build();
                    doseEventRepository.save(event);
                    log.debug("Created scheduled dose for med [{}] at {} {}", med.getId(), today, time.getReminderTime());
                }
            }
        }
    }

    @Override
    public void triggerPendingReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<DoseEvent> pendingDoses = doseEventRepository.findPendingDosesNeedingNotification(today, now);

        for (DoseEvent dose : pendingDoses) {
            dose.setNotificationTriggered(true);
            dose.setNotificationTime(Instant.now());
            doseEventRepository.save(dose);

            String medName = dose.getMedication() != null ? dose.getMedication().getName() : "medicine";
            notificationService.sendMedicineReminder(
                    dose.getPatientId(),
                    dose.getId(),
                    "Medicine Reminder: " + medName,
                    "It is time to take your " + medName + "."
            );
            log.info("Triggered reminder notification for dose event [{}] to patient [{}]", dose.getId(), dose.getPatientId());
        }
    }

    @Override
    public void checkGracePeriodAndMarkMissed() {
        LocalDate today = LocalDate.now();
        int graceMinutes = properties.getReminderGracePeriodMinutes();
        LocalTime cutoffTime = LocalTime.now().minusMinutes(graceMinutes);

        List<DoseEvent> overdueDoses = doseEventRepository.findPendingDosesPastGracePeriod(today, cutoffTime);

        for (DoseEvent dose : overdueDoses) {
            dose.markMissed();
            doseEventRepository.save(dose);
            log.warn("Dose [{}] scheduled for {} exceeded {} min grace period. Auto-marked MISSED.",
                    dose.getId(), dose.getScheduledTime(), graceMinutes);
        }

        doseEventRepository.flush();

        // Evaluate consecutive missed threshold strictly once per distinct patient in this run
        java.util.Set<UUID> evaluatedPatientIds = new java.util.HashSet<>();
        for (DoseEvent dose : overdueDoses) {
            if (dose.getPatientId() != null && evaluatedPatientIds.add(dose.getPatientId())) {
                checkConsecutiveMissedThreshold(dose.getMedication(), dose.getPatientId(), dose.getId());
            }
        }
    }

    @Override
    public int countConsecutiveMissedDoses(UUID medicationId) {
        List<DoseEvent> history = doseEventRepository.findByMedicationIdOrderByScheduledDateAscScheduledTimeAsc(medicationId);
        int lastTakenIndex = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getStatus() == DoseStatus.TAKEN) {
                lastTakenIndex = i;
            }
        }

        int missedCount = 0;
        for (int i = lastTakenIndex + 1; i < history.size(); i++) {
            DoseEvent dose = history.get(i);
            if (dose.getStatus() == DoseStatus.MISSED) {
                missedCount++;
            }
            // SKIPPED is neutral, PENDING is future/unanswered
        }
        return missedCount;
    }

    private void checkConsecutiveMissedThreshold(Medication medication, UUID patientId, UUID triggeringDoseId) {
        if (patientId == null) {
            return;
        }

        List<DoseEvent> history = doseEventRepository.findByPatientIdOrderByScheduledDateAscScheduledTimeAsc(patientId);
        int lastTakenIndex = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getStatus() == DoseStatus.TAKEN) {
                lastTakenIndex = i;
            }
        }

        int missedCount = 0;
        DoseEvent streakStartDose = null;

        for (int i = lastTakenIndex + 1; i < history.size(); i++) {
            DoseEvent dose = history.get(i);
            if (dose.getStatus() == DoseStatus.MISSED) {
                missedCount++;
                if (streakStartDose == null) {
                    streakStartDose = dose;
                }
            }
            // SKIPPED is neutral, PENDING is future/unanswered
        }

        int threshold = properties.getMissedDoseThreshold();
        if (missedCount >= threshold && streakStartDose != null) {
            UUID streakStartDoseId = streakStartDose.getId();
            Medication targetMed = medication != null ? medication : streakStartDose.getMedication();
            UUID targetMedId = targetMed != null ? targetMed.getId() : streakStartDose.getMedication().getId();
            String targetMedName = targetMed != null ? targetMed.getName() : "Prescribed Medicine";

            // Deduplicate: Dispatch escalation alert strictly once per streak
            if (escalationLogRepository.existsByStreakStartDoseId(streakStartDoseId)
                    || escalationLogRepository.existsByMedicationIdAndStreakStartDoseId(targetMedId, streakStartDoseId)) {
                log.info("Caregiver alert already sent for streak start dose [{}]. Skipping duplicate alert.",
                        streakStartDoseId);
                return;
            }

            caregiverNotificationService.notifyCaregiverConsecutiveMissedDoses(
                    patientId,
                    targetMedId,
                    targetMedName,
                    streakStartDoseId,
                    triggeringDoseId,
                    missedCount
            );
        }
    }

    private DoseEventResponse mapToResponse(DoseEvent event) {
        Medication med = event.getMedication();
        String imageUrl = (med.getImageKey() != null)
                ? storageService.getPresignedImageUrl(med.getImageKey())
                : null;

        return DoseEventResponse.builder()
                .id(event.getId())
                .medicationId(med.getId())
                .medicationName(med.getName())
                .patientId(event.getPatientId())
                .imageKey(med.getImageKey())
                .imageUrl(imageUrl)
                .dosageAmount(med.getDosageAmount())
                .dosageUnit(med.getDosageUnit())
                .foodInstruction(med.getFoodInstruction())
                .instructions(med.getInstructions())
                .scheduledDate(event.getScheduledDate())
                .scheduledTime(event.getScheduledTime())
                .status(event.getStatus())
                .takenAt(event.getTakenAt())
                .missedAt(event.getMissedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
