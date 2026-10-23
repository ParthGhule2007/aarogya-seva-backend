package com.example.consultationservice.scheduler;

import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.*;
import com.example.consultationservice.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.consultationservice.client.CommunicationServiceClient;
import com.example.consultationservice.client.DoctorServiceClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduledConsultationTaskTest {

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private ScheduledConsultationTask scheduledConsultationTask;

    @MockBean
    private DoctorServiceClient doctorServiceClient;

    @MockBean
    private CommunicationServiceClient communicationServiceClient;

    @BeforeEach
    void setUp() {
        consultationRepository.deleteAll();
    }

    // 17. Scheduled consultation becomes READY when scheduled time is reached
    @Test
    @DisplayName("17. Scheduled consultation automatically transitions to READY when scheduled time is reached")
    void testScheduledConsultation_BecomesReady_WhenTimeReached() {
        // Create a scheduled consultation whose scheduled time is in the past (due for READY)
        Consultation dueConsultation = Consultation.builder()
                .userId("102")
                .username("Chanchal")
                .doctorId(52L)
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .status(ConsultationStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();
        Consultation savedDue = consultationRepository.save(dueConsultation);

        // Create another consultation whose scheduled time is in the future (not due)
        Consultation futureConsultation = Consultation.builder()
                .userId("103")
                .username("John")
                .doctorId(52L)
                .disease("Fever")
                .urgency(Urgency.LOW)
                .consultationType(ConsultationType.AUDIO)
                .status(ConsultationStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusHours(2))
                .build();
        Consultation savedFuture = consultationRepository.save(futureConsultation);

        // Run the scheduled task
        scheduledConsultationTask.processScheduledConsultations();

        // Verify due consultation transitioned to READY
        Consultation updatedDue = consultationRepository.findById(savedDue.getId()).orElseThrow();
        assertEquals(ConsultationStatus.READY, updatedDue.getStatus());

        // Verify future consultation remains SCHEDULED
        Consultation updatedFuture = consultationRepository.findById(savedFuture.getId()).orElseThrow();
        assertEquals(ConsultationStatus.SCHEDULED, updatedFuture.getStatus());
    }
}
