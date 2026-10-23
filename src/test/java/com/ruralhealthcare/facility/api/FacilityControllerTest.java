package com.ruralhealthcare.facility.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.facility.api.dto.CreateFacilityRequest;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.service.FacilityAdminService;
import com.ruralhealthcare.facility.service.FacilityQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacilityController.class)
class FacilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FacilityQueryService queryService;

    @MockBean
    private FacilityAdminService adminService;

    @Test
    @DisplayName("GET /api/v1/facilities/search should return 200 with PostGIS distance and no suitability score")
    void testSearchNearbyFacilities() throws Exception {
        UUID id = UUID.randomUUID();
        FacilityRecommendationDto recommendation = FacilityRecommendationDto.builder()
                .facilityId(id)
                .name("Demo Rampur PHC")
                .facilityType(FacilityType.PHC)
                .facilityTypeDisplayName("Primary Health Centre")
                .distanceKm(4.2)
                .latitude(26.8467)
                .longitude(80.9462)
                .district("Lucknow")
                .contactNumber("+91-522-298101")
                .isOperational(true)
                .isDemoData(true)
                .availableServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        when(queryService.findNearbyFacilities(eq(26.85), eq(80.95), eq(30.0), any(), eq(10)))
                .thenReturn(List.of(recommendation));

        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("latitude", "26.85")
                        .param("longitude", "80.95")
                        .param("radiusKm", "30.0")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Demo Rampur PHC"))
                .andExpect(jsonPath("$.data[0].distanceKm").value(4.2))
                .andExpect(jsonPath("$.data[0].isDemoData").value(true))
                .andExpect(jsonPath("$.data[0].suitabilityScore").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/facilities/{id} should return 200 when facility exists")
    void testGetFacilityByIdSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        FacilityDto dto = FacilityDto.builder()
                .id(id)
                .name("Demo Mohanlalganj CHC")
                .facilityType(FacilityType.CHC)
                .latitude(26.6811)
                .longitude(80.9984)
                .district("Lucknow")
                .isOperational(true)
                .isDemoData(true)
                .build();

        when(queryService.getFacilityById(id)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/facilities/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Demo Mohanlalganj CHC"))
                .andExpect(jsonPath("$.data.isDemoData").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/facilities/{id} should return 404 when not found")
    void testGetFacilityByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.getFacilityById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/facilities/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/facilities with invalid payload should return 400 Bad Request with validation errors")
    void testCreateFacilityInvalidPayload() throws Exception {
        CreateFacilityRequest invalidRequest = CreateFacilityRequest.builder()
                .name("") // Empty name violates @NotBlank
                .latitude(95.0) // Invalid latitude > 90
                .build();

        mockMvc.perform(post("/api/v1/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/facilities/search with invalid latitude should return 400 Bad Request")
    void testSearchWithInvalidCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("latitude", "105.0") // Violates @DecimalMax("90.0")
                        .param("longitude", "80.95")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
