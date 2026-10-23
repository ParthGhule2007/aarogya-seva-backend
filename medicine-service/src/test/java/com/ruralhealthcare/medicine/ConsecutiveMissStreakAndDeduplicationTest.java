package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.*;
import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.repository.CaregiverEscalationLogRepository;
import com.ruralhealthcare.medicine.repository.CaregiverRepository;
import com.ruralhealthcare.medicine.repository.DoseEventRepository;
import com.ruralhealthcare.medicine.repository.MedicationRepository;
import com.ruralhealthcare.medicine.service.CaregiverService;
import com.ruralhealthcare.medicine.service.DoseEventService;
import com.ruralhealthcare.medicine.service.MedicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConsecutiveMissStreakAndDeduplicationTest {

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private DoseEventService doseEventService;

    @Autowired
    private CaregiverService caregiverService;

    @Autowired
    private DoseEventRepository doseEventRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private CaregiverEscalationLogRepository escalationLogRepository;

    @Autowired
    private CaregiverRepository caregiverRepository;

    @Test
    @DisplayName("Consecutive missed streak triggers escalation once per streak, ignores SKIPPED, and resets on TAKEN")
    void testOncePerStreakCaregiverEscalationAndReset() {
        UUID patientId = UUID.randomUUID();

        // 1. Register an active caregiver for the patient
        CaregiverRequest caregiverRequest = new CaregiverRequest(
                patientId, "Ramesh Sharma", "Son", "+919876543210", "ramesh@example.com", NotificationChannel.WHATSAPP
        );
        caregiverService.createCaregiver(caregiverRequest);

        // 2. Create medication (Mode C, no photo)
        CreateMedicationRequest medRequest = new CreateMedicationRequest(
                patientId, "Amlodipine 5mg", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(8, 0)),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                FoodInstruction.AFTER_FOOD, "Daily morning"
        );
        MedicationResponse medResponse = medicationService.createMedication(medRequest, null);
        Medication medication = medicationRepository.findById(medResponse.getId()).orElseThrow();
        // Clear any auto-generated dose events to start with clean synthetic chronological sequence
        doseEventRepository.deleteAll(doseEventRepository.findByMedicationIdOrderByScheduledDateAscScheduledTimeAsc(medication.getId()));

        // Helper to create synthetic chronological doses
        LocalDate date = LocalDate.now().minusDays(5);

        // Dose 1: Scheduled 5 days ago
        DoseEvent dose1 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 2: Scheduled 4 days ago
        DoseEvent dose2 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(1)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 3: Scheduled 3 days ago
        DoseEvent dose3 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(2)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 4: Scheduled 2 days ago (Will be SKIPPED)
        DoseEvent dose4 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(3)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 5: Scheduled 1 day ago (Will be TAKEN - streak reset)
        DoseEvent dose5 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(4)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 6 & 7: Scheduled today (New streak start)
        DoseEvent dose6 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(5)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        DoseEvent dose7 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(date.plusDays(5)).scheduledTime(LocalTime.of(20, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Step 1: Mark Dose 1 MISSED -> Count = 1, Threshold (2) not met
        doseEventService.markDoseMissed(dose1.getId());
        List<CaregiverEscalationLog> logsAfterDose1 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(0, logsAfterDose1.size(), "No alert should trigger on 1st missed dose");

        // Step 2: Mark Dose 2 MISSED -> Count = 2, Threshold (2) REACHED!
        doseEventService.markDoseMissed(dose2.getId());
        List<CaregiverEscalationLog> logsAfterDose2 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logsAfterDose2.size(), "Alert should trigger exactly once when threshold is reached");
        CaregiverEscalationLog logEntry1 = logsAfterDose2.get(0);
        assertEquals(dose1.getId(), logEntry1.getStreakStartDoseId(), "Streak start anchor must be dose 1");
        assertEquals(dose2.getId(), logEntry1.getTriggeringDoseId(), "Triggering dose must be dose 2");
        assertEquals(2, logEntry1.getConsecutiveMissedCount());
        assertEquals(NotificationChannel.WHATSAPP, logEntry1.getChannel());
        assertEquals(DeliveryStatus.SIMULATED_DISPATCH, logEntry1.getDeliveryStatus());

        // Step 3: Mark Dose 3 MISSED -> Count = 3, Deduplication: still in same streak!
        doseEventService.markDoseMissed(dose3.getId());
        List<CaregiverEscalationLog> logsAfterDose3 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logsAfterDose3.size(), "Deduplication must prevent sending another alert in the same consecutive streak");

        // Step 4: Mark Dose 4 as SKIPPED -> SKIPPED is neutral
        doseEventService.markDoseSkipped(dose4.getId());
        List<CaregiverEscalationLog> logsAfterDose4 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logsAfterDose4.size(), "SKIPPED dose must not affect escalation logs");

        // Step 5: Mark Dose 5 as TAKEN -> Streak is RESET!
        doseEventService.markDoseTaken(dose5.getId());
        int activeStreakAfterTaken = doseEventService.countConsecutiveMissedDoses(medication.getId());
        assertEquals(0, activeStreakAfterTaken, "Consecutive missed count must reset to 0 after TAKEN dose");

        // Step 6: Mark Dose 6 MISSED -> New streak starts! Count = 1
        doseEventService.markDoseMissed(dose6.getId());
        assertEquals(1, doseEventService.countConsecutiveMissedDoses(medication.getId()));
        assertEquals(1, escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId).size());

        // Step 7: Mark Dose 7 MISSED -> New streak reaches threshold 2! New alert should fire!
        doseEventService.markDoseMissed(dose7.getId());
        List<CaregiverEscalationLog> logsAfterDose7 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(2, logsAfterDose7.size(), "A new alert must fire for the new streak after a TAKEN reset");

        CaregiverEscalationLog logEntry2 = logsAfterDose7.get(0); // ordered by createdAt desc
        assertEquals(dose6.getId(), logEntry2.getStreakStartDoseId(), "New streak start anchor must be dose 6");
        assertEquals(dose7.getId(), logEntry2.getTriggeringDoseId(), "Triggering dose for second streak must be dose 7");
    }

    @Test
    @DisplayName("Patient-level consecutive misses: 2 missed doses across different medicines trigger escalation, TAKEN resets streak, deduplication works")
    void testPatientLevelConsecutiveMissesAcrossDifferentMedications() {
        UUID patientId = UUID.randomUUID();

        // Register active caregiver
        CaregiverRequest caregiverReq = new CaregiverRequest(
                patientId, "Anand Kumar", "Brother", "+919876543299",
                "anand@example.com", NotificationChannel.EMAIL
        );
        caregiverService.createCaregiver(caregiverReq);

        // Create Medication 1 (Metformin, 8 AM)
        CreateMedicationRequest medReq1 = new CreateMedicationRequest(
                patientId, "Metformin 500mg", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(8, 0)),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                FoodInstruction.WITH_FOOD, "Morning"
        );
        MedicationResponse medResp1 = medicationService.createMedication(medReq1, null);
        Medication med1 = medicationRepository.findById(medResp1.getId()).orElseThrow();

        // Create Medication 2 (Amlodipine, 8 PM)
        CreateMedicationRequest medReq2 = new CreateMedicationRequest(
                patientId, "Amlodipine 5mg", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(20, 0)),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                FoodInstruction.AFTER_FOOD, "Evening"
        );
        MedicationResponse medResp2 = medicationService.createMedication(medReq2, null);
        Medication med2 = medicationRepository.findById(medResp2.getId()).orElseThrow();

        // Clear pre-generated dose events
        doseEventRepository.deleteAll(doseEventRepository.findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(patientId));

        LocalDate baseDate = LocalDate.now().minusDays(5);

        // Dose 1: Med 1 at 08:00
        DoseEvent dose1 = doseEventRepository.save(DoseEvent.builder()
                .medication(med1).patientId(patientId)
                .scheduledDate(baseDate).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 2: Med 2 at 20:00
        DoseEvent dose2 = doseEventRepository.save(DoseEvent.builder()
                .medication(med2).patientId(patientId)
                .scheduledDate(baseDate).scheduledTime(LocalTime.of(20, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 3: Med 1 next day at 08:00
        DoseEvent dose3 = doseEventRepository.save(DoseEvent.builder()
                .medication(med1).patientId(patientId)
                .scheduledDate(baseDate.plusDays(1)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 4: Med 2 next day at 20:00 (will be TAKEN to reset streak)
        DoseEvent dose4 = doseEventRepository.save(DoseEvent.builder()
                .medication(med2).patientId(patientId)
                .scheduledDate(baseDate.plusDays(1)).scheduledTime(LocalTime.of(20, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 5: Med 1 day 3 at 08:00
        DoseEvent dose5 = doseEventRepository.save(DoseEvent.builder()
                .medication(med1).patientId(patientId)
                .scheduledDate(baseDate.plusDays(2)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 6: Med 2 day 3 at 20:00
        DoseEvent dose6 = doseEventRepository.save(DoseEvent.builder()
                .medication(med2).patientId(patientId)
                .scheduledDate(baseDate.plusDays(2)).scheduledTime(LocalTime.of(20, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Step 1: Mark Dose 1 (Med 1) MISSED -> Count = 1, Threshold not met
        doseEventService.markDoseMissed(dose1.getId());
        List<CaregiverEscalationLog> logs1 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(0, logs1.size(), "No escalation when only 1 dose of Med 1 is missed");

        // Step 2: Mark Dose 2 (Med 2) MISSED -> Count = 2 across DIFFERENT medicines! Threshold met!
        doseEventService.markDoseMissed(dose2.getId());
        List<CaregiverEscalationLog> logs2 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs2.size(), "Escalation must trigger when patient misses 2 consecutive doses across different medicines");
        CaregiverEscalationLog entry1 = logs2.get(0);
        assertEquals(dose1.getId(), entry1.getStreakStartDoseId(), "Streak start must be Dose 1 (Med 1)");
        assertEquals(dose2.getId(), entry1.getTriggeringDoseId(), "Triggering dose must be Dose 2 (Med 2)");
        assertEquals(2, entry1.getConsecutiveMissedCount());

        // Step 3: Mark Dose 3 (Med 1) MISSED -> Count = 3, Duplicate prevention in same streak
        doseEventService.markDoseMissed(dose3.getId());
        List<CaregiverEscalationLog> logs3 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs3.size(), "Duplicate alert must be prevented within same multi-medication streak");

        // Step 4: Mark Dose 4 (Med 2) TAKEN -> Reset streak
        doseEventService.markDoseTaken(dose4.getId());

        // Step 5: Mark Dose 5 (Med 1) MISSED -> Count = 1 after reset, no new escalation
        doseEventService.markDoseMissed(dose5.getId());
        List<CaregiverEscalationLog> logs5 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs5.size(), "Count is 1 after reset, no new alert yet");

        // Step 6: Mark Dose 6 (Med 2) MISSED -> Count = 2, New streak triggers 2nd escalation!
        doseEventService.markDoseMissed(dose6.getId());
        List<CaregiverEscalationLog> logs6 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(2, logs6.size(), "New streak across different medicines triggers second escalation");
        CaregiverEscalationLog entry2 = logs6.get(0); // latest
        assertEquals(dose5.getId(), entry2.getStreakStartDoseId(), "New streak start must be Dose 5");
        assertEquals(dose6.getId(), entry2.getTriggeringDoseId(), "New triggering dose must be Dose 6");
    }
}
