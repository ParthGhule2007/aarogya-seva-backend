package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.domain.DeviceToken;
import com.ruralhealthcare.sos.dto.NearbyHelperDto;
import com.ruralhealthcare.sos.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory / local device-token based adapter for UserLocationServiceClient.
 * Calculates geodesic distance using the Haversine formula server-side and enforces:
 * 1. Opt-in status (emergencyAlertOptIn = true)
 * 2. Active status (active = true)
 * 3. Distance <= radiusKm
 * 4. Exclusion of triggering patient
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "mock", matchIfMissing = true)
public class MockUserLocationServiceClient implements UserLocationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MockUserLocationServiceClient.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DeviceTokenRepository deviceTokenRepository;
    private final Map<UUID, NearbyHelperDto> manualMockHelpers = new ConcurrentHashMap<>();

    public MockUserLocationServiceClient(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public void addManualMockHelper(NearbyHelperDto helper) {
        manualMockHelpers.put(helper.getUserId(), helper);
    }

    public void clearManualMockHelpers() {
        manualMockHelpers.clear();
    }

    @Override
    public List<NearbyHelperDto> findNearbyEmergencyHelpers(Double latitude, Double longitude, Double radiusKm, UUID excludeUserId) {
        log.info("[MockUserLocationServiceClient] Searching opted-in emergency helpers near ({}, {}) within {} km, excluding patient [{}]",
                latitude, longitude, radiusKm, excludeUserId);

        Map<UUID, NearbyHelperDto> resultMap = new HashMap<>();

        // 1. Check registered device tokens in database
        List<DeviceToken> candidates = deviceTokenRepository.findOptedInEmergencyHelpers(excludeUserId);
        for (DeviceToken token : candidates) {
            if (token.getUserId().equals(excludeUserId)) {
                continue;
            }
            if (!token.isEmergencyAlertOptIn() || !token.isActive()) {
                continue;
            }

            // Enforce location freshness within last 10 minutes
            java.time.Instant tenMinThreshold = java.time.Instant.now().minus(java.time.Duration.ofMinutes(10));
            if (token.getUpdatedAt() != null && token.getUpdatedAt().isBefore(tenMinThreshold)) {
                log.info("Skipping helper [{}] because location is older than 10 minutes", token.getUserId());
                continue;
            }

            double distKm = 2.0; // default safe mock proximity if coordinates not tracked on device
            if (token.getLastLatitude() != null && token.getLastLongitude() != null) {
                distKm = calculateHaversineDistanceKm(latitude, longitude, token.getLastLatitude(), token.getLastLongitude());
            }

            if (distKm <= radiusKm) {
                resultMap.put(token.getUserId(), new NearbyHelperDto(token.getUserId(), token.getUserType(), distKm, true));
            }
        }

        // 2. Include any manually configured test helpers
        for (NearbyHelperDto helper : manualMockHelpers.values()) {
            if (!helper.getUserId().equals(excludeUserId) && helper.isEmergencyAlertOptIn()) {
                if (helper.getDistanceKm() == null || helper.getDistanceKm() <= radiusKm) {
                    resultMap.put(helper.getUserId(), helper);
                }
            }
        }

        List<NearbyHelperDto> result = new ArrayList<>(resultMap.values());
        result.sort(Comparator.comparingDouble(h -> h.getDistanceKm() != null ? h.getDistanceKm() : 0.0));
        log.info("[MockUserLocationServiceClient] Found {} eligible nearby emergency helper(s)", result.size());
        return result;
    }

    public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}
