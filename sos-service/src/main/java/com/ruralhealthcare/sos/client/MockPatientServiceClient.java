package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.dto.CaregiverDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mock adapter for PatientServiceClient used in local development and testing.
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "mock", matchIfMissing = true)
public class MockPatientServiceClient implements PatientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MockPatientServiceClient.class);

    private final Map<UUID, CaregiverDto> mockCaregivers = new ConcurrentHashMap<>();

    public MockPatientServiceClient() {
        // Pre-populate with deterministic test caregivers
        UUID samplePatientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sampleCaregiverId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        mockCaregivers.put(samplePatientId, new CaregiverDto(sampleCaregiverId, "Ramesh Patil (Caregiver)", "+919876543210", "Son"));
    }

    public void registerMockCaregiver(UUID patientId, CaregiverDto caregiverDto) {
        mockCaregivers.put(patientId, caregiverDto);
    }

    @Override
    public Optional<CaregiverDto> getCaregiverForPatient(UUID patientId) {
        log.info("[MockPatientServiceClient] Looking up caregiver for patient [{}]", patientId);
        CaregiverDto caregiver = mockCaregivers.get(patientId);
        if (caregiver == null) {
            // Generate a deterministic mock caregiver for dev testing if not explicitly registered
            UUID generatedCaregiverId = UUID.nameUUIDFromBytes(("caregiver-" + patientId).getBytes());
            caregiver = new CaregiverDto(generatedCaregiverId, "Designated Family Caregiver", "+919876500000", "Family");
        }
        return Optional.of(caregiver);
    }
}
