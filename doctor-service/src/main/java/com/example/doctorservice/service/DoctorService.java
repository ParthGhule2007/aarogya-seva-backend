package com.example.doctorservice.service;

import com.example.doctorservice.dto.*;

import java.util.List;

public interface DoctorService {

    /**
     * Register a new doctor profile.
     */
    DoctorResponseDTO createDoctor(DoctorRequestDTO request);

    /**
     * Retrieve doctor by internal primary key ID.
     */
    DoctorResponseDTO getDoctorById(Long doctorId);

    /**
     * Retrieve doctor profile by external User Service user ID.
     */
    DoctorResponseDTO getDoctorByUserId(String userId);

    /**
     * Update doctor profile information.
     */
    DoctorResponseDTO updateDoctor(Long doctorId, DoctorRequestDTO request);

    /**
     * Soft activate or deactivate a doctor account.
     */
    DoctorResponseDTO updateStatus(Long doctorId, StatusRequestDTO request);

    /**
     * Update doctor availability for consultations.
     * If transitioning from BUSY to AVAILABLE, requires coordination from Consultation Service.
     */
    DoctorResponseDTO updateAvailability(Long doctorId, AvailabilityRequestDTO request, boolean coordinated);

    /**
     * Find all doctors who are ACTIVE and currently AVAILABLE.
     */
    List<AvailableDoctorResponseDTO> findAvailableDoctors();

    /**
     * Find all doctors, sorted with AVAILABLE doctors first.
     */
    List<DoctorResponseDTO> getAllDoctors();
}
