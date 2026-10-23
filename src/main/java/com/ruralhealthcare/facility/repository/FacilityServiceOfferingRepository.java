package com.ruralhealthcare.facility.repository;

import com.ruralhealthcare.facility.domain.FacilityServiceOffering;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityServiceOfferingRepository extends JpaRepository<FacilityServiceOffering, UUID> {

    List<FacilityServiceOffering> findByFacilityId(UUID facilityId);

    List<FacilityServiceOffering> findByFacilityIdInAndIsAvailableTrue(Collection<UUID> facilityIds);

    List<FacilityServiceOffering> findByServiceCodeAndIsAvailableTrue(MedicalServiceCode serviceCode);
}
