package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.dto.FacilityContactDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mock adapter for FacilityServiceClient used in local development and testing.
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "mock", matchIfMissing = true)
public class MockFacilityServiceClient implements FacilityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MockFacilityServiceClient.class);

    private final Map<UUID, FacilityContactDto> mockFacilities = new ConcurrentHashMap<>();

    public MockFacilityServiceClient() {
        // Sample pre-populated facility
        UUID sampleFacilityId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID sampleStaffId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        mockFacilities.put(sampleFacilityId, new FacilityContactDto(
                sampleFacilityId, "Shirwal Primary Health Centre (PHC)",
                List.of(sampleStaffId), "+912169244000", true
        ));
    }

    public void registerMockFacility(FacilityContactDto facilityDto) {
        if (facilityDto != null && facilityDto.getFacilityId() != null) {
            mockFacilities.put(facilityDto.getFacilityId(), facilityDto);
        }
    }

    @Override
    public Optional<FacilityContactDto> getFacilityContact(UUID facilityId) {
        log.info("[MockFacilityServiceClient] Looking up facility [{}]", facilityId);
        FacilityContactDto facility = mockFacilities.get(facilityId);
        if (facility == null) {
            // Provide a realistic deterministic mock facility for testing any valid UUID
            UUID staffUserId = UUID.nameUUIDFromBytes(("facility-staff-" + facilityId).getBytes());
            facility = new FacilityContactDto(
                    facilityId, "Community Health Centre (Emergency Desk)",
                    List.of(staffUserId), "+912000000000", true
            );
        }
        return Optional.of(facility);
    }
}
