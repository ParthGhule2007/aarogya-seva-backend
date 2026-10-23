package com.ruralhealthcare.sos.repository;

import com.ruralhealthcare.sos.domain.SosEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosEventRepository extends JpaRepository<SosEvent, UUID> {

    Optional<SosEvent> findByIdempotencyKey(String idempotencyKey);

    Optional<SosEvent> findTopByPatientIdOrderByTriggeredAtDesc(UUID patientId);

    List<SosEvent> findByPatientIdOrderByTriggeredAtDesc(UUID patientId);

    List<SosEvent> findByFacilityIdOrderByTriggeredAtDesc(UUID facilityId);

    List<SosEvent> findTop50ByOrderByTriggeredAtDesc();

    List<SosEvent> findByPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(UUID patientId, Instant after);
}
