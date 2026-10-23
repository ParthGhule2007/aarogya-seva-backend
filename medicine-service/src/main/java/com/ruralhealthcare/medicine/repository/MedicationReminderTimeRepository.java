package com.ruralhealthcare.medicine.repository;

import com.ruralhealthcare.medicine.domain.MedicationReminderTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationReminderTimeRepository extends JpaRepository<MedicationReminderTime, UUID> {

    List<MedicationReminderTime> findByMedicationId(UUID medicationId);

    void deleteByMedicationId(UUID medicationId);
}
