package com.ruralhealthcare.patient.repository;

import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthcareCaseRepository extends JpaRepository<HealthcareCase, UUID> {

    @Query("SELECT c FROM HealthcareCase c LEFT JOIN FETCH c.symptoms LEFT JOIN FETCH c.patient WHERE c.id = :id")
    Optional<HealthcareCase> findByIdWithSymptoms(@Param("id") UUID id);

    Optional<HealthcareCase> findByCaseNumber(String caseNumber);

    List<HealthcareCase> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<HealthcareCase> findByStatusOrderByCreatedAtDesc(CaseStatus status);

    boolean existsByCaseNumber(String caseNumber);
}
