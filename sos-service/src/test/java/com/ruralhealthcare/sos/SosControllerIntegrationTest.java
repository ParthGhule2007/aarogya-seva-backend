package com.ruralhealthcare.sos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.domain.UserType;
import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.dto.DeviceRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SosControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integration Test: Actuator Health Endpoint")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Integration Test: Device Registration and Deletion flow")
    void testDeviceRegistrationFlow() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "int-test-token-" + UUID.randomUUID();

        DeviceRegistrationRequest regReq = new DeviceRegistrationRequest();
        regReq.setUserId(userId);
        regReq.setUserType(UserType.PATIENT);
        regReq.setFcmToken(token);
        regReq.setEmergencyAlertOptIn(true);

        // 1. Register device
        mockMvc.perform(post("/api/v1/sos/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.emergencyAlertOptIn", is(true)))
                .andExpect(jsonPath("$.active", is(true)));

        // 2. Deactivate device
        mockMvc.perform(delete("/api/v1/sos/devices/" + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Integration Test: End-to-end SOS Triggering and Status Query")
    void testTriggerSosAndQueryStatus() throws Exception {
        UUID patientId = UUID.randomUUID();

        CreateSosRequest req = new CreateSosRequest();
        req.setPatientId(patientId);
        req.setLatitude(26.6811);
        req.setLongitude(80.9984);
        req.setTargets(List.of(TargetType.CAREGIVER));
        req.setMessage("Emergency: Chest pain and dizziness");

        // 1. Trigger SOS with authenticated patient headers
        MvcResult result = mockMvc.perform(post("/api/v1/sos")
                        .header("X-User-Id", patientId.toString())
                        .header("X-User-Role", "PATIENT")
                        .header("Idempotency-Key", "int-test-key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sosId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PROCESSING")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String sosIdStr = objectMapper.readTree(responseBody).get("sosId").asText();

        // Give a moment for background async execution to settle
        Thread.sleep(500);

        // 2. Query SOS status
        mockMvc.perform(get("/api/v1/sos/" + sosIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sosId", is(sosIdStr)))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    @DisplayName("Integration Test: Validation failure on missing patientId returns 400 Bad Request")
    void testValidationFailure() throws Exception {
        CreateSosRequest req = new CreateSosRequest();
        req.setLatitude(26.6811);
        req.setLongitude(80.9984);
        req.setTargets(List.of(TargetType.CAREGIVER));

        mockMvc.perform(post("/api/v1/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }
}
