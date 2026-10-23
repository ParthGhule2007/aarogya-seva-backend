package com.ruralhealthcare.common.util;

import com.ruralhealthcare.common.domain.GeoLocation;

/**
 * @deprecated All production geographic search and distance calculations must use PostgreSQL/PostGIS
 *             database-level operations (ST_DWithin with spatial GiST indexing and ST_Distance).
 *             This utility is preserved only for auxiliary client/offline calculations.
 */
@Deprecated
public final class HaversineDistanceCalculator {

    public static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineDistanceCalculator() {
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLatRad = Math.toRadians(lat1);
        double destLatRad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(originLatRad) * Math.cos(destLatRad);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;
        return Math.round(distance * 100.0) / 100.0;
    }

    public static double calculateDistanceKm(GeoLocation origin, GeoLocation destination) {
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("Origin and destination coordinates must not be null");
        }
        return calculateDistanceKm(
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude()
        );
    }
}
