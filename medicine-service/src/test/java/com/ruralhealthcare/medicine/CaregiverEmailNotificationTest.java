package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.*;
import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.repository.CaregiverEscalationLogRepository;
import com.ruralhealthcare.medicine.repository.CaregiverRepository;
import com.ruralhealthcare.medicine.repository.DoseEventRepository;
import com.ruralhealthcare.medicine.repository.MedicationRepository;
import com.ruralhealthcare.medicine.service.CaregiverNotificationService;
import com.ruralhealthcare.medicine.service.CaregiverService;
import com.ruralhealthcare.medicine.service.DoseEventService;
import com.ruralhealthcare.medicine.service.MedicationService;
import com.ruralhealthcare.medicine.service.channel.DeliveryResult;
import com.ruralhealthcare.medicine.service.channel.GmailSmtpEmailSender;
import com.ruralhealthcare.medicine.service.channel.MockEmailSender;
import com.ruralhealthcare.medicine.service.impl.CaregiverNotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CaregiverEmailNotificationTest {

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
    private CaregiverRepository caregiverRepository;

    @Autowired
    private CaregiverEscalationLogRepository escalationLogRepository;

    @Test
    @DisplayName("Unit: GmailSmtpEmailSender dispatches email successfully via JavaMailSender")
    void testGmailSmtpSenderSuccessfulDispatch() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        GmailSmtpEmailSender sender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        DeliveryResult result = sender.send("caregiver.test@example.com", "Patient missed 2 doses of Metformin.");

        assertEquals(DeliveryStatus.GATEWAY_ACCEPTED, result.getStatus());
        assertNotNull(result.getProviderReference());
        assertTrue(result.getProviderReference().startsWith("GMAIL-SMTP-"));
        assertNull(result.getErrorMessage());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mockMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage captured = messageCaptor.getValue();
        assertEquals("caregiver.test@example.com", captured.getTo()[0]);
        assertEquals("aarogya.alerts@gmail.com", captured.getFrom());
        assertEquals("Aarogya Seva: Medicine Reminder Alert", captured.getSubject());
        assertTrue(captured.getText().contains("Patient missed 2 doses of Metformin."));
        assertFalse(captured.getText().contains("password"));
    }

    @Test
    @DisplayName("Unit: GmailSmtpEmailSender rejects invalid recipient formats before attempting SMTP")
    void testGmailSmtpSenderInvalidRecipients() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        GmailSmtpEmailSender sender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        String[] invalidEmails = {null, "", "   ", "plainaddress", "missingatsign.com", "@domain.com", "user@"};

        for (String invalid : invalidEmails) {
            DeliveryResult result = sender.send(invalid, "Alert text");
            assertEquals(DeliveryStatus.GATEWAY_FAILED, result.getStatus(), "Failed for input: " + invalid);
            assertEquals("Invalid recipient email address", result.getErrorMessage());
        }

        verify(mockMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Unit: GmailSmtpEmailSender handles SMTP exceptions safely without uncaught errors")
    void testGmailSmtpSenderSmtpFailures() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("Authentication failed 535-5.7.8"))
                .when(mockMailSender).send(any(SimpleMailMessage.class));

        GmailSmtpEmailSender sender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        DeliveryResult result = sender.send("caregiver@example.com", "Patient missed dose.");

        assertEquals(DeliveryStatus.GATEWAY_FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("SMTP delivery failed"));

        // Test network/connection failure
        doThrow(new MailSendException("Could not connect to SMTP host: smtp.gmail.com, port: 587"))
                .when(mockMailSender).send(any(SimpleMailMessage.class));

        DeliveryResult connResult = sender.send("caregiver@example.com", "Patient missed dose.");
        assertEquals(DeliveryStatus.GATEWAY_FAILED, connResult.getStatus());
        assertTrue(connResult.getErrorMessage().contains("Could not connect to SMTP host"));
    }

    @Test
    @DisplayName("Unit: MockEmailSender returns SIMULATED_DISPATCH by default")
    void testMockEmailSenderBehavior() {
        MockEmailSender mockSender = new MockEmailSender();
        assertEquals(NotificationChannel.EMAIL, mockSender.getChannel());

        DeliveryResult result = mockSender.send("caregiver@example.com", "Mock alert");
        assertEquals(DeliveryStatus.SIMULATED_DISPATCH, result.getStatus());
        assertTrue(result.getProviderReference().startsWith("SIMULATED-EMAIL-"));
    }

    @Test
    @DisplayName("Integration: Escalation threshold, TAKEN reset, and duplicate prevention with Email channel")
    void testCaregiverEmailEscalationFlowWithJavaMailSender() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        GmailSmtpEmailSender emailSender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        // Custom instance of CaregiverNotificationServiceImpl with GmailSmtpEmailSender
        CaregiverNotificationService notificationServiceWithSmtp = new CaregiverNotificationServiceImpl(
                caregiverRepository,
                escalationLogRepository,
                medicationRepository,
                List.of(emailSender)
        );

        UUID patientId = UUID.randomUUID();

        // 1. Register active caregiver with EMAIL preferred channel
        CaregiverRequest caregiverReq = new CaregiverRequest(
                patientId, "Dr. Priya Verma", "Physician", "+919876543210",
                "priya.verma@example.com", NotificationChannel.EMAIL
        );
        caregiverService.createCaregiver(caregiverReq);

        // 2. Create medication
        CreateMedicationRequest medReq = new CreateMedicationRequest(
                patientId, "Metformin 500mg", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(8, 0)),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                FoodInstruction.WITH_FOOD, "Morning"
        );
        MedicationResponse medResp = medicationService.createMedication(medReq, null);
        Medication medication = medicationRepository.findById(medResp.getId()).orElseThrow();

        // Clear pre-generated dose events
        doseEventRepository.deleteAll(doseEventRepository.findByMedicationIdOrderByScheduledDateAscScheduledTimeAsc(medication.getId()));

        LocalDate baseDate = LocalDate.now().minusDays(5);

        // Create 4 chronological doses
        DoseEvent dose1 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(baseDate).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        DoseEvent dose2 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(baseDate.plusDays(1)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        DoseEvent dose3 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(baseDate.plusDays(2)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        DoseEvent dose4 = doseEventRepository.save(DoseEvent.builder()
                .medication(medication).patientId(patientId)
                .scheduledDate(baseDate.plusDays(3)).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 1 MISSED -> Count = 1, Threshold (2) not met -> No email dispatched
        doseEventService.markDoseMissed(dose1.getId());
        List<CaregiverEscalationLog> logs1 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(0, logs1.size(), "Threshold 2 not met, no email should be dispatched");
        verify(mockMailSender, never()).send(any(SimpleMailMessage.class));

        // Dose 2 MISSED -> Count = 2, Threshold reached! Email dispatched!
        // We trigger escalation through our notificationServiceWithSmtp
        notificationServiceWithSmtp.notifyCaregiverConsecutiveMissedDoses(
                patientId, medication.getId(), medication.getName(),
                dose1.getId(), dose2.getId(), 2
        );

        List<CaregiverEscalationLog> logs2 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs2.size(), "Escalation log must be recorded upon reaching threshold");
        CaregiverEscalationLog logEntry = logs2.get(0);
        assertEquals(NotificationChannel.EMAIL, logEntry.getChannel());
        assertEquals("priya.verma@example.com", logEntry.getRecipient());
        assertEquals(DeliveryStatus.GATEWAY_ACCEPTED, logEntry.getDeliveryStatus());
        verify(mockMailSender, times(1)).send(any(SimpleMailMessage.class));

        // Dose 3 MISSED -> Duplicate prevention: same streak must not re-dispatch email
        notificationServiceWithSmtp.notifyCaregiverConsecutiveMissedDoses(
                patientId, medication.getId(), medication.getName(),
                dose1.getId(), dose3.getId(), 3
        );
        List<CaregiverEscalationLog> logs3 = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs3.size(), "Deduplication must prevent duplicate email in same streak");
        verify(mockMailSender, times(1)).send(any(SimpleMailMessage.class)); // still only 1 invocation

        // Dose 4 TAKEN -> Streak is reset
        doseEventService.markDoseTaken(dose4.getId());
        assertEquals(0, doseEventService.countConsecutiveMissedDoses(medication.getId()), "TAKEN must reset missed streak to 0");
    }

    @Test
    @DisplayName("Integration: SMTP failure records GATEWAY_FAILED in escalation log and does not claim success")
    void testCaregiverEmailEscalationSmtpFailureRecordsGatewayFailedAudit() {
        JavaMailSender failingMailSender = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("Bad credentials"))
                .when(failingMailSender).send(any(SimpleMailMessage.class));

        GmailSmtpEmailSender emailSender = new GmailSmtpEmailSender(failingMailSender, "aarogya.alerts@gmail.com");

        CaregiverNotificationService notificationServiceWithSmtp = new CaregiverNotificationServiceImpl(
                caregiverRepository,
                escalationLogRepository,
                medicationRepository,
                List.of(emailSender)
        );

        UUID patientId = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        UUID streakStartId = UUID.randomUUID();
        UUID triggerId = UUID.randomUUID();

        CaregiverRequest caregiverReq = new CaregiverRequest(
                patientId, "Sunil Gupta", "Brother", "+919876543211",
                "sunil.gupta@example.com", NotificationChannel.EMAIL
        );
        caregiverService.createCaregiver(caregiverReq);

        notificationServiceWithSmtp.notifyCaregiverConsecutiveMissedDoses(
                patientId, medId, "Aspirin 75mg", streakStartId, triggerId, 2
        );

        List<CaregiverEscalationLog> logs = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs.size());
        CaregiverEscalationLog logEntry = logs.get(0);
        assertEquals(DeliveryStatus.GATEWAY_FAILED, logEntry.getDeliveryStatus(), "SMTP failure must be recorded as GATEWAY_FAILED");
        assertNotEquals(DeliveryStatus.GATEWAY_ACCEPTED, logEntry.getDeliveryStatus());
    }

    @Test
    @DisplayName("Integration: Multiple caregivers for same patient are all notified without violating uq_escalation_streak")
    void testCaregiverEscalationWithMultipleCaregiversHonorsUniqueIndex() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        GmailSmtpEmailSender emailSender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        CaregiverNotificationService notificationServiceWithSmtp = new CaregiverNotificationServiceImpl(
                caregiverRepository,
                escalationLogRepository,
                medicationRepository,
                List.of(emailSender)
        );

        UUID patientId = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        UUID streakStartId = UUID.randomUUID();
        UUID triggerId = UUID.randomUUID();

        // Register 2 caregivers for the same patient
        caregiverService.createCaregiver(new CaregiverRequest(
                patientId, "Caregiver One", "Mother", "+919876543201",
                "caregiver1@example.com", NotificationChannel.EMAIL
        ));
        caregiverService.createCaregiver(new CaregiverRequest(
                patientId, "Caregiver Two", "Father", "+919876543202",
                "caregiver2@example.com", NotificationChannel.EMAIL
        ));

        // Trigger escalation
        assertDoesNotThrow(() -> notificationServiceWithSmtp.notifyCaregiverConsecutiveMissedDoses(
                patientId, medId, "Metformin", streakStartId, triggerId, 2
        ));

        // Verify both caregivers received emails
        verify(mockMailSender, times(2)).send(any(SimpleMailMessage.class));

        // Verify exactly 1 escalation audit record exists for this streak
        List<CaregiverEscalationLog> logs = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs.size(), "Exactly one audit record must exist per streak to satisfy uq_escalation_streak");
        assertTrue(logs.get(0).getRecipient().contains("caregiver1@example.com"));
        assertTrue(logs.get(0).getRecipient().contains("caregiver2@example.com"));
    }

    @Test
    @DisplayName("Integration: Two missed doses across different medicines dispatches caregiver email via GmailSmtpEmailSender")
    void testEndToEndEmailEscalationAcrossDifferentMedications() {
        JavaMailSender mockMailSender = mock(JavaMailSender.class);
        GmailSmtpEmailSender emailSender = new GmailSmtpEmailSender(mockMailSender, "aarogya.alerts@gmail.com");

        CaregiverNotificationService notificationServiceWithSmtp = new CaregiverNotificationServiceImpl(
                caregiverRepository,
                escalationLogRepository,
                medicationRepository,
                List.of(emailSender)
        );

        UUID patientId = UUID.randomUUID();

        // Register caregiver with EMAIL preference
        caregiverService.createCaregiver(new CaregiverRequest(
                patientId, "Dr. Sanjay", "Physician", "+919876543255",
                "dr.sanjay@example.com", NotificationChannel.EMAIL
        ));

        // Create Medication 1 and 2
        CreateMedicationRequest medReq1 = new CreateMedicationRequest(
                patientId, "Metformin", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(8, 0)),
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5),
                FoodInstruction.WITH_FOOD, "Morning"
        );
        Medication med1 = medicationRepository.findById(medicationService.createMedication(medReq1, null).getId()).orElseThrow();

        CreateMedicationRequest medReq2 = new CreateMedicationRequest(
                patientId, "Atorvastatin", null, BigDecimal.valueOf(1.0),
                DosageUnit.TABLET, 1, List.of(LocalTime.of(20, 0)),
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5),
                FoodInstruction.AFTER_FOOD, "Night"
        );
        Medication med2 = medicationRepository.findById(medicationService.createMedication(medReq2, null).getId()).orElseThrow();

        // Clear pre-generated doses
        doseEventRepository.deleteAll(doseEventRepository.findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(patientId));

        LocalDate date = LocalDate.now().minusDays(1);
        DoseEvent dose1 = doseEventRepository.save(DoseEvent.builder()
                .medication(med1).patientId(patientId)
                .scheduledDate(date).scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        DoseEvent dose2 = doseEventRepository.save(DoseEvent.builder()
                .medication(med2).patientId(patientId)
                .scheduledDate(date).scheduledTime(LocalTime.of(20, 0))
                .status(DoseStatus.PENDING).notificationTriggered(false).build());

        // Dose 1 missed -> Dose 2 missed -> Trigger escalation via notificationServiceWithSmtp
        notificationServiceWithSmtp.notifyCaregiverConsecutiveMissedDoses(
                patientId, med2.getId(), med2.getName(), dose1.getId(), dose2.getId(), 2
        );

        // Verify email sent to caregiver
        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mockMailSender, times(1)).send(msgCaptor.capture());
        assertEquals("dr.sanjay@example.com", msgCaptor.getValue().getTo()[0]);
        assertTrue(msgCaptor.getValue().getText().contains("Atorvastatin"));

        // Verify escalation history for patient returns the log
        List<CaregiverEscalationLog> logs = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertEquals(1, logs.size());
        assertEquals(DeliveryStatus.GATEWAY_ACCEPTED, logs.get(0).getDeliveryStatus());
        assertEquals(dose1.getId(), logs.get(0).getStreakStartDoseId());
        assertEquals(dose2.getId(), logs.get(0).getTriggeringDoseId());
    }
}
