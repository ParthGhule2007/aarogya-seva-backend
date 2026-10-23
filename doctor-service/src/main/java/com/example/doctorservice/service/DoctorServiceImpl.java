package com.example.doctorservice.service;

import com.example.doctorservice.dto.*;
import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.enums.DoctorAvailabilityStatus;
import com.example.doctorservice.enums.DoctorStatus;
import com.example.doctorservice.exception.DoctorAlreadyExistsException;
import com.example.doctorservice.exception.DoctorNotFoundException;
import com.example.doctorservice.exception.InvalidAvailabilityStatusException;
import com.example.doctorservice.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);

    private final DoctorRepository doctorRepository;

    // Constructor injection (no field injection)
    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public DoctorResponseDTO createDoctor(DoctorRequestDTO request) {
        log.info("Creating new doctor profile for userId: {}, licenseNumber: {}", request.getUserId(), request.getLicenseNumber());

        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DoctorAlreadyExistsException("Doctor with license number '" + request.getLicenseNumber() + "' already exists");
        }

        if (doctorRepository.existsByUserId(request.getUserId())) {
            throw new DoctorAlreadyExistsException("Doctor profile already exists for user ID: " + request.getUserId());
        }

        Doctor doctor = Doctor.builder()
                .userId(request.getUserId())
                .name(request.getName().trim())
                .specialization(request.getSpecialization().trim())
                .licenseNumber(request.getLicenseNumber().trim())
                .experienceYears(request.getExperienceYears())
                .qualification(request.getQualification().trim())
                .hospitalName(request.getHospitalName().trim())
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Successfully registered doctor ID: {} for userId: {}", savedDoctor.getId(), savedDoctor.getUserId());
        return mapToResponseDTO(savedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDTO getDoctorById(Long doctorId) {
        log.info("Fetching doctor details for doctorId: {}", doctorId);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor with id " + doctorId + " not found"));
        return mapToResponseDTO(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDTO getDoctorByUserId(String userId) {
        log.info("Fetching doctor details for userId: {}", userId);
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor with user ID " + userId + " not found"));
        return mapToResponseDTO(doctor);
    }

    @Override
    @Transactional
    public DoctorResponseDTO updateDoctor(Long doctorId, DoctorRequestDTO request) {
        log.info("Updating doctor profile for doctorId: {}", doctorId);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor with id " + doctorId + " not found"));

        // If license number is changed, ensure the new license number is not already taken by another doctor
        if (!doctor.getLicenseNumber().equalsIgnoreCase(request.getLicenseNumber().trim())) {
            doctorRepository.findByLicenseNumber(request.getLicenseNumber().trim())
                    .ifPresent(existingWithLicense -> {
                        if (!existingWithLicense.getId().equals(doctorId)) {
                            throw new DoctorAlreadyExistsException("Doctor with license number '" + request.getLicenseNumber() + "' already exists");
                        }
                    });
            doctor.setLicenseNumber(request.getLicenseNumber().trim());
        }

        doctor.setName(request.getName().trim());
        doctor.setSpecialization(request.getSpecialization().trim());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setQualification(request.getQualification().trim());
        doctor.setHospitalName(request.getHospitalName().trim());

        Doctor updatedDoctor = doctorRepository.save(doctor);
        log.info("Successfully updated doctor profile for doctorId: {}", doctorId);
        return mapToResponseDTO(updatedDoctor);
    }

    @Override
    @Transactional
    public DoctorResponseDTO updateStatus(Long doctorId, StatusRequestDTO request) {
        log.info("Updating account status for doctorId: {} to {}", doctorId, request.getStatus());
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor with id " + doctorId + " not found"));

        doctor.setStatus(request.getStatus());

        // When doctor account becomes INACTIVE, reset availability to OFFLINE
        if (request.getStatus() == DoctorStatus.INACTIVE) {
            doctor.setAvailabilityStatus(DoctorAvailabilityStatus.OFFLINE);
            log.info("Doctor ID: {} marked INACTIVE, availabilityStatus reset to OFFLINE", doctorId);
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return mapToResponseDTO(updatedDoctor);
    }

    @Override
    @Transactional
    public DoctorResponseDTO updateAvailability(Long doctorId, AvailabilityRequestDTO request, boolean coordinated) {
        log.info("Updating availability status for doctorId: {} to {} (coordinated: {})",
                doctorId, request.getAvailabilityStatus(), coordinated);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor with id " + doctorId + " not found"));

        // Rule 1: An INACTIVE doctor cannot become AVAILABLE (or BUSY)
        if (doctor.getStatus() == DoctorStatus.INACTIVE && request.getAvailabilityStatus() == DoctorAvailabilityStatus.AVAILABLE) {
            log.warn("Attempt to set AVAILABLE for INACTIVE doctor ID: {}", doctorId);
            throw new InvalidAvailabilityStatusException(
                    "Cannot set availability to AVAILABLE: Doctor account is INACTIVE. Activate account first."
            );
        }
        if (doctor.getStatus() == DoctorStatus.INACTIVE && request.getAvailabilityStatus() == DoctorAvailabilityStatus.BUSY) {
            log.warn("Attempt to set BUSY for INACTIVE doctor ID: {}", doctorId);
            throw new InvalidAvailabilityStatusException(
                    "Cannot set availability to BUSY: Doctor account is INACTIVE. Activate account first."
            );
        }

        // Rule 2: A BUSY doctor cannot become AVAILABLE through an arbitrary update if they are currently handling an active consultation.
        // The Consultation Service should coordinate the transition upon consultation completion.
        if (doctor.getAvailabilityStatus() == DoctorAvailabilityStatus.BUSY
                && request.getAvailabilityStatus() == DoctorAvailabilityStatus.AVAILABLE
                && !coordinated) {
            log.warn("Arbitrary update attempt from BUSY to AVAILABLE for doctor ID: {} without consultation coordination", doctorId);
            throw new InvalidAvailabilityStatusException(
                    "A BUSY doctor cannot become AVAILABLE through an arbitrary update while handling an active consultation. " +
                    "The Consultation Service must coordinate this transition upon consultation completion."
            );
        }

        doctor.setAvailabilityStatus(request.getAvailabilityStatus());
        Doctor updatedDoctor = doctorRepository.save(doctor);
        log.info("Doctor ID: {} availabilityStatus updated to {}", doctorId, updatedDoctor.getAvailabilityStatus());
        return mapToResponseDTO(updatedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableDoctorResponseDTO> findAvailableDoctors() {
        log.info("Fetching all ACTIVE and AVAILABLE doctors");
        List<Doctor> doctors = doctorRepository.findByStatusAndAvailabilityStatus(
                DoctorStatus.ACTIVE,
                DoctorAvailabilityStatus.AVAILABLE
        );

        return doctors.stream()
                .map(this::mapToAvailableDoctorResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponseDTO> getAllDoctors() {
        log.info("Fetching all doctors sorted with AVAILABLE first");
        return doctorRepository.findAll().stream()
                .sorted((d1, d2) -> {
                    boolean d1Avail = d1.getAvailabilityStatus() == DoctorAvailabilityStatus.AVAILABLE;
                    boolean d2Avail = d2.getAvailabilityStatus() == DoctorAvailabilityStatus.AVAILABLE;
                    if (d1Avail && !d2Avail) return -1;
                    if (!d1Avail && d2Avail) return 1;
                    return d1.getName().compareToIgnoreCase(d2.getName());
                })
                .map(this::mapToResponseDTO)
                .toList();
    }

    private DoctorResponseDTO mapToResponseDTO(Doctor doctor) {
        return DoctorResponseDTO.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .licenseNumber(doctor.getLicenseNumber())
                .experienceYears(doctor.getExperienceYears())
                .qualification(doctor.getQualification())
                .hospitalName(doctor.getHospitalName())
                .status(doctor.getStatus())
                .availabilityStatus(doctor.getAvailabilityStatus())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }

    private AvailableDoctorResponseDTO mapToAvailableDoctorResponseDTO(Doctor doctor) {
        return AvailableDoctorResponseDTO.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .experienceYears(doctor.getExperienceYears())
                .qualification(doctor.getQualification())
                .hospitalName(doctor.getHospitalName())
                .availabilityStatus(doctor.getAvailabilityStatus())
                .build();
    }
}
