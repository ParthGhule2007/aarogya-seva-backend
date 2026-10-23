package com.ruralhealthcare.sos.service;

import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.dto.NearbyAlertDto;
import com.ruralhealthcare.sos.dto.SosResponse;
import com.ruralhealthcare.sos.dto.SosStatusResponse;

import java.util.List;
import java.util.UUID;

/**
 * Core service contract for managing SOS emergency alerts.
 */
public interface SosService {

    /**
     * Triggers an emergency SOS alert.
     * Enforces authentication, authorization, target validation, deduplication, and async dispatch.
     *
     * @param request        validated SOS trigger request
     * @param idempotencyKey optional client-supplied idempotency key
     * @return fast HTTP 202 response
     */
    SosResponse triggerSos(CreateSosRequest request, String idempotencyKey);

    /**
     * Retrieves the current processing and notification status of an SOS alert.
     *
     * @param sosId unique SOS event identifier
     * @return current status response
     */
    SosStatusResponse getSosStatus(UUID sosId);

    /**
     * Resolves an active SOS event.
     *
     * @param sosId unique SOS event identifier
     * @return updated status response
     */
    SosStatusResponse resolveSos(UUID sosId);

    List<SosStatusResponse> getRecentSosEvents();

    List<SosStatusResponse> getSosEventsByPatient(UUID patientId);

    List<SosStatusResponse> getSosEventsByFacility(UUID facilityId);

    List<NearbyAlertDto> getActiveNearbyAlertsForUser(UUID userId);
}
