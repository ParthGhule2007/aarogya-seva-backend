package com.ruralhealthcare.common.util;

import com.ruralhealthcare.common.domain.GeoLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
class HaversineDistanceCalculatorTest {

    @Test
    @DisplayName("Should calculate distance as 0.0 for identical coordinates")
    void testIdenticalCoordinates() {
        double dist = HaversineDistanceCalculator.calculateDistanceKm(26.8467, 80.9462, 26.8467, 80.9462);
        assertEquals(0.0, dist, 0.01);
    }

    @Test
    @DisplayName("Should correctly calculate distance between Lucknow and Kanpur (~75 km)")
    void testLucknowToKanpur() {
        // Lucknow: 26.8467, 80.9462
        // Kanpur: 26.4499, 80.3319
        double dist = HaversineDistanceCalculator.calculateDistanceKm(26.8467, 80.9462, 26.4499, 80.3319);
        assertTrue(dist > 70.0 && dist < 85.0, "Expected distance between 70 and 85 km, but was: " + dist);
    }

    @Test
    @DisplayName("Should calculate distance with GeoLocation value objects")
    void testGeoLocationObjects() {
        GeoLocation loc1 = GeoLocation.of(26.8467, 80.9462);
        GeoLocation loc2 = GeoLocation.of(26.6811, 80.9984);
        double dist = HaversineDistanceCalculator.calculateDistanceKm(loc1, loc2);
        assertTrue(dist > 15.0 && dist < 25.0, "Expected distance between 15 and 25 km, but was: " + dist);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when GeoLocation is null")
    void testNullGeoLocation() {
        assertThrows(IllegalArgumentException.class, () ->
                HaversineDistanceCalculator.calculateDistanceKm(null, GeoLocation.of(20.0, 80.0)));
    }
}
