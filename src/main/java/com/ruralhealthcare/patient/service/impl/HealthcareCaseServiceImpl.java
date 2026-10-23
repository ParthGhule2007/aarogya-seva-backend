package com.ruralhealthcare.patient.service.impl;

import com.ruralhealthcare.common.domain.GeoLocation;
import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.RecordSymptomsRequest;
import com.ruralhealthcare.patient.api.dto.SymptomInputDto;
import com.ruralhealthcare.patient.api.dto.UpdateCaseStatusRequest;
import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.CaseSymptom;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import com.ruralhealthcare.patient.domain.IntakeChannel;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.CaseSymptomRepository;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.patient.service.HealthcareCaseService;
import com.ruralhealthcare.patient.service.PatientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HealthcareCaseServiceImpl implements HealthcareCaseService {

    private static final Logger log = LoggerFactory.getLogger(HealthcareCaseServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HealthcareCaseRepository caseRepository;
    private final PatientRepository patientRepository;
    private final CaseSymptomRepository symptomRepository;

    public HealthcareCaseServiceImpl(HealthcareCaseRepository caseRepository,
                                     PatientRepository patientRepository,
                                     CaseSymptomRepository symptomRepository) {
        this.caseRepository = caseRepository;
        this.patientRepository = patientRepository;
        this.symptomRepository = symptomRepository;
    }

    @Override
    @Transactional
    public HealthcareCaseDto createCase(CreateCaseRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientId()));

        double lat = request.getLatitude() != null ? request.getLatitude()
                : (patient.getGeoLocation() != null ? patient.getGeoLocation().getLatitude() : 18.5204);
        double lon = request.getLongitude() != null ? request.getLongitude()
                : (patient.getGeoLocation() != null ? patient.getGeoLocation().getLongitude() : 73.8567);
        validateCoordinates(lat, lon);

        IntakeChannel channel = request.getIntakeChannel() != null ? request.getIntakeChannel() : IntakeChannel.FORM;

        String caseNumber = generateUniqueCaseNumber();

        HealthcareCase healthcareCase = HealthcareCase.builder()
                .id(UUID.randomUUID())
                .caseNumber(caseNumber)
                .patient(patient)
                .status(CaseStatus.CREATED)
                .priority(CasePriority.LOW)
                .intakeChannel(channel)
                .chiefComplaint(request.getChiefComplaint() != null ? request.getChiefComplaint().trim() : null)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .geoLocation(GeoLocation.of(lat, lon))
                .build();

        if (request.getInitialSymptoms() != null && !request.getInitialSymptoms().isEmpty()) {
            for (SymptomInputDto symptomInput : request.getInitialSymptoms()) {
                CaseSymptom symptom = CaseSymptom.builder()
                        .id(UUID.randomUUID())
                        .symptomName(symptomInput.getSymptomName().trim())
                        .severity(symptomInput.getSeverity())
                        .durationDays(symptomInput.getDurationDays())
                        .description(symptomInput.getDescription() != null ? symptomInput.getDescription().trim() : null)
                        .build();
                healthcareCase.addSymptom(symptom);
            }
        }

        HealthcareCase saved = caseRepository.save(healthcareCase);
        log.info("Opened new healthcare case [{}] (id: [{}]) for patient [{}] via channel {}",
                saved.getCaseNumber(), saved.getId(), patient.getId(), saved.getIntakeChannel());
        return PatientMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthcareCaseDto> getCaseById(UUID caseId) {
        if (caseId == null) {
            return Optional.empty();
        }
        return caseRepository.findByIdWithSymptoms(caseId).map(PatientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthcareCaseDto> getCaseByNumber(String caseNumber) {
        if (caseNumber == null || caseNumber.isBlank()) {
            return Optional.empty();
        }
        return caseRepository.findByCaseNumber(caseNumber.trim()).map(PatientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthcareCaseDto> getCasesByPatientId(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return caseRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(PatientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthcareCaseDto> getAllCases() {
        return caseRepository.findAll().stream()
                .map(PatientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HealthcareCaseDto recordSymptoms(UUID caseId, RecordSymptomsRequest request) {
        HealthcareCase healthcareCase = caseRepository.findByIdWithSymptoms(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareCase", caseId));

        if (healthcareCase.getStatus() == CaseStatus.CLOSED || healthcareCase.getStatus() == CaseStatus.CANCELLED) {
            throw new ValidationException("Cannot record symptoms for a closed or cancelled case: " + healthcareCase.getStatus());
        }

        if (request.getSymptoms() != null) {
            for (SymptomInputDto input : request.getSymptoms()) {
                CaseSymptom symptom = CaseSymptom.builder()
                        .id(UUID.randomUUID())
                        .symptomName(input.getSymptomName().trim())
                        .severity(input.getSeverity())
                        .durationDays(input.getDurationDays())
                        .description(input.getDescription() != null ? input.getDescription().trim() : null)
                        .build();
                healthcareCase.addSymptom(symptom);
            }
        }

        HealthcareCase saved = caseRepository.save(healthcareCase);
        int count = request.getSymptoms() != null ? request.getSymptoms().size() : 0;
        log.info("Recorded {} symptoms to case [{}] (id: [{}]). Current status: {}",
                count, saved.getCaseNumber(), saved.getId(), saved.getStatus());
        return PatientMapper.toDto(saved);
    }

    @Override
    @Transactional
    public HealthcareCaseDto updateCaseStatus(UUID caseId, UpdateCaseStatusRequest request) {
        HealthcareCase healthcareCase = caseRepository.findByIdWithSymptoms(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareCase", caseId));

        healthcareCase.transitionStatus(request.getStatus());

        if (request.getPriority() != null) {
            healthcareCase.setPriority(request.getPriority());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = healthcareCase.getNotes() != null ? healthcareCase.getNotes() + "\n" : "";
            healthcareCase.setNotes(existingNotes + request.getNotes().trim());
        }

        HealthcareCase saved = caseRepository.save(healthcareCase);
        log.info("Transitioned case [{}] (id: [{}]) to status [{}] with priority [{}]",
                saved.getCaseNumber(), saved.getId(), saved.getStatus(), saved.getPriority());
        return PatientMapper.toDto(saved);
    }

    private String generateUniqueCaseNumber() {
        String prefix = "CASE-" + LocalDate.now().format(DATE_FORMATTER) + "-";
        for (int i = 0; i < 10; i++) {
            String candidate = prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            if (!caseRepository.existsByCaseNumber(candidate)) {
                return candidate;
            }
        }
        return prefix + System.currentTimeMillis();
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
