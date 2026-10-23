package com.example.consultationservice.controller;

import com.example.consultationservice.client.CommunicationServiceClient;
import com.example.consultationservice.client.DoctorServiceClient;
import com.example.consultationservice.dto.CommunicationSessionResponseDTO;
import com.example.consultationservice.dto.DoctorResponseDTO;
import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.*;
import com.example.consultationservice.repository.ConsultationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConsultationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorServiceClient doctorServiceClient;

    @MockBean
    private CommunicationServiceClient communicationServiceClient;

    @BeforeEach
    void setUp() {
        consultationRepository.deleteAll();
    }

    // 1. Create consultation successfully
    @Test
    @DisplayName("1. Create consultation returns 201 Created")
    void testCreateConsultation_Success() throws Exception {
        String requestJson = """
                {
                  "userId": "102",
                  "username": "Chanchal",
                  "doctorId": 52,
                  "disease": "Severe stomach pain",
                  "urgency": "HIGH",
                  "consultationType": "VIDEO"
                }
                """;

        when(doctorServiceClient.getDoctorById(52L))
                .thenReturn(DoctorResponseDTO.builder().id(52L).name("Dr. Smith").build());

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultationId").exists())
                .andExpect(jsonPath("$.userId").value("102"))
                .andExpect(jsonPath("$.username").value("Chanchal"))
                .andExpect(jsonPath("$.doctorId").value(52))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.urgency").value("HIGH"))
                .andExpect(jsonPath("$.consultationType").value("VIDEO"))
                .andExpect(jsonPath("$.requestedAt").exists());
    }

    // 2. Invalid user ID
    @Test
    @DisplayName("2. Missing/Invalid user ID returns 400 Bad Request")
    void testCreateConsultation_MissingUserId() throws Exception {
        String requestJson = """
                {
                  "username": "Chanchal",
                  "doctorId": 52,
                  "disease": "Severe stomach pain",
                  "urgency": "HIGH",
                  "consultationType": "VIDEO"
                }
                """;

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 3. Invalid doctor ID
    @Test
    @DisplayName("3. Missing/Invalid doctor ID returns 400 Bad Request")
    void testCreateConsultation_MissingDoctorId() throws Exception {
        String requestJson = """
                {
                  "userId": "102",
                  "username": "Chanchal",
                  "disease": "Severe stomach pain",
                  "urgency": "HIGH",
                  "consultationType": "VIDEO"
                }
                """;

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 4. Missing disease
    @Test
    @DisplayName("4. Missing disease returns 400 Bad Request")
    void testCreateConsultation_MissingDisease() throws Exception {
        String requestJson = """
                {
                  "userId": "102",
                  "username": "Chanchal",
                  "doctorId": 52,
                  "urgency": "HIGH",
                  "consultationType": "VIDEO"
                }
                """;

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 5. Missing urgency
    @Test
    @DisplayName("5. Missing urgency returns 400 Bad Request")
    void testCreateConsultation_MissingUrgency() throws Exception {
        String requestJson = """
                {
                  "userId": "102",
                  "username": "Chanchal",
                  "doctorId": 52,
                  "disease": "Fever",
                  "consultationType": "VIDEO"
                }
                """;

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 6. Missing consultation type
    @Test
    @DisplayName("6. Missing consultation type returns 400 Bad Request")
    void testCreateConsultation_MissingConsultationType() throws Exception {
        String requestJson = """
                {
                  "userId": "102",
                  "username": "Chanchal",
                  "doctorId": 52,
                  "disease": "Fever",
                  "urgency": "LOW"
                }
                """;

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 7. Doctor accepts REQUESTED consultation
    @Test
    @DisplayName("7. Doctor accepts REQUESTED consultation, coordinates BUSY and creates communication session")
    void testDoctorAccepts_RequestedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(), eq("Consultation-Service")))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.createSession(any())).thenReturn(
                CommunicationSessionResponseDTO.builder()
                        .id(101L)
                        .consultationId(consultation.getId())
                        .userId("102")
                        .doctorId(52L)
                        .roomId("consultation-room-101")
                        .meetingUrl("https://meet.jit.si/consultation-room-101")
                        .status("CREATED")
                        .build()
        );

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.roomId").value("consultation-room-101"))
                .andExpect(jsonPath("$.meetingUrl").value("https://meet.jit.si/consultation-room-101"))
                .andExpect(jsonPath("$.acceptedAt").exists());
    }

    // 7b. Doctor accepts REQUESTED consultation, Communication Service fails -> rolls back doctor availability to AVAILABLE and returns 502
    @Test
    @DisplayName("7b. Accept consultation rolls back doctor to AVAILABLE and returns 502 BAD_GATEWAY if Communication Service fails")
    void testDoctorAccepts_CommunicationServiceFailure_RollsBackDoctorAndReturns502() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(), eq("Consultation-Service")))
                .thenReturn(ResponseEntity.ok().build());
        when(communicationServiceClient.createSession(any()))
                .thenThrow(new RuntimeException("Communication Service connection refused"));

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/accept"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("COMMUNICATION_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Communication Service failed to create session")));

        // Verify doctor availability was rolled back to AVAILABLE
        org.mockito.Mockito.verify(doctorServiceClient)
                .updateDoctorAvailability(eq(52L), org.mockito.ArgumentMatchers.argThat(dto -> "AVAILABLE".equals(dto.getAvailabilityStatus())), eq("Consultation-Service"));

        // Verify consultation in DB remains in REQUESTED status
        Consultation reloaded = consultationRepository.findById(consultation.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ConsultationStatus.REQUESTED, reloaded.getStatus());
    }

    // 8. Doctor rejects REQUESTED consultation
    @Test
    @DisplayName("8. Doctor rejects REQUESTED consultation")
    void testDoctorRejects_RequestedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // 9. Doctor schedules REQUESTED consultation
    @Test
    @DisplayName("9. Doctor schedules REQUESTED consultation with future date")
    void testDoctorSchedules_RequestedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
        String scheduleJson = String.format("{\"scheduledAt\": \"%s\"}", futureDate.toString());

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.scheduledAt").exists());
    }

    // 10. Cannot accept REJECTED consultation
    @Test
    @DisplayName("10. Cannot accept REJECTED consultation returns 400")
    void testCannotAccept_RejectedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REJECTED);

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/accept"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONSULTATION_STATE"));
    }

    // 11. Cannot accept CANCELLED consultation
    @Test
    @DisplayName("11. Cannot accept CANCELLED consultation returns 400")
    void testCannotAccept_CancelledConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.CANCELLED);

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/accept"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONSULTATION_STATE"));
    }

    // 12. Cannot schedule COMPLETED consultation
    @Test
    @DisplayName("12. Cannot schedule COMPLETED consultation returns 400")
    void testCannotSchedule_CompletedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.COMPLETED);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        String scheduleJson = String.format("{\"scheduledAt\": \"%s\"}", futureDate.toString());

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONSULTATION_STATE"));
    }

    // 13. User cancels REQUESTED consultation
    @Test
    @DisplayName("13. User cancels REQUESTED consultation returns 200")
    void testUserCancels_RequestedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        String cancelJson = "{\"cancelledBy\": \"USER\"}";

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("USER"))
                .andExpect(jsonPath("$.cancelledAt").exists());
    }

    // 14. User cancels SCHEDULED consultation
    @Test
    @DisplayName("14. User cancels SCHEDULED consultation returns 200")
    void testUserCancels_ScheduledConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.SCHEDULED);
        consultation.setScheduledAt(LocalDateTime.now().plusDays(2));
        consultationRepository.save(consultation);

        String cancelJson = "{\"cancelledBy\": \"USER\"}";

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // 15. Cannot cancel COMPLETED consultation
    @Test
    @DisplayName("15. Cannot cancel COMPLETED consultation returns 400")
    void testCannotCancel_CompletedConsultation() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.COMPLETED);

        String cancelJson = "{\"cancelledBy\": \"USER\"}";

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONSULTATION_STATE"));
    }

    // 16. Cannot schedule in the past
    @Test
    @DisplayName("16. Cannot schedule in the past returns 400")
    void testCannotSchedule_InThePast() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        LocalDateTime pastDate = LocalDateTime.now().minusHours(2);
        String scheduleJson = String.format("{\"scheduledAt\": \"%s\"}", pastDate.toString());

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // 18. READY -> IN_CALL
    @Test
    @DisplayName("18. Start consultation: READY -> IN_CALL")
    void testStartConsultation_ReadyToInCall() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.READY);

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_CALL"))
                .andExpect(jsonPath("$.startedAt").exists());
    }

    // 19 & 20. IN_CALL -> COMPLETED, Doctor released to AVAILABLE
    @Test
    @DisplayName("19 & 20. Complete consultation: IN_CALL -> COMPLETED and doctor released to AVAILABLE")
    void testCompleteConsultation_InCallToCompleted() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.IN_CALL);
        consultation.setStartedAt(LocalDateTime.now().minusMinutes(20));
        consultationRepository.save(consultation);

        when(doctorServiceClient.updateDoctorAvailability(eq(52L), any(), eq("Consultation-Service")))
                .thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(put("/api/consultations/" + consultation.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    // Test GET /api/consultations/{id}
    @Test
    @DisplayName("Get consultation by ID")
    void testGetConsultationById() throws Exception {
        Consultation consultation = createAndSaveSample(ConsultationStatus.REQUESTED);

        mockMvc.perform(get("/api/consultations/" + consultation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value(consultation.getId()))
                .andExpect(jsonPath("$.userId").value("102"))
                .andExpect(jsonPath("$.doctorId").value(52));
    }

    // Test GET /api/consultations/user/{userId}
    @Test
    @DisplayName("Get user's consultations")
    void testGetUserConsultations() throws Exception {
        createAndSaveSample(ConsultationStatus.REQUESTED);

        mockMvc.perform(get("/api/consultations/user/102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value("102"));
    }

    // Test with CoreService Patient UUID as String
    @Test
    @DisplayName("Create and retrieve consultation with CoreService Patient UUID string")
    void testCreateAndGetConsultation_WithCoreServicePatientUuid() throws Exception {
        String patientUuid = "550e8400-e29b-41d4-a716-446655440000";
        String requestJson = String.format("""
                {
                  "userId": "%s",
                  "username": "Ramesh Kumar",
                  "doctorId": 52,
                  "disease": "Acute Migraine",
                  "urgency": "HIGH",
                  "consultationType": "VIDEO"
                }
                """, patientUuid);

        when(doctorServiceClient.getDoctorById(52L))
                .thenReturn(DoctorResponseDTO.builder().id(52L).name("Dr. Smith").build());

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(patientUuid))
                .andExpect(jsonPath("$.username").value("Ramesh Kumar"));

        mockMvc.perform(get("/api/consultations/user/" + patientUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(patientUuid))
                .andExpect(jsonPath("$[0].username").value("Ramesh Kumar"));
    }

    // Test GET /api/consultations/doctor/{doctorId}/pending
    @Test
    @DisplayName("Get doctor's pending requests")
    void testGetDoctorPendingRequests() throws Exception {
        createAndSaveSample(ConsultationStatus.REQUESTED);

        mockMvc.perform(get("/api/consultations/doctor/52/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].doctorId").value(52))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    // Test GET /api/consultations/available-doctors
    @Test
    @DisplayName("Get available doctors via Doctor Service")
    void testGetAvailableDoctors() throws Exception {
        when(doctorServiceClient.getAvailableDoctors()).thenReturn(List.of(
                DoctorResponseDTO.builder().id(52L).name("Dr. Smith").availabilityStatus("AVAILABLE").build()
        ));

        mockMvc.perform(get("/api/consultations/available-doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(52))
                .andExpect(jsonPath("$[0].name").value("Dr. Smith"));
    }

    private Consultation createAndSaveSample(ConsultationStatus status) {
        Consultation consultation = Consultation.builder()
                .userId("102")
                .username("Chanchal")
                .doctorId(52L)
                .doctorName("Dr. Smith")
                .disease("Severe stomach pain")
                .urgency(Urgency.HIGH)
                .consultationType(ConsultationType.VIDEO)
                .status(status)
                .requestedAt(LocalDateTime.now().minusMinutes(10))
                .build();
        return consultationRepository.save(consultation);
    }
}
