package com.ruralhealthcare.referral.repository;

import com.ruralhealthcare.referral.domain.ReferralRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<ReferralRequest, UUID> {

    List<ReferralRequest> findByCaseIdOrderByRequestedAtDesc(UUID caseId);

    List<ReferralRequest> findByPatientIdOrderByRequestedAtDesc(UUID patientId);

    List<ReferralRequest> findByFacilityIdOrderByRequestedAtDesc(UUID facilityId);

    List<ReferralRequest> findByFacilityIdAndStatusOrderByRequestedAtDesc(UUID facilityId, ReferralStatus status);

    boolean existsByCaseIdAndStatusIn(UUID caseId, Collection<ReferralStatus> statuses);

    Optional<ReferralRequest> findFirstByCaseIdAndStatusIn(UUID caseId, Collection<ReferralStatus> statuses);
}
