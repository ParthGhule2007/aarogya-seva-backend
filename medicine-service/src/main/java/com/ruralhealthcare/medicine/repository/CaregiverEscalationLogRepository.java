package com.ruralhealthcare.medicine.repository;

import com.ruralhealthcare.medicine.domain.CaregiverEscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaregiverEscalationLogRepository extends JpaRepository<CaregiverEscalationLog, UUID> {

    boolean existsByStreakStartDoseId(UUID streakStartDoseId);

    boolean existsByMedicationIdAndStreakStartDoseId(UUID medicationId, UUID streakStartDoseId);

    List<CaregiverEscalationLog> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<CaregiverEscalationLog> findByMedicationIdOrderByCreatedAtDesc(UUID medicationId);
}
