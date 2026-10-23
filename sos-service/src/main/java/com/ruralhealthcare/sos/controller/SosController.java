package com.ruralhealthcare.sos.controller;

import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.dto.NearbyAlertDto;
import com.ruralhealthcare.sos.dto.SosResponse;
import com.ruralhealthcare.sos.dto.SosStatusResponse;
import com.ruralhealthcare.sos.service.SosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for triggering and tracking SOS emergency alerts.
 */
@RestController
@RequestMapping("/api/v1/sos")
@Tag(name = "SOS Emergency Alerts", description = "Endpoints for triggering emergency SOS and tracking status")
public class SosController {

    private final SosService sosService;

    public SosController(SosService sosService) {
        this.sosService = sosService;
    }

    @PostMapping
    @Operation(summary = "Trigger an Emergency SOS alert",
            description = "Triggers emergency SOS to any combination of CAREGIVER, FACILITY, and NEARBY_PEOPLE. Returns 202 Accepted immediately.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "SOS triggered and processing asynchronously"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller"),
            @ApiResponse(responseCode = "403", description = "Forbidden to trigger SOS on behalf of this patient")
    })
    public ResponseEntity<SosResponse> triggerSos(
            @Valid @RequestBody CreateSosRequest request,
            @Parameter(description = "Optional unique idempotency key to prevent accidental duplicate alerts")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        SosResponse response = sosService.triggerSos(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/active")
    @Operation(summary = "List recent/active SOS emergency alerts across the platform")
    public ResponseEntity<java.util.List<SosStatusResponse>> getActiveSosAlerts() {
        return ResponseEntity.ok(sosService.getRecentSosEvents());
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List SOS emergency alerts for a specific patient")
    public ResponseEntity<java.util.List<SosStatusResponse>> getSosByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(sosService.getSosEventsByPatient(patientId));
    }

    @GetMapping("/facility/{facilityId}")
    @Operation(summary = "List SOS emergency alerts targeting a specific healthcare facility")
    public ResponseEntity<java.util.List<SosStatusResponse>> getSosByFacility(@PathVariable UUID facilityId) {
        return ResponseEntity.ok(sosService.getSosEventsByFacility(facilityId));
    }

    @GetMapping({"/alerts/user/{userId}", "/nearby-alerts/{userId}"})
    @Operation(summary = "Get active nearby emergency alerts for a user")
    public ResponseEntity<java.util.List<NearbyAlertDto>> getNearbyAlertsForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(sosService.getActiveNearbyAlertsForUser(userId));
    }

    @GetMapping("/{sosId}")
    @Operation(summary = "Get SOS alert status",
            description = "Retrieves high-level notification delivery status without exposing sensitive patient details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current SOS status"),
            @ApiResponse(responseCode = "404", description = "SOS event not found")
    })
    public ResponseEntity<SosStatusResponse> getSosStatus(
            @Parameter(description = "Unique SOS event ID")
            @PathVariable UUID sosId) {

        SosStatusResponse response = sosService.getSosStatus(sosId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sosId}/resolve")
    @Operation(summary = "Mark an SOS alert as resolved",
            description = "Updates the emergency alert status to RESOLVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SOS marked as resolved"),
            @ApiResponse(responseCode = "404", description = "SOS event not found")
    })
    public ResponseEntity<SosStatusResponse> resolveSos(
            @Parameter(description = "Unique SOS event ID")
            @PathVariable UUID sosId) {

        SosStatusResponse response = sosService.resolveSos(sosId);
        return ResponseEntity.ok(response);
    }
}
