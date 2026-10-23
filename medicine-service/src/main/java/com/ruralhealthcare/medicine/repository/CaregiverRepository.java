package com.ruralhealthcare.medicine.repository;

import com.ruralhealthcare.medicine.domain.Caregiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {

    List<Caregiver> findByPatientIdAndIsActiveTrue(UUID patientId);

    List<Caregiver> findByPatientId(UUID patientId);
}
