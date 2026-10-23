package com.ruralhealthcare.facility.service;

import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public service interface for internal module-to-module queries.
 * Other modules (Screening, Referral, Surveillance) MUST interact with the
 * Facility domain exclusively through this interface.
 */
public interface FacilityQueryService {

    /**
     * Retrieves facility details by its unique identifier.
     *
     * @param facilityId the unique ID of the facility
     * @return Optional containing FacilityDto if found, otherwise empty
     */
    Optional<FacilityDto> getFacilityById(UUID facilityId);

    /**
     * Lists all registered healthcare facilities.
     *
     * @return list of all facilities
     */
    List<FacilityDto> getAllFacilities();

    /**
     * Discovers and ranks nearby healthcare facilities based on geodesic distance
     * and care service requirements.
     *
     * @param latitude user's current latitude (-90 to 90)
     * @param longitude user's current longitude (-180 to 180)
     * @param maxRadiusKm maximum search radius in kilometers
     * @param requiredServices required medical services (e.g. EMERGENCY, MATERNAL_HEALTH)
     * @param limit maximum number of recommendations to return
     * @return list of distance-ranked facility recommendations
     */
    List<FacilityRecommendationDto> findNearbyFacilities(
            double latitude,
            double longitude,
            double maxRadiusKm,
            Set<MedicalServiceCode> requiredServices,
            int limit
    );

    /**
     * Discovers and ranks nearby healthcare facilities based on geodesic distance,
     * required medical services, and allowed facility types.
     *
     * @param latitude user's current latitude (-90 to 90)
     * @param longitude user's current longitude (-180 to 180)
     * @param maxRadiusKm maximum search radius in kilometers
     * @param requiredServices required medical services (e.g. EMERGENCY, GENERAL_OPD)
     * @param facilityTypes allowed facility types (e.g. PHC, CHC)
     * @param limit maximum number of recommendations to return
     * @return list of distance-ranked facility recommendations matching criteria
     */
    default List<FacilityRecommendationDto> findNearbyFacilities(
            double latitude,
            double longitude,
            double maxRadiusKm,
            Set<MedicalServiceCode> requiredServices,
            Set<FacilityType> facilityTypes,
            int limit
    ) {
        List<FacilityRecommendationDto> results = findNearbyFacilities(latitude, longitude, maxRadiusKm, requiredServices, limit);
        if (facilityTypes == null || facilityTypes.isEmpty()) {
            return results;
        }
        return results.stream()
                .filter(f -> facilityTypes.contains(f.getFacilityType()))
                .toList();
    }

    /**
     * Verifies whether a facility exists and is currently operational.
     * Useful for referral validation before creating a referral record.
     *
     * @param facilityId the facility ID to verify
     * @return true if the facility exists and is operational
     */
    boolean isFacilityOperational(UUID facilityId);
}
