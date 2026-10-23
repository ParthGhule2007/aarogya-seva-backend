package com.ruralhealthcare.sos.controller;

import com.ruralhealthcare.sos.dto.DeviceRegistrationRequest;
import com.ruralhealthcare.sos.dto.DeviceRegistrationResponse;
import com.ruralhealthcare.sos.service.DeviceRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing device push tokens and emergency alert opt-in preferences.
 */
@RestController
@RequestMapping({"/api/v1/sos/devices", "/api/v1/devices"})
@Tag(name = "SOS Device Registration", description = "Endpoints for registering FCM push tokens and opting in/out of alerts")
public class DeviceRegistrationController {

    private final DeviceRegistrationService deviceRegistrationService;

    public DeviceRegistrationController(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    @PostMapping({"", "/register"})
    @Operation(summary = "Register FCM device token",
            description = "Registers or updates a device push token, user mapping, and emergency alert opt-in preference.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload")
    })
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {

        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/location")
    @Operation(summary = "Update user live location for emergency alerts",
            description = "Periodically updates user coordinates and emergency alert opt-in preference.")
    public ResponseEntity<DeviceRegistrationResponse> updateLocation(
            @RequestBody DeviceRegistrationRequest request) {
        if (request.getFcmToken() == null || request.getFcmToken().isBlank()) {
            request.setFcmToken("user-device-" + request.getUserId());
        }
        if (request.getUserType() == null) {
            request.setUserType(com.ruralhealthcare.sos.domain.UserType.PATIENT);
        }
        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Deactivate device token",
            description = "Deactivates an FCM device token so it no longer receives emergency notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device token deactivated"),
            @ApiResponse(responseCode = "404", description = "Token not found")
    })
    public ResponseEntity<Void> deactivateDevice(
            @Parameter(description = "FCM device registration token to deactivate")
            @PathVariable String token) {

        deviceRegistrationService.deactivateDeviceToken(token);
        return ResponseEntity.noContent().build();
    }
}
