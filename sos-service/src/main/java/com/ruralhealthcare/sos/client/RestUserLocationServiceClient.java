package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.dto.NearbyHelperDto;
import com.ruralhealthcare.sos.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Production REST implementation of UserLocationServiceClient.
 * Queries the core platform or location service for opted-in emergency helpers within radius.
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "rest")
public class RestUserLocationServiceClient implements UserLocationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestUserLocationServiceClient.class);

    private final RestTemplate restTemplate;
    private final SosProperties sosProperties;

    public RestUserLocationServiceClient(RestTemplate restTemplate, SosProperties sosProperties) {
        this.restTemplate = restTemplate;
        this.sosProperties = sosProperties;
    }

    @Override
    public List<NearbyHelperDto> findNearbyEmergencyHelpers(Double latitude, Double longitude, Double radiusKm, UUID excludeUserId) {
        log.info("Querying location service for helpers within {} km of lat={}, lon={} (excluding={})",
                radiusKm, latitude, longitude, excludeUserId);

        String baseUrl = sosProperties.getClients().getLocationServiceUrl();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/api/v1/users/nearby-emergency-helpers")
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("radiusKm", radiusKm)
                    .queryParam("emergencyAlertOptIn", true);

            if (excludeUserId != null) {
                builder.queryParam("excludeUserId", excludeUserId.toString());
            }

            ResponseEntity<List<NearbyHelperDto>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<NearbyHelperDto>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            log.error("Failed to query nearby helpers from location service: {}", ex.getMessage());
            throw new ExternalServiceException("Failed to query nearby helpers from location service: " + ex.getMessage(), ex);
        }
    }
}
