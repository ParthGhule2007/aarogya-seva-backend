package com.ruralhealthcare.referral.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.referral.api.dto.CreateReferralRequest;
import com.ruralhealthcare.referral.api.dto.ReferralDetailDto;
import com.ruralhealthcare.referral.api.dto.ReferralResponseDto;
import com.ruralhealthcare.referral.api.dto.UpdateReferralStatusRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import com.ruralhealthcare.referral.service.ReferralService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferralController.class)
class ReferralControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReferralService referralService;

    @Test
    @DisplayName("POST /api/v1/referrals should return 201 CREATED with referral summary")
    void testCreateReferralSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID referralId = UUID.randomUUID();

        CreateReferralRequest request = CreateReferralRequest.builder()
                .caseId(caseId)
                .facilityId(facilityId)
                .patientMessage("Need urgent evaluation for persistent chest pain")
                .build();

        ReferralResponseDto responseDto = ReferralResponseDto.builder()
                .id(referralId)
                .caseId(caseId)
                .facilityId(facilityId)
                .facilityName("Shirwal Primary Health Centre")
                .facilityType("PHC")
                .status(ReferralStatus.PENDING)
                .statusDisplayName("Pending Review")
                .patientMessage("Need urgent evaluation for persistent chest pain")
                .requestedAt(Instant.now())
                .build();

        when(referralService.createReferral(any(CreateReferralRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/referrals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(referralId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.facilityName").value("Shirwal Primary Health Centre"));
    }

    @Test
    @DisplayName("GET /api/v1/referrals/{id} should return 200 OK with comprehensive referral details")
    void testGetReferralByIdSuccess() throws Exception {
        UUID referralId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();

        ReferralResponseDto referral = ReferralResponseDto.builder()
                .id(referralId)
                .caseId(caseId)
                .patientId(patientId)
                .facilityId(facilityId)
                .facilityName("Shirwal Primary Health Centre")
                .status(ReferralStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        PatientDto patient = PatientDto.builder()
                .id(patientId)
                .fullName("Ramesh Jadhav")
                .age(52)
                .gender(Gender.MALE)
                .contactNumber("+919876543210")
                .villageTown("Shirwal")
                .build();

        HealthcareCaseDto healthcareCase = HealthcareCaseDto.builder()
                .id(caseId)
                .caseNumber("CASE-20260923-001")
                .patientId(patientId)
                .build();

        FacilityDto facility = FacilityDto.builder()
                .id(facilityId)
                .name("Shirwal Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .isOperational(true)
                .contactNumber("+912169222111")
                .build();

        ReferralDetailDto detail = ReferralDetailDto.builder()
                .referral(referral)
                .patient(patient)
                .healthcareCase(healthcareCase)
                .symptoms(Collections.emptyList())
                .facility(facility)
                .build();

        when(referralService.getReferralById(referralId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/referrals/{id}", referralId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referral.id").value(referralId.toString()))
                .andExpect(jsonPath("$.data.patient.fullName").value("Ramesh Jadhav"))
                .andExpect(jsonPath("$.data.healthcareCase.caseNumber").value("CASE-20260923-001"))
                .andExpect(jsonPath("$.data.facility.name").value("Shirwal Primary Health Centre"));
    }

    @Test
    @DisplayName("GET /api/v1/referrals/case/{caseId} should return 200 OK with list of referrals for the case")
    void testGetReferralsByCaseIdSuccess() throws Exception {
        UUID caseId = UUID.randomUUID();

        ReferralResponseDto ref = ReferralResponseDto.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .facilityName("Shirwal PHC")
                .status(ReferralStatus.PENDING)
                .build();

        when(referralService.getReferralsByCaseId(caseId)).thenReturn(List.of(ref));

        mockMvc.perform(get("/api/v1/referrals/case/{caseId}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].caseId").value(caseId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/referrals/patient/{patientId} should return 200 OK with list of patient referrals")
    void testGetReferralsByPatientIdSuccess() throws Exception {
        UUID patientId = UUID.randomUUID();

        ReferralResponseDto ref = ReferralResponseDto.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .facilityName("Shirwal PHC")
                .status(ReferralStatus.ACCEPTED)
                .build();

        when(referralService.getReferralsByPatientId(patientId)).thenReturn(List.of(ref));

        mockMvc.perform(get("/api/v1/referrals/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].patientId").value(patientId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/referrals/facility/{facilityId} should return 200 OK with list of facility referrals")
    void testGetReferralsByFacilityIdSuccess() throws Exception {
        UUID facilityId = UUID.randomUUID();

        ReferralResponseDto ref = ReferralResponseDto.builder()
                .id(UUID.randomUUID())
                .facilityId(facilityId)
                .facilityName("Shirwal PHC")
                .status(ReferralStatus.PENDING)
                .build();

        when(referralService.getReferralsByFacilityId(eq(facilityId), any())).thenReturn(List.of(ref));

        mockMvc.perform(get("/api/v1/referrals/facility/{facilityId}", facilityId)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].facilityId").value(facilityId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/referrals/{id}/status should return 200 OK with updated referral")
    void testUpdateReferralStatusSuccess() throws Exception {
        UUID referralId = UUID.randomUUID();

        UpdateReferralStatusRequest request = UpdateReferralStatusRequest.builder()
                .status(ReferralStatus.ACCEPTED)
                .facilityResponse("Patient can be evaluated in OPD today.")
                .build();

        ReferralResponseDto updatedDto = ReferralResponseDto.builder()
                .id(referralId)
                .status(ReferralStatus.ACCEPTED)
                .statusDisplayName("Accepted")
                .facilityResponse("Patient can be evaluated in OPD today.")
                .respondedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build();

        when(referralService.updateReferralStatus(eq(referralId), any(UpdateReferralStatusRequest.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/api/v1/referrals/{id}/status", referralId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.facilityResponse").value("Patient can be evaluated in OPD today."));
    }
}
