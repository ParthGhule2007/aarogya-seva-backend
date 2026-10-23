package com.example.doctorservice.repository;

import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.enums.DoctorAvailabilityStatus;
import com.example.doctorservice.enums.DoctorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Find doctors by account status and availability status.
     * Used by Consultation Service to find active and available doctors.
     */
    List<Doctor> findByStatusAndAvailabilityStatus(
        DoctorStatus status,
        DoctorAvailabilityStatus availabilityStatus
    );

    /**
     * Find doctor by external User Service user ID.
     */
    Optional<Doctor> findByUserId(String userId);

    /**
     * Find doctor by medical license number.
     */
    Optional<Doctor> findByLicenseNumber(String licenseNumber);

    /**
     * Check if a license number is already registered.
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Check if a profile already exists for given user ID.
     */
    boolean existsByUserId(String userId);
}

