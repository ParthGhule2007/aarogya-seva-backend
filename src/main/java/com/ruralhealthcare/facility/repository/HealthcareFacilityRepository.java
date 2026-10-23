package com.ruralhealthcare.facility.repository;

import com.ruralhealthcare.facility.domain.HealthcareFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthcareFacilityRepository extends JpaRepository<HealthcareFacility, UUID> {

    @Query("SELECT f FROM HealthcareFacility f LEFT JOIN FETCH f.serviceOfferings WHERE f.id = :id")
    Optional<HealthcareFacility> findByIdWithServices(@Param("id") UUID id);

    List<HealthcareFacility> findByDistrictIgnoreCaseAndStateIgnoreCaseAndIsOperationalTrue(
            String district, String state
    );

    /**
     * Database-level PostGIS geographic search.
     * Uses PostGIS spatial GiST indexing via ST_DWithin on the location geography column,
     * and calculates spheroid distance in kilometers using ST_Distance / 1000.0.
     * Ordered transparently by distance ascending. No in-memory Haversine filtering.
     */
    @Query(value = """
            SELECT f.id AS id,
                   f.name AS name,
                   f.facility_type AS facilityType,
                   f.latitude AS latitude,
                   f.longitude AS longitude,
                   f.address_line1 AS addressLine1,
                   f.address_line2 AS addressLine2,
                   f.village_town AS villageTown,
                   f.sub_district AS subDistrict,
                   f.district AS district,
                   f.state AS state,
                   f.pincode AS pincode,
                   f.contact_number AS contactNumber,
                   f.emergency_number AS emergencyNumber,
                   f.is_operational AS isOperational,
                   f.is_demo_data AS isDemoData,
                   ROUND(CAST(ST_Distance(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 AS numeric), 2) AS distanceKm
            FROM healthcare_facilities f
            WHERE f.is_operational = true
              AND ST_DWithin(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
              AND (
                  :filterByServices = false OR
                  EXISTS (
                      SELECT 1 FROM facility_service_offerings s
                      WHERE s.facility_id = f.id
                        AND s.is_available = true
                        AND s.service_code IN (:serviceCodes)
                  )
              )
            ORDER BY distanceKm ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<FacilityDistanceProjection> searchNearbyFacilitiesWithPostGis(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("filterByServices") boolean filterByServices,
            @Param("serviceCodes") Collection<String> serviceCodes,
            @Param("limit") int limit
    );

    /**
     * Database-level PostGIS geographic search with both service offering and facility type filtering.
     * All criteria (operational, radius, service offerings, facility types) are applied using strict AND semantics.
     */
    @Query(value = """
            SELECT f.id AS id,
                   f.name AS name,
                   f.facility_type AS facilityType,
                   f.latitude AS latitude,
                   f.longitude AS longitude,
                   f.address_line1 AS addressLine1,
                   f.address_line2 AS addressLine2,
                   f.village_town AS villageTown,
                   f.sub_district AS subDistrict,
                   f.district AS district,
                   f.state AS state,
                   f.pincode AS pincode,
                   f.contact_number AS contactNumber,
                   f.emergency_number AS emergencyNumber,
                   f.is_operational AS isOperational,
                   f.is_demo_data AS isDemoData,
                   ROUND(CAST(ST_Distance(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 AS numeric), 2) AS distanceKm
            FROM healthcare_facilities f
            WHERE f.is_operational = true
              AND ST_DWithin(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
              AND (
                  :filterByFacilityTypes = false OR
                  f.facility_type IN (:facilityTypes)
              )
              AND (
                  :filterByServices = false OR
                  EXISTS (
                      SELECT 1 FROM facility_service_offerings s
                      WHERE s.facility_id = f.id
                        AND s.is_available = true
                        AND s.service_code IN (:serviceCodes)
                  )
              )
            ORDER BY distanceKm ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<FacilityDistanceProjection> searchNearbyFacilitiesWithCriteria(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("filterByServices") boolean filterByServices,
            @Param("serviceCodes") Collection<String> serviceCodes,
            @Param("filterByFacilityTypes") boolean filterByFacilityTypes,
            @Param("facilityTypes") Collection<String> facilityTypes,
            @Param("limit") int limit
    );
}
