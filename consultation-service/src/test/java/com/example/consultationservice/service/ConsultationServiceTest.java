package com.example.consultationservice.service;

import com.example.consultationservice.client.CommunicationServiceClient;
import com.example.consultationservice.client.DoctorServiceClient;
import com.example.consultationservice.dto.*;
import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.*;
import com.example.consultationservice.exception.*;
import com.example.consultationservice.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private DoctorServiceClient doctorServiceClient;

    @Mock
    private CommunicationServiceClient communicationServiceClient;

    @InjectMocks
    private ConsultationServiceImpl consultationService;

    private Consultation sampleConsultation;

    @BeforeEach
    void setUp() {
        sampleConsultation = Consultation.builder()
                .id(5001L)
                .userId("102")
                .username("Chanchal")
                .doctorId(52L)
                .doctorName("Dr. Smith")
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .status(ConsultationStatus.REQUESTED)
                .requestedAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    // 1. Create consultation successfully
    @Test
    @DisplayName("1. Create consultation successfully with string userId")
    void createConsultation_Success() {
        ConsultationRequestDTO request = ConsultationRequestDTO.builder()
                .userId("102")
                .username("Chanchal")
                .doctorId(52L)
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .build();

        DoctorResponseDTO doctor = DoctorResponseDTO.builder()
                .id(52L)
                .name("Dr. Smith")
                .build();

        when(doctorServiceClient.getDoctorById(52L)).thenReturn(doctor);
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> {
            Consultation c = invocation.getArgument(0);
            c.setId(5001L);
            return c;
        });

        ConsultationResponseDTO response = consultationService.createConsultation(request);

        assertNotNull(response);
        assertEquals(5001L, response.getConsultationId());
        assertEquals("102", response.getUserId());
        assertEquals(52L, response.getDoctorId());
        assertEquals("Dr. Smith", response.getDoctorName());
        assertEquals(ConsultationStatus.REQUESTED, response.getStatus());
        assertEquals(Urgency.HIGH, response.getUrgency());
        assertEquals(ConsultationType.VIDEO, response.getConsultationType());
        assertNotNull(response.getRequestedAt());
    }

    // 1b. Create consultation successfully with CoreService Patient UUID
    @Test
    @DisplayName("1b. Create consultation with CoreService Patient UUID string")
    void createConsultation_WithUuidString() {
        String patientUuid = "550e8400-e29b-41d4-a716-446655440000";
        ConsultationRequestDTO request = ConsultationRequestDTO.builder()
                .userId(patientUuid)
                .username("Ramesh Kumar")
                .doctorId(52L)
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .build();

        DoctorResponseDTO doctor = DoctorResponseDTO.builder()
                .id(52L)
                .name("Dr. Smith")
                .build();

        when(doctorServiceClient.getDoctorById(52L)).thenReturn(doctor);
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> {
            Consultation c = invocation.getArgument(0);
            c.setId(5002L);
            return c;
        });

        ConsultationResponseDTO response = consultationService.createConsultation(request);

        assertNotNull(response);
        assertEquals(5002L, response.getConsultationId());
        assertEquals(patientUuid, response.getUserId());
        assertEquals(52L, response.getDoctorId());
    }

    // 2. Invalid user ID
    @Test
    @DisplayName("2. Cannot create consultation with invalid user ID")
    void createConsultation_InvalidUserId() {
        ConsultationRequestDTO request = ConsultationRequestDTO.builder()
                .userId("   ")
                .username("Chanchal")
                .doctorId(52L)
                .disease("Fever")
                .urgency(Urgency.LOW)
                .consultationType(ConsultationType.AUDIO)
                .build();

        assertThrows(IllegalArgumentException.class, () -> consultationService.createConsultation(request));
    }

    // 3. Invalid doctor ID
    @Test
    @DisplayName("3. Cannot create consultation with invalid doctor ID")
    void createConsultation_InvalidDoctorId() {
        ConsultationRequestDTO request = ConsultationRequestDTO.builder()
                .userId("102")
                .username("Chanchal")
                .doctorId(0L)
                .disease("Fever")
                .urgency(Urgency.LOW)
                .consultationType(ConsultationType.AUDIO)
                .build();

        assertThrows(IllegalArgumentException.class, () -> consultationService.createConsultation(request));
    }

    // 7. Doctor accepts REQUESTED consultation
    @Test
    @DisplayName("7. Doctor accepts REQUESTED consultation, updates doctor to BUSY and creates communication session")
    void doctorAccepts_RequestedConsultation() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(DoctorAvailabilityUpdateDTO.class), anyString()))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.createSession(any(CreateCommunicationSessionRequestDTO.class)))
                .thenReturn(CommunicationSessionResponseDTO.builder()
                        .id(888L)
                        .consultationId(5001L)
                        .userId("102")
                        .doctorId(52L)
                        .roomId("consultation-room-999")
                        .meetingUrl("https://meet.jit.si/consultation-room-999")
                        .status("CREATED")
                        .build());
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponseDTO response = consultationService.acceptConsultation(5001L);

        assertEquals(ConsultationStatus.ACCEPTED, response.getStatus());
        assertNotNull(response.getAcceptedAt());
        assertEquals("consultation-room-999", response.getRoomId());
        assertEquals("https://meet.jit.si/consultation-room-999", response.getMeetingUrl());
        verify(doctorServiceClient).updateDoctorAvailability(eq(52L), argThat(dto -> "BUSY".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));
        verify(communicationServiceClient).createSession(argThat(dto ->
                dto.getConsultationId().equals(5001L)
                        && dto.getUserId().equals("102")
                        && dto.getDoctorId().equals(52L)
                        && "VIDEO".equals(dto.getCommunicationType())
        ));
    }

    // 7b. Doctor accepts REQUESTED consultation, Communication Service fails -> rolls back doctor availability to AVAILABLE
    @Test
    @DisplayName("7b. Communication Service failure during accept rolls back doctor availability to AVAILABLE and throws CommunicationServiceException")
    void doctorAccepts_CommunicationServiceFailure_RollsBackDoctorAvailability() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(DoctorAvailabilityUpdateDTO.class), anyString()))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.createSession(any(CreateCommunicationSessionRequestDTO.class)))
                .thenThrow(new RuntimeException("Communication Service connection timed out"));

        CommunicationServiceException ex = assertThrows(CommunicationServiceException.class,
                () -> consultationService.acceptConsultation(5001L));

        assertTrue(ex.getMessage().contains("Communication Service failed to create session"));
        // Verify doctor was first updated to BUSY
        verify(doctorServiceClient).updateDoctorAvailability(eq(52L), argThat(dto -> "BUSY".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));
        // Verify compensating rollback: doctor updated back to AVAILABLE
        verify(doctorServiceClient).updateDoctorAvailability(eq(52L), argThat(dto -> "AVAILABLE".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));
        // Verify consultation repository was never saved with ACCEPTED status
        verify(consultationRepository, never()).save(any());
        assertEquals(ConsultationStatus.REQUESTED, sampleConsultation.getStatus());
    }

    // 8. Doctor rejects REQUESTED consultation
    @Test
    @DisplayName("8. Doctor rejects REQUESTED consultation")
    void doctorRejects_RequestedConsultation() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponseDTO response = consultationService.rejectConsultation(5001L);

        assertEquals(ConsultationStatus.REJECTED, response.getStatus());
    }

    // 9. Doctor schedules REQUESTED consultation
    @Test
    @DisplayName("9. Doctor schedules REQUESTED consultation")
    void doctorSchedules_RequestedConsultation() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        ScheduleConsultationDTO scheduleDto = new ScheduleConsultationDTO(futureTime);

        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponseDTO response = consultationService.scheduleConsultation(5001L, scheduleDto);

        assertEquals(ConsultationStatus.SCHEDULED, response.getStatus());
        assertEquals(futureTime, response.getScheduledAt());
    }

    // 10. Cannot accept REJECTED consultation
    @Test
    @DisplayName("10. Cannot accept REJECTED consultation")
    void cannotAccept_RejectedConsultation() {
        sampleConsultation.setStatus(ConsultationStatus.REJECTED);
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));

        assertThrows(InvalidConsultationStateException.class, () -> consultationService.acceptConsultation(5001L));
    }

    // 11. Cannot accept CANCELLED consultation
    @Test
    @DisplayName("11. Cannot accept CANCELLED consultation")
    void cannotAccept_CancelledConsultation() {
        sampleConsultation.setStatus(ConsultationStatus.CANCELLED);
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));

        assertThrows(InvalidConsultationStateException.class, () -> consultationService.acceptConsultation(5001L));
    }

    // 12. Cannot schedule COMPLETED consultation
    @Test
    @DisplayName("12. Cannot schedule COMPLETED consultation")
    void cannotSchedule_CompletedConsultation() {
        sampleConsultation.setStatus(ConsultationStatus.COMPLETED);
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));

        ScheduleConsultationDTO scheduleDto = new ScheduleConsultationDTO(LocalDateTime.now().plusDays(1));
        assertThrows(InvalidConsultationStateException.class, () -> consultationService.scheduleConsultation(5001L, scheduleDto));
    }

    // 13. User cancels REQUESTED consultation
    @Test
    @DisplayName("13. User cancels REQUESTED consultation")
    void userCancels_RequestedConsultation() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelConsultationDTO cancelDto = new CancelConsultationDTO(CancelledBy.USER);
        ConsultationResponseDTO response = consultationService.cancelConsultation(5001L, cancelDto);

        assertEquals(ConsultationStatus.CANCELLED, response.getStatus());
        assertEquals(CancelledBy.USER, response.getCancelledBy());
        assertNotNull(response.getCancelledAt());
        // For REQUESTED, doctor was not marked BUSY so no release needed
        verify(doctorServiceClient, never()).updateDoctorAvailability(anyLong(), any(), anyString());
    }

    // 14. User cancels SCHEDULED consultation
    @Test
    @DisplayName("14. User cancels SCHEDULED consultation")
    void userCancels_ScheduledConsultation() {
        sampleConsultation.setStatus(ConsultationStatus.SCHEDULED);
        sampleConsultation.setScheduledAt(LocalDateTime.now().plusDays(2));
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelConsultationDTO cancelDto = new CancelConsultationDTO(CancelledBy.USER);
        ConsultationResponseDTO response = consultationService.cancelConsultation(5001L, cancelDto);

        assertEquals(ConsultationStatus.CANCELLED, response.getStatus());
    }

    // 15. Cannot cancel COMPLETED consultation
    @Test
    @DisplayName("15. Cannot cancel COMPLETED consultation")
    void cannotCancel_CompletedConsultation() {
        sampleConsultation.setStatus(ConsultationStatus.COMPLETED);
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));

        CancelConsultationDTO cancelDto = new CancelConsultationDTO(CancelledBy.USER);
        assertThrows(InvalidConsultationStateException.class, () -> consultationService.cancelConsultation(5001L, cancelDto));
    }

    // 16. Cannot schedule in the past
    @Test
    @DisplayName("16. Cannot schedule in the past")
    void cannotSchedule_InThePast() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));

        ScheduleConsultationDTO pastSchedule = new ScheduleConsultationDTO(LocalDateTime.now().minusHours(1));
        assertThrows(InvalidScheduleTimeException.class, () -> consultationService.scheduleConsultation(5001L, pastSchedule));
    }

    // 18. READY -> IN_CALL
    @Test
    @DisplayName("18. Start consultation: READY -> IN_CALL and activates communication session")
    void startConsultation_ReadyToInCall() {
        sampleConsultation.setStatus(ConsultationStatus.READY);
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(communicationServiceClient.getSessionByConsultationId(5001L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).build());
        when(communicationServiceClient.startSession(999L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).status("ACTIVE").build());
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponseDTO response = consultationService.startConsultation(5001L);

        assertEquals(ConsultationStatus.IN_CALL, response.getStatus());
        assertNotNull(response.getStartedAt());
        verify(communicationServiceClient).startSession(999L);
    }

    // 19 & 20. IN_CALL -> COMPLETED, Doctor becomes AVAILABLE after consultation completion and session ends
    @Test
    @DisplayName("19 & 20. Complete consultation: IN_CALL -> COMPLETED, doctor released to AVAILABLE, and communication session ended")
    void completeConsultation_InCallToCompleted() {
        sampleConsultation.setStatus(ConsultationStatus.IN_CALL);
        sampleConsultation.setStartedAt(LocalDateTime.now().minusMinutes(30));
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(DoctorAvailabilityUpdateDTO.class), anyString()))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.getSessionByConsultationId(5001L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).build());
        when(communicationServiceClient.endSession(999L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).status("ENDED").build());
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsultationResponseDTO response = consultationService.completeConsultation(5001L);

        assertEquals(ConsultationStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
        verify(doctorServiceClient).updateDoctorAvailability(eq(52L), argThat(dto -> "AVAILABLE".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));
        verify(communicationServiceClient).endSession(999L);
    }

    // Cancellation of ACCEPTED consultation releases doctor back to AVAILABLE and cancels communication session
    @Test
    @DisplayName("Cancelling an ACCEPTED consultation coordinates doctor availability back to AVAILABLE and cancels communication session")
    void cancelAcceptedConsultation_ReleasesDoctorToAvailable() {
        sampleConsultation.setStatus(ConsultationStatus.ACCEPTED);
        sampleConsultation.setAcceptedAt(LocalDateTime.now().minusMinutes(10));
        sampleConsultation.setRoomId("consultation-room-999");
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(DoctorAvailabilityUpdateDTO.class), anyString()))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.getSessionByConsultationId(5001L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).build());
        when(communicationServiceClient.cancelSession(999L))
                .thenReturn(CommunicationSessionResponseDTO.builder().id(999L).status("CANCELLED").build());
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelConsultationDTO cancelDto = new CancelConsultationDTO(CancelledBy.USER);
        ConsultationResponseDTO response = consultationService.cancelConsultation(5001L, cancelDto);

        assertEquals(ConsultationStatus.CANCELLED, response.getStatus());
        verify(doctorServiceClient).updateDoctorAvailability(eq(52L), argThat(dto -> "AVAILABLE".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));
        verify(communicationServiceClient).cancelSession(999L);
    }

    // Doctor Service failure during accept
    @Test
    @DisplayName("Doctor Service failure during accept throws DoctorUnavailableException and does not change state")
    void doctorServiceFailure_DuringAccept() {
        when(consultationRepository.findById(5001L)).thenReturn(Optional.of(sampleConsultation));
        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(DoctorAvailabilityUpdateDTO.class), anyString()))
                .thenThrow(new RuntimeException("Doctor Service down"));

        assertThrows(DoctorUnavailableException.class, () -> consultationService.acceptConsultation(5001L));
        assertEquals(ConsultationStatus.REQUESTED, sampleConsultation.getStatus());
        verify(consultationRepository, never()).save(any());
    }
}
