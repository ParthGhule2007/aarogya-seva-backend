package com.ruralhealthcare.medicine.repository;

import com.ruralhealthcare.medicine.domain.Medication;
import com.ruralhealthcare.medicine.domain.MedicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByPatientIdAndStatus(UUID patientId, MedicationStatus status);

    List<Medication> findByPatientId(UUID patientId);

    List<Medication> findByStatus(MedicationStatus status);

    List<Medication> findByStatusAndStartDateLessThanEqual(MedicationStatus status, LocalDate date);
}
