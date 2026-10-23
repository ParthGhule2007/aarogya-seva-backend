package com.example.consultationservice.service;

import com.example.consultationservice.client.CommunicationServiceClient;
import com.example.consultationservice.client.DoctorServiceClient;
import com.example.consultationservice.dto.CancelConsultationDTO;
import com.example.consultationservice.dto.CommunicationSessionResponseDTO;
import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.*;
import com.example.consultationservice.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class ConsultationConcurrencyTest {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private ConsultationRepository consultationRepository;

    @MockBean
    private DoctorServiceClient doctorServiceClient;

    @MockBean
    private CommunicationServiceClient communicationServiceClient;

    @BeforeEach
    void setUp() {
        consultationRepository.deleteAll();
        when(doctorServiceClient.updateDoctorAvailability(anyLong(), any(), anyString()))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.createSession(any())).thenReturn(
                CommunicationSessionResponseDTO.builder()
                        .id(1L)
                        .roomId("consultation-room-concurrency")
                        .meetingUrl("https://meet.jit.si/consultation-room-concurrency")
                        .status("CREATED")
                        .build()
        );
    }

    // 21. Simulate accept + cancel race condition and ensure only one valid state wins
    @Test
    @DisplayName("21. Simulate accept + cancel race condition: DB and state machine enforce consistent final state")
    void testConcurrentAcceptAndCancel_ConsistentFinalState() throws Exception {
        Consultation consultation = Consultation.builder()
                .userId("102")
                .username("Chanchal")
                .doctorId(52L)
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .status(ConsultationStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
        Consultation saved = consultationRepository.saveAndFlush(consultation);
        final Long consultationId = saved.getId();

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        // Thread 1: Accept
        executor.submit(() -> {
            try {
                startLatch.await();
                consultationService.acceptConsultation(consultationId);
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 2: Cancel
        executor.submit(() -> {
            try {
                startLatch.await();
                consultationService.cancelConsultation(consultationId, new CancelConsultationDTO(CancelledBy.USER));
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify final state in database is consistent (either ACCEPTED or CANCELLED, never in an invalid state)
        Consultation finalConsultation = consultationRepository.findById(consultationId).orElseThrow();
        ConsultationStatus finalStatus = finalConsultation.getStatus();

        assertTrue(finalStatus == ConsultationStatus.ACCEPTED || finalStatus == ConsultationStatus.CANCELLED,
                "Final state must be valid (ACCEPTED or CANCELLED), but was: " + finalStatus);

        // Once cancelled, cannot accept again
        if (finalStatus == ConsultationStatus.CANCELLED) {
            assertThrows(Exception.class, () -> consultationService.acceptConsultation(consultationId));
        }
    }
}
