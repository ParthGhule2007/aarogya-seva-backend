package com.ruralhealthcare.medicine.repository;

import com.ruralhealthcare.medicine.domain.DoseEvent;
import com.ruralhealthcare.medicine.domain.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoseEventRepository extends JpaRepository<DoseEvent, UUID> {

    List<DoseEvent> findByMedicationIdAndScheduledDateAndScheduledTime(
            UUID medicationId, LocalDate scheduledDate, LocalTime scheduledTime);

    List<DoseEvent> findByPatientIdAndScheduledDateOrderByScheduledTimeAsc(
            UUID patientId, LocalDate scheduledDate);

    List<DoseEvent> findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(
            UUID patientId);

    List<DoseEvent> findByPatientIdOrderByScheduledDateAscScheduledTimeAsc(
            UUID patientId);

    List<DoseEvent> findByMedicationIdOrderByScheduledDateDescScheduledTimeDesc(
            UUID medicationId);

    List<DoseEvent> findByMedicationIdOrderByScheduledDateAscScheduledTimeAsc(
            UUID medicationId);

    List<DoseEvent> findByScheduledDateAndStatus(LocalDate scheduledDate, DoseStatus status);

    @Query("SELECT d FROM DoseEvent d WHERE d.patientId = :patientId ORDER BY d.scheduledDate DESC, d.scheduledTime DESC")
    List<DoseEvent> findRecentDosesForPatient(@Param("patientId") UUID patientId);

    @Query("SELECT d FROM DoseEvent d WHERE d.scheduledDate = :date AND d.status = 'PENDING' AND d.scheduledTime <= :currentTime AND d.notificationTriggered = false")
    List<DoseEvent> findPendingDosesNeedingNotification(
            @Param("date") LocalDate date, @Param("currentTime") LocalTime currentTime);

    @Query("SELECT d FROM DoseEvent d WHERE d.scheduledDate = :date AND d.status = 'PENDING' AND d.scheduledTime <= :cutoffTime")
    List<DoseEvent> findPendingDosesPastGracePeriod(
            @Param("date") LocalDate date, @Param("cutoffTime") LocalTime cutoffTime);
}
