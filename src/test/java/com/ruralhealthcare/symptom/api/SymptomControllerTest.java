package com.ruralhealthcare.symptom.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.symptom.api.dto.BatchCreateSymptomsRequest;
import com.ruralhealthcare.symptom.api.dto.CreateSymptomRequest;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.api.dto.UpdateSymptomRequest;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.service.SymptomService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SymptomController.class)
class SymptomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SymptomService symptomService;

    @Test
    @DisplayName("POST /api/v1/cases/{caseId}/symptoms should return 201 Created on valid symptom")
    void testAddSymptomSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID symptomId = UUID.randomUUID();

        CreateSymptomRequest request = CreateSymptomRequest.builder()
                .symptomName("High Fever")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(3)
                .description("Temperature above 102 F")
                .bodySite("Whole Body")
                .build();

        SymptomDto dto = SymptomDto.builder()
                .id(symptomId)
                .caseId(caseId)
                .symptomName("High Fever")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(3)
                .description("Temperature above 102 F")
                .bodySite("Whole Body")
                .createdAt(Instant.now())
                .build();

        when(symptomService.addSymptom(eq(caseId), any(CreateSymptomRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/cases/{caseId}/symptoms", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symptomName").value("High Fever"))
                .andExpect(jsonPath("$.data.severity").value("SEVERE"));
    }

    @Test
    @DisplayName("POST /api/v1/cases/{caseId}/symptoms/batch should return 201 Created on batch insert")
    void testAddSymptomsBatchSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();

        BatchCreateSymptomsRequest request = BatchCreateSymptomsRequest.builder()
                .symptoms(List.of(
                        CreateSymptomRequest.builder()
                                .symptomName("Nausea")
                                .severity(SymptomSeverity.MILD)
                                .durationDays(1)
                                .build()
                ))
                .build();

        SymptomDto dto = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Nausea")
                .severity(SymptomSeverity.MILD)
                .durationDays(1)
                .createdAt(Instant.now())
                .build();

        when(symptomService.addSymptomsBatch(eq(caseId), any(BatchCreateSymptomsRequest.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/cases/{caseId}/symptoms/batch", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].symptomName").value("Nausea"));
    }

    @Test
    @DisplayName("GET /api/v1/cases/{caseId}/symptoms should return 200 OK with symptom list")
    void testGetSymptomsByCaseId() throws Exception {
        UUID caseId = UUID.randomUUID();
        SymptomDto dto = SymptomDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName("Headache")
                .severity(SymptomSeverity.MILD)
                .durationDays(2)
                .createdAt(Instant.now())
                .build();

        when(symptomService.getSymptomsByCaseId(eq(caseId), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/cases/{caseId}/symptoms", caseId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].symptomName").value("Headache"));
    }

    @Test
    @DisplayName("GET /api/v1/symptoms/{id} should return 200 OK when found")
    void testGetSymptomByIdSuccess() throws Exception {
        UUID symptomId = UUID.randomUUID();
        SymptomDto dto = SymptomDto.builder()
                .id(symptomId)
                .symptomName("Chest Pain")
                .severity(SymptomSeverity.CRITICAL)
                .durationDays(1)
                .createdAt(Instant.now())
                .build();

        when(symptomService.getSymptomById(symptomId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/symptoms/{id}", symptomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("GET /api/v1/symptoms/{id} should return 404 Not Found when missing")
    void testGetSymptomByIdNotFound() throws Exception {
        UUID symptomId = UUID.randomUUID();
        when(symptomService.getSymptomById(symptomId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/symptoms/{id}", symptomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/symptoms/{id} should return 200 OK after update")
    void testUpdateSymptomSuccess() throws Exception {
        UUID symptomId = UUID.randomUUID();
        UpdateSymptomRequest request = UpdateSymptomRequest.builder()
                .severity(SymptomSeverity.SEVERE)
                .durationDays(5)
                .build();

        SymptomDto dto = SymptomDto.builder()
                .id(symptomId)
                .symptomName("Abdominal Cramps")
                .severity(SymptomSeverity.SEVERE)
                .durationDays(5)
                .createdAt(Instant.now())
                .build();

        when(symptomService.updateSymptom(eq(symptomId), any(UpdateSymptomRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/symptoms/{id}", symptomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.severity").value("SEVERE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/symptoms/{id} should return 200 OK")
    void testRemoveSymptomSuccess() throws Exception {
        UUID symptomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/symptoms/{id}", symptomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/cases/{caseId}/symptoms should return 400 Bad Request on blank symptom name")
    void testAddSymptomValidationFailure() throws Exception {
        UUID caseId = UUID.randomUUID();
        CreateSymptomRequest invalidRequest = CreateSymptomRequest.builder()
                .symptomName("") // Blank
                .severity(SymptomSeverity.MILD)
                .durationDays(1)
                .build();

        mockMvc.perform(post("/api/v1/cases/{caseId}/symptoms", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
