package com.ruralhealthcare.patient.repository;

import com.ruralhealthcare.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByContactNumber(String contactNumber);

    boolean existsByContactNumber(String contactNumber);

    List<Patient> findByDistrictIgnoreCaseAndStateIgnoreCase(String district, String state);

    List<Patient> findByFullNameContainingIgnoreCase(String nameFragment);
}
