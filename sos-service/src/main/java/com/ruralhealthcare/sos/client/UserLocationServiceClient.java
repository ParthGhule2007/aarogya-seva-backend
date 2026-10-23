package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.dto.NearbyHelperDto;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for resolving registered nearby emergency helpers who have opted in.
 */
public interface UserLocationServiceClient {

    /**
     * Resolves registered users within the specified radius who have opted in to emergency alerts.
     *
     * @param latitude Current latitude of the triggering patient
     * @param longitude Current longitude of the triggering patient
     * @param radiusKm Radius in kilometers (enforced server-side)
     * @param excludeUserId The patient ID to exclude from receiving their own alert
     * @return List of nearby opted-in helpers
     */
    List<NearbyHelperDto> findNearbyEmergencyHelpers(Double latitude, Double longitude, Double radiusKm, UUID excludeUserId);
}
