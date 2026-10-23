package com.ruralhealthcare.patient.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.patient.api.dto.CreatePatientRequest;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @Test
    @DisplayName("POST /api/v1/patients should return 201 Created on valid registration")
    void testRegisterPatientSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        CreatePatientRequest request = CreatePatientRequest.builder()
                .fullName("Asha Devi")
                .gender(Gender.FEMALE)
                .age(35)
                .contactNumber("+91-9876543210")
                .villageTown("Rampur")
                .subDistrict("Mohanlalganj")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .build();

        PatientDto dto = PatientDto.builder()
                .id(id)
                .fullName("Asha Devi")
                .gender(Gender.FEMALE)
                .contactNumber("+91-9876543210")
                .villageTown("Rampur")
                .district("Lucknow")
                .build();

        when(patientService.registerPatient(any(CreatePatientRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Asha Devi"))
                .andExpect(jsonPath("$.data.contactNumber").value("+91-9876543210"));
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} should return 200 OK when patient exists")
    void testGetPatientByIdSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        PatientDto dto = PatientDto.builder()
                .id(id)
                .fullName("Asha Devi")
                .gender(Gender.FEMALE)
                .contactNumber("+91-9876543210")
                .district("Lucknow")
                .build();

        when(patientService.getPatientById(id)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/patients/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Asha Devi"));
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} should return 404 Not Found when patient missing")
    void testGetPatientByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientService.getPatientById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/patients/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/patients should return 400 Bad Request on blank full name")
    void testRegisterPatientInvalidPayload() throws Exception {
        CreatePatientRequest invalid = CreatePatientRequest.builder()
                .fullName("")
                .gender(Gender.FEMALE)
                .contactNumber("123") // Invalid format
                .build();

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }
}
