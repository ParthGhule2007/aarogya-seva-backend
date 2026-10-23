package com.ruralhealthcare.facility.service;

import com.ruralhealthcare.facility.api.dto.CreateFacilityRequest;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.UpdateFacilityRequest;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.util.Set;
import java.util.UUID;

public interface FacilityAdminService {

    FacilityDto createFacility(CreateFacilityRequest request);

    FacilityDto updateFacility(UUID facilityId, UpdateFacilityRequest request);

    FacilityDto updateServiceOfferings(UUID facilityId, Set<MedicalServiceCode> activeServices);

    void setOperationalStatus(UUID facilityId, boolean isOperational);

    void deleteFacility(UUID facilityId);
}
