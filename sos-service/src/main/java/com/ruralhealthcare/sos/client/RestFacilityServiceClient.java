package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.dto.FacilityContactDto;
import com.ruralhealthcare.sos.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Production REST client implementation for FacilityServiceClient.
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "rest")
public class RestFacilityServiceClient implements FacilityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestFacilityServiceClient.class);

    private final RestClient restClient;

    public RestFacilityServiceClient(SosProperties properties, RestClient.Builder restClientBuilder) {
        String baseUrl = properties.getClients().getFacilityService().getBaseUrl();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Optional<FacilityContactDto> getFacilityContact(UUID facilityId) {
        log.info("[RestFacilityServiceClient] Validating facility [{}] with Facility Service", facilityId);
        try {
            FacilityContactDto response = restClient.get()
                    .uri("/api/v1/facilities/{facilityId}/emergency-contacts", facilityId)
                    .retrieve()
                    .body(FacilityContactDto.class);

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("[RestFacilityServiceClient] Facility lookup failed for [{}]: {}", facilityId, ex.getMessage());
            throw new ExternalServiceException("Failed to validate facility from Facility Service: " + ex.getMessage(), ex);
        }
    }
}
