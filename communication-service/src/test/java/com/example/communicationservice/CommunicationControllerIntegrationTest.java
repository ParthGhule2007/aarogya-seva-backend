package com.example.communicationservice;

import com.example.communicationservice.dto.CreateCommunicationSessionDTO;
import com.example.communicationservice.entity.CommunicationSession;
import com.example.communicationservice.enums.CommunicationStatus;
import com.example.communicationservice.enums.CommunicationType;
import com.example.communicationservice.repository.CommunicationSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommunicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunicationSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("1. Create VIDEO communication session successfully")
    void testCreateVideoSession() throws Exception {
        CreateCommunicationSessionDTO request = CreateCommunicationSessionDTO.builder()
                .consultationId(101L)
                .userId("201")
                .doctorId(301L)
                .communicationType(CommunicationType.VIDEO)
                .build();

        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.consultationId").value(101))
                .andExpect(jsonPath("$.userId").value("201"))
                .andExpect(jsonPath("$.doctorId").value(301))
                .andExpect(jsonPath("$.communicationType").value("VIDEO"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.roomId", startsWith("consultation-")))
                .andExpect(jsonPath("$.meetingUrl", startsWith("https://meet.jit.si/consultation-")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.startedAt").isEmpty())
                .andExpect(jsonPath("$.endedAt").isEmpty());
    }

    @Test
    @DisplayName("2. Create AUDIO communication session successfully")
    void testCreateAudioSession() throws Exception {
        CreateCommunicationSessionDTO request = CreateCommunicationSessionDTO.builder()
                .consultationId(102L)
                .userId("202")
                .doctorId(302L)
                .communicationType(CommunicationType.AUDIO)
                .build();

        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.consultationId").value(102))
                .andExpect(jsonPath("$.communicationType").value("AUDIO"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.meetingUrl", startsWith("https://meet.jit.si/consultation-")));
    }

    @Test
    @DisplayName("3. Create CHAT communication session successfully")
    void testCreateChatSession() throws Exception {
        CreateCommunicationSessionDTO request = CreateCommunicationSessionDTO.builder()
                .consultationId(103L)
                .userId("203")
                .doctorId(303L)
                .communicationType(CommunicationType.CHAT)
                .build();

        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.consultationId").value(103))
                .andExpect(jsonPath("$.communicationType").value("CHAT"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("4. Attempt to create duplicate session for same consultation returns 409 Conflict")
    void testDuplicateConsultationSession() throws Exception {
        CreateCommunicationSessionDTO request = CreateCommunicationSessionDTO.builder()
                .consultationId(104L)
                .userId("204")
                .doctorId(304L)
                .communicationType(CommunicationType.VIDEO)
                .build();

        // First creation succeeds
        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation for same consultationId must return 409 CONFLICT
        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("consultation ID: 104")));
    }

    @Test
    @DisplayName("5. Get session by ID successfully")
    void testGetSessionById() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(105L)
                .userId("205")
                .doctorId(305L)
                .roomId("consultation-105-uuid")
                .meetingUrl("https://meet.jit.si/consultation-105-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/communications/sessions/{id}", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.consultationId").value(105))
                .andExpect(jsonPath("$.roomId").value("consultation-105-uuid"))
                .andExpect(jsonPath("$.meetingUrl").value("https://meet.jit.si/consultation-105-uuid"));
    }

    @Test
    @DisplayName("6. Get session by consultation ID successfully")
    void testGetSessionByConsultationId() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(106L)
                .userId("206")
                .doctorId(306L)
                .roomId("consultation-106-uuid")
                .meetingUrl("https://meet.jit.si/consultation-106-uuid")
                .communicationType(CommunicationType.AUDIO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/communications/sessions/consultation/{consultationId}", 106L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.consultationId").value(106))
                .andExpect(jsonPath("$.communicationType").value("AUDIO"));
    }

    @Test
    @DisplayName("7. Start session successfully (CREATED -> ACTIVE)")
    void testStartSessionSuccess() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(107L)
                .userId("207")
                .doctorId(307L)
                .roomId("consultation-107-uuid")
                .meetingUrl("https://meet.jit.si/consultation-107-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/start", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    @Test
    @DisplayName("8. End session successfully (ACTIVE -> ENDED)")
    void testEndSessionSuccess() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(108L)
                .userId("208")
                .doctorId(308L)
                .roomId("consultation-108-uuid")
                .meetingUrl("https://meet.jit.si/consultation-108-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/end", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("9. Cancel CREATED session successfully (CREATED -> CANCELLED)")
    void testCancelCreatedSession() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(109L)
                .userId("209")
                .doctorId(309L)
                .roomId("consultation-109-uuid")
                .meetingUrl("https://meet.jit.si/consultation-109-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/cancel", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("10. Cancel ACTIVE session successfully (ACTIVE -> CANCELLED)")
    void testCancelActiveSession() throws Exception {
        CommunicationSession session = repository.save(CommunicationSession.builder()
                .consultationId(110L)
                .userId("210")
                .doctorId(310L)
                .roomId("consultation-110-uuid")
                .meetingUrl("https://meet.jit.si/consultation-110-uuid")
                .communicationType(CommunicationType.AUDIO)
                .status(CommunicationStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .startedAt(LocalDateTime.now().minusMinutes(2))
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/cancel", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("11. Invalid state transitions return 400 Bad Request")
    void testInvalidStateTransitions() throws Exception {
        // Case A: CREATED -> ENDED is invalid
        CommunicationSession createdSession = repository.save(CommunicationSession.builder()
                .consultationId(111L)
                .userId("211")
                .doctorId(311L)
                .roomId("consultation-111-uuid")
                .meetingUrl("https://meet.jit.si/consultation-111-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/end", createdSession.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Only ACTIVE sessions can be transitioned to ENDED")));

        // Case B: ENDED -> ACTIVE is invalid
        CommunicationSession endedSession = repository.save(CommunicationSession.builder()
                .consultationId(112L)
                .userId("212")
                .doctorId(312L)
                .roomId("consultation-112-uuid")
                .meetingUrl("https://meet.jit.si/consultation-112-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.ENDED)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .startedAt(LocalDateTime.now().minusMinutes(25))
                .endedAt(LocalDateTime.now().minusMinutes(5))
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/start", endedSession.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Only CREATED sessions can be transitioned to ACTIVE")));

        // Case C: ENDED -> CANCELLED is invalid
        mockMvc.perform(put("/api/communications/sessions/{id}/cancel", endedSession.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Only CREATED or ACTIVE sessions can be cancelled")));

        // Case D: CANCELLED -> ACTIVE is invalid
        CommunicationSession cancelledSession = repository.save(CommunicationSession.builder()
                .consultationId(113L)
                .userId("213")
                .doctorId(313L)
                .roomId("consultation-113-uuid")
                .meetingUrl("https://meet.jit.si/consultation-113-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CANCELLED)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .endedAt(LocalDateTime.now().minusMinutes(5))
                .build());

        mockMvc.perform(put("/api/communications/sessions/{id}/start", cancelledSession.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("12. Session not found returns 404 Not Found")
    void testSessionNotFound() throws Exception {
        mockMvc.perform(get("/api/communications/sessions/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Communication session not found with ID: 99999")));

        mockMvc.perform(get("/api/communications/sessions/consultation/{consultationId}", 88888L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("consultation ID: 88888")));
    }

    @Test
    @DisplayName("13. Validation failure returns 400 Bad Request")
    void testValidationFailure() throws Exception {
        // Missing consultationId, userId, doctorId, communicationType
        CreateCommunicationSessionDTO invalidRequest = new CreateCommunicationSessionDTO();

        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("14. Unique consultationId constraint verified in repository")
    void testUniqueConsultationIdConstraint() {
        CommunicationSession session1 = repository.save(CommunicationSession.builder()
                .consultationId(114L)
                .userId("214")
                .doctorId(314L)
                .roomId("consultation-114-uuid")
                .meetingUrl("https://meet.jit.si/consultation-114-uuid")
                .communicationType(CommunicationType.VIDEO)
                .status(CommunicationStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build());

        assertNotNull(session1.getId());
        assertTrue(repository.existsByConsultationId(114L));
    }

    @Test
    @DisplayName("15. Create session with CoreService Patient UUID string successfully")
    void testCreateSession_WithCoreServicePatientUuid() throws Exception {
        String patientUuid = "550e8400-e29b-41d4-a716-446655440000";
        CreateCommunicationSessionDTO request = CreateCommunicationSessionDTO.builder()
                .consultationId(115L)
                .userId(patientUuid)
                .doctorId(315L)
                .communicationType(CommunicationType.VIDEO)
                .build();

        mockMvc.perform(post("/api/communications/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(patientUuid))
                .andExpect(jsonPath("$.consultationId").value(115))
                .andExpect(jsonPath("$.doctorId").value(315));
    }
}
