package com.ruralhealthcare.patient.service.impl;

import com.ruralhealthcare.common.domain.GeoLocation;
import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.CreatePatientRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.api.dto.UpdatePatientRequest;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.patient.service.PatientMapper;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.patient.service.PatientService;
import com.ruralhealthcare.auth.domain.UserRole;
import com.ruralhealthcare.auth.repository.UserRepository;
import com.ruralhealthcare.auth.security.AuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService, PatientQueryService {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceImpl.class);

    private final PatientRepository patientRepository;
    private final HealthcareCaseRepository caseRepository;
    private final UserRepository userRepository;

    public PatientServiceImpl(PatientRepository patientRepository,
                              HealthcareCaseRepository caseRepository,
                              UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PatientDto registerPatient(CreatePatientRequest request) {
        String cleanPhone = request.getContactNumber().trim();
        if (patientRepository.existsByContactNumber(cleanPhone)) {
            throw new ValidationException("Patient already registered with contact number: " + cleanPhone);
        }

        GeoLocation geoLocation = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            validateCoordinates(request.getLatitude(), request.getLongitude());
            geoLocation = GeoLocation.of(request.getLatitude(), request.getLongitude());
        }

        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .fullName(request.getFullName().trim())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .age(request.getAge())
                .contactNumber(cleanPhone)
                .emergencyContactName(request.getEmergencyContactName() != null ? request.getEmergencyContactName().trim() : null)
                .emergencyContactNumber(request.getEmergencyContactNumber() != null ? request.getEmergencyContactNumber().trim() : null)
                .villageTown(request.getVillageTown().trim())
                .subDistrict(request.getSubDistrict().trim())
                .district(request.getDistrict().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode().trim())
                .bloodGroup(request.getBloodGroup() != null ? request.getBloodGroup().trim() : null)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage().trim() : "Hindi")
                .geoLocation(geoLocation)
                .build();

        Patient saved = patientRepository.save(patient);
        log.info("Registered new patient [{}] ({}) from {}, {}", saved.getId(), saved.getFullName(), saved.getVillageTown(), saved.getDistrict());

        AuthenticationContext.getPrincipal().ifPresent(principal -> {
            if (principal.getRole() == UserRole.PATIENT && userRepository != null) {
                userRepository.findById(principal.getUserId()).ifPresent(user -> {
                    if (user.getAssociatedEntityId() == null || user.getAssociatedEntityId().isBlank()) {
                        user.setAssociatedEntityId(saved.getId().toString());
                        userRepository.save(user);
                        log.info("Linked registered patient [{}] to authenticated user [{}]", saved.getId(), user.getUsername());
                    }
                });
            }
        });

        return PatientMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientDto> getPatientById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return patientRepository.findById(id).map(PatientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientDto> getPatientByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            return Optional.empty();
        }
        return patientRepository.findByContactNumber(contactNumber.trim()).map(PatientMapper::toDto);
    }

    @Override
    @Transactional
    public PatientDto updatePatient(UUID id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            patient.setFullName(request.getFullName().trim());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAge() != null) {
            patient.setAge(request.getAge());
        }
        if (request.getContactNumber() != null && !request.getContactNumber().isBlank()) {
            String newPhone = request.getContactNumber().trim();
            if (!newPhone.equals(patient.getContactNumber()) && patientRepository.existsByContactNumber(newPhone)) {
                throw new ValidationException("Contact number already registered by another patient: " + newPhone);
            }
            patient.setContactNumber(newPhone);
        }
        if (request.getEmergencyContactName() != null) {
            patient.setEmergencyContactName(request.getEmergencyContactName().trim());
        }
        if (request.getEmergencyContactNumber() != null) {
            patient.setEmergencyContactNumber(request.getEmergencyContactNumber().trim());
        }
        if (request.getVillageTown() != null) {
            patient.setVillageTown(request.getVillageTown().trim());
        }
        if (request.getSubDistrict() != null) {
            patient.setSubDistrict(request.getSubDistrict().trim());
        }
        if (request.getDistrict() != null) {
            patient.setDistrict(request.getDistrict().trim());
        }
        if (request.getState() != null) {
            patient.setState(request.getState().trim());
        }
        if (request.getPincode() != null) {
            patient.setPincode(request.getPincode().trim());
        }
        if (request.getBloodGroup() != null) {
            patient.setBloodGroup(request.getBloodGroup().trim());
        }
        if (request.getPreferredLanguage() != null) {
            patient.setPreferredLanguage(request.getPreferredLanguage().trim());
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            validateCoordinates(request.getLatitude(), request.getLongitude());
            patient.setGeoLocation(GeoLocation.of(request.getLatitude(), request.getLongitude()));
        }

        Patient updated = patientRepository.save(patient);
        log.info("Updated patient [{}] profile", updated.getId());
        return PatientMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> searchPatientsByName(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            return List.of();
        }
        return patientRepository.findByFullNameContainingIgnoreCase(nameFragment.trim()).stream()
                .map(PatientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(PatientMapper::toDto)
                .collect(Collectors.toList());
    }

    // --- Public Cross-Module Contract (PatientQueryService) ---

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientDto> findPatientById(UUID patientId) {
        return getPatientById(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean patientExists(UUID patientId) {
        if (patientId == null) {
            return false;
        }
        return patientRepository.existsById(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthcareCaseDto> findCaseById(UUID caseId) {
        if (caseId == null) {
            return Optional.empty();
        }
        return caseRepository.findByIdWithSymptoms(caseId).map(PatientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean caseExists(UUID caseId) {
        if (caseId == null) {
            return false;
        }
        return caseRepository.existsById(caseId);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0");
        }
    }
}
