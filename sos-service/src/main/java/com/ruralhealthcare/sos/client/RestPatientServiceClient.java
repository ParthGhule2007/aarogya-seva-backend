package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.config.SosProperties;
import com.ruralhealthcare.sos.dto.CaregiverDto;
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
 * Production REST client implementation for PatientServiceClient.
 */
@Component
@ConditionalOnProperty(name = "sos.clients.mode", havingValue = "rest")
public class RestPatientServiceClient implements PatientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RestPatientServiceClient.class);

    private final RestClient restClient;

    public RestPatientServiceClient(SosProperties properties, RestClient.Builder restClientBuilder) {
        String baseUrl = properties.getClients().getPatientService().getBaseUrl();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Optional<CaregiverDto> getCaregiverForPatient(UUID patientId) {
        log.info("[RestPatientServiceClient] Calling Patient Service for caregiver of patient [{}]", patientId);
        try {
            CaregiverDto response = restClient.get()
                    .uri("/api/v1/patients/{patientId}/caregiver", patientId)
                    .retrieve()
                    .body(CaregiverDto.class);

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("[RestPatientServiceClient] Failed to resolve caregiver for patient [{}]: {}", patientId, ex.getMessage());
            throw new ExternalServiceException("Failed to resolve caregiver from Patient Service: " + ex.getMessage(), ex);
        }
    }
}
