package com.ruralhealthcare.facility.service;

import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.facility.api.dto.CreateFacilityRequest;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityServiceOffering;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.HealthcareFacility;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.repository.FacilityDistanceProjection;
import com.ruralhealthcare.facility.repository.FacilityServiceOfferingRepository;
import com.ruralhealthcare.facility.repository.HealthcareFacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacilityServiceTest {

    @Mock
    private HealthcareFacilityRepository facilityRepository;

    @Mock
    private FacilityServiceOfferingRepository serviceOfferingRepository;

    @InjectMocks
    private FacilityServiceImpl facilityService;

    private UUID facilityId1;
    private UUID facilityId2;
    private FacilityDistanceProjection projection1;
    private FacilityDistanceProjection projection2;

    @BeforeEach
    void setUp() {
        facilityId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        facilityId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        projection1 = mock(FacilityDistanceProjection.class);
        when(projection1.getId()).thenReturn(facilityId1);
        when(projection1.getName()).thenReturn("Demo Rampur Primary Health Centre");
        when(projection1.getFacilityType()).thenReturn("PHC");
        when(projection1.getLatitude()).thenReturn(26.8467);
        when(projection1.getLongitude()).thenReturn(80.9462);
        when(projection1.getAddressLine1()).thenReturn("Main Village Road");
        when(projection1.getVillageTown()).thenReturn("Rampur");
        when(projection1.getDistrict()).thenReturn("Lucknow");
        when(projection1.getState()).thenReturn("Uttar Pradesh");
        when(projection1.getPincode()).thenReturn("226301");
        when(projection1.getContactNumber()).thenReturn("+91-522-298101");
        when(projection1.getIsOperational()).thenReturn(true);
        when(projection1.getIsDemoData()).thenReturn(true);
        when(projection1.getDistanceKm()).thenReturn(2.5);

        projection2 = mock(FacilityDistanceProjection.class);
        when(projection2.getId()).thenReturn(facilityId2);
        when(projection2.getName()).thenReturn("Demo Mohanlalganj Community Health Centre");
        when(projection2.getFacilityType()).thenReturn("CHC");
        when(projection2.getLatitude()).thenReturn(26.6811);
        when(projection2.getLongitude()).thenReturn(80.9984);
        when(projection2.getAddressLine1()).thenReturn("Hospital Road");
        when(projection2.getVillageTown()).thenReturn("Mohanlalganj");
        when(projection2.getDistrict()).thenReturn("Lucknow");
        when(projection2.getState()).thenReturn("Uttar Pradesh");
        when(projection2.getPincode()).thenReturn("226301");
        when(projection2.getContactNumber()).thenReturn("+91-522-298201");
        when(projection2.getIsOperational()).thenReturn(true);
        when(projection2.getIsDemoData()).thenReturn(true);
        when(projection2.getDistanceKm()).thenReturn(18.2);
    }

    @Test
    @DisplayName("Should delegate nearby facility search to PostGIS query and preserve database distance order")
    void testFindNearbyFacilitiesDelegatesToPostGis() {
        when(facilityRepository.searchNearbyFacilitiesWithPostGis(
                eq(26.85), eq(80.95), eq(30000.0), eq(false), any(), eq(10)
        )).thenReturn(List.of(projection1, projection2));

        when(serviceOfferingRepository.findByFacilityIdInAndIsAvailableTrue(List.of(facilityId1, facilityId2)))
                .thenReturn(Collections.emptyList());

        List<FacilityRecommendationDto> results = facilityService.findNearbyFacilities(
                26.85, 80.95, 30.0, null, 10
        );

        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify transparent distance ordering (PostGIS derived, no 60/40 scoring)
        assertEquals("Demo Rampur Primary Health Centre", results.get(0).getName());
        assertEquals(2.5, results.get(0).getDistanceKm());
        assertTrue(results.get(0).isDemoData());

        assertEquals("Demo Mohanlalganj Community Health Centre", results.get(1).getName());
        assertEquals(18.2, results.get(1).getDistanceKm());

        // Verify PostGIS query was invoked with meter conversion (30 km -> 30,000 m)
        verify(facilityRepository, times(1)).searchNearbyFacilitiesWithPostGis(
                26.85, 80.95, 30000.0, false, Set.of("NONE"), 10
        );
    }

    @Test
    @DisplayName("Should filter by service code in PostGIS database query")
    void testFindNearbyFacilitiesWithServiceFilter() {
        when(facilityRepository.searchNearbyFacilitiesWithPostGis(
                eq(26.85), eq(80.95), eq(50000.0), eq(true), eq(Set.of("EMERGENCY")), eq(10)
        )).thenReturn(List.of(projection2));

        HealthcareFacility facility = HealthcareFacility.builder()
                .id(facilityId2)
                .build();

        FacilityServiceOffering offering = FacilityServiceOffering.builder()
                .facility(facility)
                .serviceCode(MedicalServiceCode.EMERGENCY)
                .isAvailable(true)
                .build();

        when(serviceOfferingRepository.findByFacilityIdInAndIsAvailableTrue(List.of(facilityId2)))
                .thenReturn(List.of(offering));

        List<FacilityRecommendationDto> results = facilityService.findNearbyFacilities(
                26.85, 80.95, 50.0, Set.of(MedicalServiceCode.EMERGENCY), 10
        );

        assertEquals(1, results.size());
        assertEquals("Demo Mohanlalganj Community Health Centre", results.get(0).getName());
        assertTrue(results.get(0).getAvailableServices().contains(MedicalServiceCode.EMERGENCY));
    }

    @Test
    @DisplayName("Should reject invalid coordinates with ValidationException")
    void testInvalidCoordinates() {
        assertThrows(ValidationException.class, () ->
                facilityService.findNearbyFacilities(95.0, 80.0, 10.0, null, 5));
        assertThrows(ValidationException.class, () ->
                facilityService.findNearbyFacilities(-95.0, 80.0, 10.0, null, 5));
        assertThrows(ValidationException.class, () ->
                facilityService.findNearbyFacilities(20.0, 185.0, 10.0, null, 5));
    }

    @Test
    @DisplayName("Should reject invalid search radius with ValidationException")
    void testInvalidRadius() {
        assertThrows(ValidationException.class, () ->
                facilityService.findNearbyFacilities(26.85, 80.95, 0.5, null, 5));
        assertThrows(ValidationException.class, () ->
                facilityService.findNearbyFacilities(26.85, 80.95, 250.0, null, 5));
    }

    @Test
    @DisplayName("Should create production facility with isDemoData=false")
    void testCreateFacility() {
        CreateFacilityRequest request = CreateFacilityRequest.builder()
                .name("New Verified Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .latitude(26.85)
                .longitude(80.95)
                .addressLine1("Sector 1")
                .villageTown("Test Village")
                .subDistrict("Tehsil")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226001")
                .contactNumber("+91-9876543210")
                .isOperational(true)
                .initialServices(Set.of(MedicalServiceCode.GENERAL_OPD))
                .build();

        when(facilityRepository.save(any(HealthcareFacility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FacilityDto created = facilityService.createFacility(request);

        assertNotNull(created);
        assertEquals("New Verified Primary Health Centre", created.getName());
        assertFalse(created.isDemoData(), "Admin-created facilities must not be marked as demo data");
    }

    @Test
    @DisplayName("Should retrieve facility by ID")
    void testGetFacilityById() {
        HealthcareFacility facility = HealthcareFacility.builder()
                .id(facilityId1)
                .name("Demo Rampur Primary Health Centre")
                .facilityType(FacilityType.PHC)
                .addressLine1("Main Road")
                .villageTown("Rampur")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .contactNumber("+91-522-298101")
                .isOperational(true)
                .isDemoData(true)
                .build();

        when(facilityRepository.findByIdWithServices(facilityId1)).thenReturn(Optional.of(facility));

        Optional<FacilityDto> result = facilityService.getFacilityById(facilityId1);

        assertTrue(result.isPresent());
        assertEquals("Demo Rampur Primary Health Centre", result.get().getName());
        assertTrue(result.get().isDemoData());
    }
}
