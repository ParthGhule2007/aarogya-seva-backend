package com.ruralhealthcare.facility.api;

import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.facility.api.dto.CreateFacilityRequest;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.api.dto.UpdateFacilityRequest;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.service.FacilityAdminService;
import com.ruralhealthcare.facility.service.FacilityQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facilities")
@Validated
@Tag(name = "Healthcare Facilities", description = "Endpoints for healthcare facility discovery, nearby search, and administration")
public class FacilityController {

    private final FacilityQueryService queryService;
    private final FacilityAdminService adminService;

    public FacilityController(FacilityQueryService queryService, FacilityAdminService adminService) {
        this.queryService = queryService;
        this.adminService = adminService;
    }

    @Operation(summary = "List all healthcare facilities", description = "Retrieves all registered healthcare facilities")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getAllFacilities() {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getAllFacilities()));
    }

    @Operation(summary = "Search nearby healthcare facilities", description = "Finds and ranks facilities within a radius matching required medical services and facility types")
    @GetMapping({"/nearby", "/search"})
    public ResponseEntity<ApiResponse<List<FacilityRecommendationDto>>> searchNearbyFacilities(
            @Parameter(description = "User current latitude", example = "26.8500")
            @RequestParam @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,

            @Parameter(description = "User current longitude", example = "80.9500")
            @RequestParam @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,

            @Parameter(description = "Maximum search radius in kilometers", example = "30.0")
            @RequestParam(defaultValue = "30.0") @DecimalMin("1.0") @DecimalMax("200.0") Double radiusKm,

            @Parameter(description = "Filter by required medical services (optional)")
            @RequestParam(name = "services", required = false) Set<MedicalServiceCode> services,
            @RequestParam(name = "requiredServices", required = false) Set<MedicalServiceCode> requiredServices,

            @Parameter(description = "Filter by required facility types (optional)")
            @RequestParam(name = "facilityTypes", required = false) Set<FacilityType> facilityTypes,
            @RequestParam(name = "facilityType", required = false) FacilityType facilityType,

            @Parameter(description = "Max number of facilities to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit
    ) {
        Set<MedicalServiceCode> filterServices = services != null ? services : requiredServices;
        Set<FacilityType> filterTypes = facilityTypes != null ? new HashSet<>(facilityTypes) : new HashSet<>();
        if (facilityType != null) {
            filterTypes.add(facilityType);
        }

        List<FacilityRecommendationDto> recommendations;
        if (filterTypes.isEmpty()) {
            recommendations = queryService.findNearbyFacilities(
                    latitude, longitude, radiusKm, filterServices, limit
            );
        } else {
            recommendations = queryService.findNearbyFacilities(
                    latitude, longitude, radiusKm, filterServices, filterTypes, limit
            );
        }
        return ResponseEntity.ok(ApiResponse.ok(recommendations));
    }

    @Operation(summary = "Get facility details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> getFacilityById(@PathVariable UUID id) {
        return queryService.getFacilityById(id)
                .map(facility -> ResponseEntity.ok(ApiResponse.ok(facility)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<FacilityDto>builder()
                                .success(false)
                                .message("Facility not found with ID: " + id)
                                .build()));
    }

    @Operation(summary = "Register a new healthcare facility", description = "Administrative endpoint to onboard a rural facility with its available services")
    @PostMapping
    public ResponseEntity<ApiResponse<FacilityDto>> createFacility(@Valid @RequestBody CreateFacilityRequest request) {
        FacilityDto created = adminService.createFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Facility created successfully", created));
    }

    @Operation(summary = "Update facility information")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> updateFacility(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFacilityRequest request
    ) {
        FacilityDto updated = adminService.updateFacility(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Facility updated successfully", updated));
    }

    @Operation(summary = "Set operational status of a facility")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> setOperationalStatus(
            @PathVariable UUID id,
            @RequestParam(name = "operational", required = false) Boolean operational,
            @RequestParam(name = "isOperational", required = false) Boolean isOperational
    ) {
        boolean status = Boolean.TRUE.equals(operational) || Boolean.TRUE.equals(isOperational);
        adminService.setOperationalStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Operational status updated to: " + status, null));
    }

    @Operation(summary = "Delete a facility")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(@PathVariable UUID id) {
        adminService.deleteFacility(id);
        return ResponseEntity.ok(ApiResponse.ok("Facility deleted successfully", null));
    }
}
