package com.ruralhealthcare.facility.service;

import com.ruralhealthcare.common.domain.GeoLocation;
import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.facility.api.dto.CreateFacilityRequest;
import com.ruralhealthcare.facility.api.dto.FacilityDto;
import com.ruralhealthcare.facility.api.dto.FacilityRecommendationDto;
import com.ruralhealthcare.facility.api.dto.UpdateFacilityRequest;
import com.ruralhealthcare.facility.domain.FacilityServiceOffering;
import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.HealthcareFacility;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import com.ruralhealthcare.facility.repository.FacilityDistanceProjection;
import com.ruralhealthcare.facility.repository.FacilityServiceOfferingRepository;
import com.ruralhealthcare.facility.repository.HealthcareFacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FacilityServiceImpl implements FacilityQueryService, FacilityAdminService {

    private static final Logger log = LoggerFactory.getLogger(FacilityServiceImpl.class);

    private final HealthcareFacilityRepository facilityRepository;
    private final FacilityServiceOfferingRepository serviceOfferingRepository;

    public FacilityServiceImpl(HealthcareFacilityRepository facilityRepository,
                               FacilityServiceOfferingRepository serviceOfferingRepository) {
        this.facilityRepository = facilityRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacilityDto> getFacilityById(UUID facilityId) {
        if (facilityId == null) {
            return Optional.empty();
        }
        return facilityRepository.findByIdWithServices(facilityId)
                .map(FacilityMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityDto> getAllFacilities() {
        return facilityRepository.findAll().stream()
                .map(FacilityMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityRecommendationDto> findNearbyFacilities(
            double latitude,
            double longitude,
            double maxRadiusKm,
            Set<MedicalServiceCode> requiredServices,
            int limit
    ) {
        return findNearbyFacilities(latitude, longitude, maxRadiusKm, requiredServices, null, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityRecommendationDto> findNearbyFacilities(
            double latitude,
            double longitude,
            double maxRadiusKm,
            Set<MedicalServiceCode> requiredServices,
            Set<FacilityType> facilityTypes,
            int limit
    ) {
        validateCoordinates(latitude, longitude);

        if (maxRadiusKm < 1.0 || maxRadiusKm > 200.0) {
            throw new ValidationException("Search radius must be between 1.0 km and 200.0 km");
        }
        int effectiveLimit = Math.max(1, Math.min(limit, 50));
        double radiusMeters = maxRadiusKm * 1000.0;

        boolean filterByServices = requiredServices != null && !requiredServices.isEmpty();
        Set<String> serviceCodes = filterByServices
                ? requiredServices.stream().map(Enum::name).collect(Collectors.toSet())
                : Collections.singleton("NONE");

        boolean filterByFacilityTypes = facilityTypes != null && !facilityTypes.isEmpty();
        Set<String> typeStrings = filterByFacilityTypes
                ? facilityTypes.stream().map(Enum::name).collect(Collectors.toSet())
                : Collections.singleton("NONE");

        List<FacilityDistanceProjection> projections;
        if (filterByFacilityTypes) {
            projections = facilityRepository.searchNearbyFacilitiesWithCriteria(
                    latitude,
                    longitude,
                    radiusMeters,
                    filterByServices,
                    serviceCodes,
                    filterByFacilityTypes,
                    typeStrings,
                    effectiveLimit
            );
        } else {
            projections = facilityRepository.searchNearbyFacilitiesWithPostGis(
                    latitude,
                    longitude,
                    radiusMeters,
                    filterByServices,
                    serviceCodes,
                    effectiveLimit
            );
        }

        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> facilityIds = projections.stream().map(FacilityDistanceProjection::getId).toList();
        Map<UUID, Set<MedicalServiceCode>> servicesByFacility = serviceOfferingRepository
                .findByFacilityIdInAndIsAvailableTrue(facilityIds).stream()
                .collect(Collectors.groupingBy(
                        offering -> offering.getFacility().getId(),
                        Collectors.mapping(FacilityServiceOffering::getServiceCode, Collectors.toSet())
                ));

        return projections.stream()
                .map(projection -> {
                    Set<MedicalServiceCode> services = servicesByFacility.getOrDefault(projection.getId(), Collections.emptySet());
                    return FacilityMapper.toRecommendationDto(projection, services);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFacilityOperational(UUID facilityId) {
        if (facilityId == null) {
            return false;
        }
        return facilityRepository.findById(facilityId)
                .map(HealthcareFacility::isOperational)
                .orElse(false);
    }

    @Override
    @Transactional
    public FacilityDto createFacility(CreateFacilityRequest request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());

        HealthcareFacility facility = HealthcareFacility.builder()
                .id(UUID.randomUUID())
                .name(request.getName().trim())
                .facilityType(request.getFacilityType())
                .geoLocation(GeoLocation.of(request.getLatitude(), request.getLongitude()))
                .addressLine1(request.getAddressLine1().trim())
                .addressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null)
                .villageTown(request.getVillageTown().trim())
                .subDistrict(request.getSubDistrict().trim())
                .district(request.getDistrict().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode().trim())
                .contactNumber(request.getContactNumber().trim())
                .emergencyNumber(request.getEmergencyNumber() != null ? request.getEmergencyNumber().trim() : null)
                .isOperational(request.isOperational())
                .isDemoData(false)
                .build();

        if (request.getInitialServices() != null && !request.getInitialServices().isEmpty()) {
            for (MedicalServiceCode serviceCode : request.getInitialServices()) {
                FacilityServiceOffering offering = FacilityServiceOffering.builder()
                        .serviceCode(serviceCode)
                        .isAvailable(true)
                        .build();
                facility.addServiceOffering(offering);
            }
        }

        HealthcareFacility saved = facilityRepository.save(facility);
        log.info("Registered new healthcare facility [{}] ({}) in {}, {}", saved.getId(), saved.getName(), saved.getDistrict(), saved.getState());
        return FacilityMapper.toDto(saved);
    }

    @Override
    @Transactional
    public FacilityDto updateFacility(UUID facilityId, UpdateFacilityRequest request) {
        HealthcareFacility facility = facilityRepository.findByIdWithServices(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareFacility", facilityId));

        if (request.getName() != null && !request.getName().isBlank()) {
            facility.setName(request.getName().trim());
        }
        if (request.getFacilityType() != null) {
            facility.setFacilityType(request.getFacilityType());
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            validateCoordinates(request.getLatitude(), request.getLongitude());
            facility.setGeoLocation(GeoLocation.of(request.getLatitude(), request.getLongitude()));
        }
        if (request.getAddressLine1() != null) {
            facility.setAddressLine1(request.getAddressLine1().trim());
        }
        if (request.getAddressLine2() != null) {
            facility.setAddressLine2(request.getAddressLine2().trim());
        }
        if (request.getVillageTown() != null) {
            facility.setVillageTown(request.getVillageTown().trim());
        }
        if (request.getSubDistrict() != null) {
            facility.setSubDistrict(request.getSubDistrict().trim());
        }
        if (request.getDistrict() != null) {
            facility.setDistrict(request.getDistrict().trim());
        }
        if (request.getState() != null) {
            facility.setState(request.getState().trim());
        }
        if (request.getPincode() != null) {
            facility.setPincode(request.getPincode().trim());
        }
        if (request.getContactNumber() != null) {
            facility.setContactNumber(request.getContactNumber().trim());
        }
        if (request.getEmergencyNumber() != null) {
            facility.setEmergencyNumber(request.getEmergencyNumber().trim());
        }
        if (request.getIsOperational() != null) {
            facility.setOperational(request.getIsOperational());
        }
        if (request.getActiveServices() != null) {
            updateServicesInternal(facility, request.getActiveServices());
        }

        HealthcareFacility updated = facilityRepository.save(facility);
        log.info("Updated healthcare facility [{}] ({})", updated.getId(), updated.getName());
        return FacilityMapper.toDto(updated);
    }

    @Override
    @Transactional
    public FacilityDto updateServiceOfferings(UUID facilityId, Set<MedicalServiceCode> activeServices) {
        HealthcareFacility facility = facilityRepository.findByIdWithServices(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareFacility", facilityId));

        updateServicesInternal(facility, activeServices);
        HealthcareFacility updated = facilityRepository.save(facility);
        return FacilityMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void setOperationalStatus(UUID facilityId, boolean isOperational) {
        HealthcareFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareFacility", facilityId));
        facility.setOperational(isOperational);
        facilityRepository.save(facility);
        log.info("Set operational status for facility [{}] to {}", facilityId, isOperational);
    }

    @Override
    @Transactional
    public void deleteFacility(UUID facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("HealthcareFacility", facilityId);
        }
        facilityRepository.deleteById(facilityId);
        log.info("Deleted healthcare facility [{}]", facilityId);
    }

    private void updateServicesInternal(HealthcareFacility facility, Set<MedicalServiceCode> activeServices) {
        if (activeServices == null) {
            activeServices = new HashSet<>();
        }
        Set<MedicalServiceCode> requested = new HashSet<>(activeServices);

        facility.getServiceOfferings().forEach(offering -> {
            boolean shouldBeActive = requested.contains(offering.getServiceCode());
            offering.setAvailable(shouldBeActive);
            requested.remove(offering.getServiceCode());
        });

        for (MedicalServiceCode newCode : requested) {
            FacilityServiceOffering newOffering = FacilityServiceOffering.builder()
                    .serviceCode(newCode)
                    .isAvailable(true)
                    .build();
            facility.addServiceOffering(newOffering);
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0");
        }
    }
}
