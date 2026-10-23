package com.ruralhealthcare.facility.service;

import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.domain.FacilityServiceOffering;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.HealthcareFacility;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.repository.FacilityDistanceProjection;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class FacilityMapper {

    private FacilityMapper() {
    }

    public static FacilityDto toDto(HealthcareFacility facility) {
        if (facility == null) {
            return null;
        }

        Set<MedicalServiceCode> availableServices = facility.getServiceOfferings() != null
                ? facility.getServiceOfferings().stream()
                .filter(FacilityServiceOffering::isAvailable)
                .map(FacilityServiceOffering::getServiceCode)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return FacilityDto.builder()
                .id(facility.getId())
                .name(facility.getName())
                .facilityType(facility.getFacilityType())
                .facilityTypeDisplayName(facility.getFacilityType() != null ? facility.getFacilityType().getDisplayName() : null)
                .latitude(facility.getGeoLocation() != null ? facility.getGeoLocation().getLatitude() : null)
                .longitude(facility.getGeoLocation() != null ? facility.getGeoLocation().getLongitude() : null)
                .addressLine1(facility.getAddressLine1())
                .addressLine2(facility.getAddressLine2())
                .villageTown(facility.getVillageTown())
                .subDistrict(facility.getSubDistrict())
                .district(facility.getDistrict())
                .state(facility.getState())
                .pincode(facility.getPincode())
                .contactNumber(facility.getContactNumber())
                .emergencyNumber(facility.getEmergencyNumber())
                .isOperational(facility.isOperational())
                .isDemoData(facility.isDemoData())
                .availableServices(availableServices)
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }

    public static FacilityRecommendationDto toRecommendationDto(
            FacilityDistanceProjection projection,
            Set<MedicalServiceCode> availableServices
    ) {
        if (projection == null) {
            return null;
        }

        FacilityType type = null;
        if (projection.getFacilityType() != null) {
            try {
                type = FacilityType.valueOf(projection.getFacilityType());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String addressSummary = String.format("%s, %s, %s, %s - %s",
                projection.getAddressLine1(),
                projection.getVillageTown(),
                projection.getDistrict(),
                projection.getState(),
                projection.getPincode());

        return FacilityRecommendationDto.builder()
                .facilityId(projection.getId())
                .name(projection.getName())
                .facilityType(type)
                .facilityTypeDisplayName(type != null ? type.getDisplayName() : projection.getFacilityType())
                .addressSummary(addressSummary)
                .villageTown(projection.getVillageTown())
                .district(projection.getDistrict())
                .state(projection.getState())
                .pincode(projection.getPincode())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .distanceKm(projection.getDistanceKm())
                .contactNumber(projection.getContactNumber())
                .emergencyNumber(projection.getEmergencyNumber())
                .isOperational(Boolean.TRUE.equals(projection.getIsOperational()))
                .isDemoData(Boolean.TRUE.equals(projection.getIsDemoData()))
                .availableServices(availableServices != null ? availableServices : Collections.emptySet())
                .build();
    }
}
