package com.ruralhealthcare.patient.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.RecordSymptomsRequest;
import com.ruralhealthcare.patient.api.dto.SymptomInputDto;
import com.ruralhealthcare.patient.api.dto.UpdateCaseStatusRequest;
import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.IntakeChannel;
import com.ruralhealthcare.patient.domain.SymptomSeverity;
import com.ruralhealthcare.patient.service.HealthcareCaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthcareCaseController.class)
class HealthcareCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HealthcareCaseService caseService;

    @Test
    @DisplayName("POST /api/v1/cases should return 201 Created on valid request")
    void testCreateCaseSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        CreateCaseRequest request = CreateCaseRequest.builder()
                .patientId(patientId)
                .intakeChannel(IntakeChannel.COMMUNITY_HEALTH_WORKER)
                .latitude(26.8467)
                .longitude(80.9462)
                .chiefComplaint("Abdominal pain")
                .notes("Patient reports severe abdominal pain")
                .build();

        HealthcareCaseDto dto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0001")
                .patientId(patientId)
                .status(CaseStatus.CREATED)
                .priority(CasePriority.MEDIUM)
                .intakeChannel(IntakeChannel.COMMUNITY_HEALTH_WORKER)
                .chiefComplaint("Abdominal pain")
                .createdAt(Instant.now())
                .build();

        when(caseService.createCase(any(CreateCaseRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseNumber").value("CASE-20260922-0001"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.chiefComplaint").value("Abdominal pain"));
    }

    @Test
    @DisplayName("GET /api/v1/cases/{id} should return 200 OK when case exists")
    void testGetCaseByIdSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HealthcareCaseDto dto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0002")
                .patientId(patientId)
                .status(CaseStatus.CREATED)
                .priority(CasePriority.HIGH)
                .intakeChannel(IntakeChannel.CHAT)
                .createdAt(Instant.now())
                .build();

        when(caseService.getCaseById(caseId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/cases/{id}", caseId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(caseId.toString()))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/cases/{id} should return 404 Not Found when case does not exist")
    void testGetCaseByIdNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(caseService.getCaseById(caseId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cases/{id}", caseId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/cases/{id}/case-symptoms should return 200 OK after recording symptoms")
    void testRecordSymptomsSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        RecordSymptomsRequest request = new RecordSymptomsRequest(List.of(
                SymptomInputDto.builder()
                        .symptomName("Fever")
                        .severity(SymptomSeverity.SEVERE)
                        .durationDays(3)
                        .description("High grade fever since 3 days")
                        .build()
        ));

        HealthcareCaseDto dto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0003")
                .patientId(patientId)
                .status(CaseStatus.SYMPTOMS_RECORDED)
                .priority(CasePriority.HIGH)
                .intakeChannel(IntakeChannel.FORM)
                .createdAt(Instant.now())
                .build();

        when(caseService.recordSymptoms(eq(caseId), any(RecordSymptomsRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/cases/{id}/case-symptoms", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SYMPTOMS_RECORDED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/cases/{id}/status should return 200 OK when transitioning status")
    void testUpdateCaseStatusSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(
                CaseStatus.PENDING_TRIAGE,
                CasePriority.CRITICAL,
                "Triaged by intake officer"
        );

        HealthcareCaseDto dto = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260922-0004")
                .patientId(patientId)
                .status(CaseStatus.PENDING_TRIAGE)
                .priority(CasePriority.CRITICAL)
                .intakeChannel(IntakeChannel.COMMUNITY_HEALTH_WORKER)
                .createdAt(Instant.now())
                .build();

        when(caseService.updateCaseStatus(eq(caseId), any(UpdateCaseStatusRequest.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_TRIAGE"))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }
}
