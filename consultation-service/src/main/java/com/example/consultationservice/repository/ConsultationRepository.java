package com.example.consultationservice.repository;

import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.ConsultationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByUserId(String userId);

    List<Consultation> findByUserIdOrderByRequestedAtDesc(String userId);

    List<Consultation> findByDoctorIdOrderByRequestedAtDesc(Long doctorId);

    List<Consultation> findByDoctorIdAndStatus(Long doctorId, ConsultationStatus status);

    List<Consultation> findByDoctorIdAndStatusOrderByRequestedAtAsc(Long doctorId, ConsultationStatus status);

    List<Consultation> findByStatusAndScheduledAtLessThanEqual(ConsultationStatus status, LocalDateTime threshold);
}
