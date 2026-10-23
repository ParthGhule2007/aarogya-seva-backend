package com.ruralhealthcare.facility.repository;

import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("postgis-integration")
class HealthcareFacilityRepositoryPostGisTest {

    @Container
    static PostgreSQLContainer<?> postgisContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgisContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgisContainer::getUsername);
        registry.add("spring.datasource.password", postgisContainer::getPassword);
    }

    @Autowired
    private HealthcareFacilityRepository facilityRepository;

    @Test
    @DisplayName("Should verify PostGIS ST_DWithin and ST_Distance spatial calculations on PostgreSQL database")
    void testPostGisSpatialQuery() {
        // Query near Rampur coordinates (26.8467, 80.9462) with 30 km radius (30,000 meters)
        List<FacilityDistanceProjection> results = facilityRepository.searchNearbyFacilitiesWithPostGis(
                26.8467, 80.9462, 30000.0, false, Set.of("NONE"), 10
        );

        assertNotNull(results, "Query results must not be null");
        assertFalse(results.isEmpty(), "Expected seeded facilities within 30km radius");

        // First facility should be Rampur PHC (at origin point, distance ~0 km)
        FacilityDistanceProjection closest = results.get(0);
        assertTrue(closest.getName().contains("Rampur"), "Closest facility should be Rampur PHC");
        assertEquals(0.0, closest.getDistanceKm(), 0.05, "Distance to itself should be approximately 0 km");
        assertTrue(closest.getIsDemoData(), "Seeded facility must be marked as demo data");

        // Verify transparent distance ordering (strictly ascending)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getDistanceKm() <= results.get(i + 1).getDistanceKm(),
                    "Results must be strictly ordered by distance ascending without arbitrary weighting");
        }
    }

    @Test
    @DisplayName("Should filter facilities by medical capability at database level via PostGIS query")
    void testPostGisServiceFiltering() {
        // Query with EMERGENCY service filter
        List<FacilityDistanceProjection> emergencyFacilities = facilityRepository.searchNearbyFacilitiesWithPostGis(
                26.8467, 80.9462, 50000.0, true, Set.of(MedicalServiceCode.EMERGENCY.name()), 10
        );

        assertNotNull(emergencyFacilities);
        assertFalse(emergencyFacilities.isEmpty());

        // CHC Mohanlalganj has EMERGENCY; Rampur PHC does not
        boolean containsChc = emergencyFacilities.stream().anyMatch(f -> f.getName().contains("Mohanlalganj"));
        boolean containsPhc = emergencyFacilities.stream().anyMatch(f -> f.getName().contains("Rampur"));

        assertTrue(containsChc, "Mohanlalganj CHC provides EMERGENCY care and must be returned");
        assertFalse(containsPhc, "Rampur PHC does not provide EMERGENCY care and must be excluded");
    }
}
