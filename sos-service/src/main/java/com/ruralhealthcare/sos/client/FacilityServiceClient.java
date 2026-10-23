package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.dto.FacilityContactDto;
import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for validating facilities and resolving authorized notification recipients.
 */
public interface FacilityServiceClient {

    /**
     * Validates that the facility exists and is operational, returning emergency contact and authorized recipient IDs.
     *
     * @param facilityId ID of the facility selected by the patient
     * @return Facility contact and recipient IDs, or empty if facility not found/invalid
     */
    Optional<FacilityContactDto> getFacilityContact(UUID facilityId);
}
