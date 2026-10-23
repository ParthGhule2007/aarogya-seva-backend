package com.ruralhealthcare.symptom.service.impl;

import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.service.PatientQueryService;
import com.ruralhealthcare.symptom.api.dto.BatchCreateSymptomsRequest;
import com.ruralhealthcare.symptom.api.dto.CreateSymptomRequest;
import com.ruralhealthcare.symptom.api.dto.SymptomDto;
import com.ruralhealthcare.symptom.api.dto.UpdateSymptomRequest;
import com.ruralhealthcare.symptom.domain.Symptom;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import com.ruralhealthcare.symptom.repository.SymptomRepository;
import com.ruralhealthcare.symptom.service.SymptomMapper;
import com.ruralhealthcare.symptom.service.SymptomQueryService;
import com.ruralhealthcare.symptom.service.SymptomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SymptomServiceImpl implements SymptomService, SymptomQueryService {

    private static final Logger log = LoggerFactory.getLogger(SymptomServiceImpl.class);

    private final SymptomRepository symptomRepository;
    private final PatientQueryService patientQueryService;

    public SymptomServiceImpl(SymptomRepository symptomRepository,
                              PatientQueryService patientQueryService) {
        this.symptomRepository = symptomRepository;
        this.patientQueryService = patientQueryService;
    }

    @Override
    @Transactional
    public SymptomDto addSymptom(UUID caseId, CreateSymptomRequest request) {
        validateCaseActive(caseId);

        Symptom symptom = Symptom.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .symptomName(request.getSymptomName().trim())
                .severity(request.getSeverity())
                .durationDays(request.getDurationDays())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .bodySite(request.getBodySite() != null ? request.getBodySite().trim() : null)
                .build();

        Symptom saved = symptomRepository.save(symptom);
        log.info("Recorded symptom [{}] ({}) for case [{}]", saved.getSymptomName(), saved.getSeverity(), caseId);
        return SymptomMapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<SymptomDto> addSymptomsBatch(UUID caseId, BatchCreateSymptomsRequest request) {
        validateCaseActive(caseId);

        List<Symptom> symptomsToSave = new ArrayList<>();
        for (CreateSymptomRequest item : request.getSymptoms()) {
            Symptom symptom = Symptom.builder()
                    .id(UUID.randomUUID())
                    .caseId(caseId)
                    .symptomName(item.getSymptomName().trim())
                    .severity(item.getSeverity())
                    .durationDays(item.getDurationDays())
                    .description(item.getDescription() != null ? item.getDescription().trim() : null)
                    .bodySite(item.getBodySite() != null ? item.getBodySite().trim() : null)
                    .build();
            symptomsToSave.add(symptom);
        }

        List<Symptom> saved = symptomRepository.saveAll(symptomsToSave);
        log.info("Recorded batch of {} symptoms for case [{}]", saved.size(), caseId);
        return saved.stream().map(SymptomMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SymptomDto> getSymptomsByCaseId(UUID caseId, SymptomSeverity severityFilter) {
        if (!patientQueryService.caseExists(caseId)) {
            throw new ResourceNotFoundException("HealthcareCase", caseId);
        }

        List<Symptom> list = (severityFilter != null)
                ? symptomRepository.findByCaseIdAndSeverity(caseId, severityFilter)
                : symptomRepository.findByCaseIdOrderByCreatedAtDesc(caseId);

        return list.stream().map(SymptomMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SymptomDto> getSymptomById(UUID symptomId) {
        if (symptomId == null) {
            return Optional.empty();
        }
        return symptomRepository.findById(symptomId).map(SymptomMapper::toDto);
    }

    @Override
    @Transactional
    public SymptomDto updateSymptom(UUID symptomId, UpdateSymptomRequest request) {
        Symptom symptom = symptomRepository.findById(symptomId)
                .orElseThrow(() -> new ResourceNotFoundException("Symptom", symptomId));

        validateCaseActive(symptom.getCaseId());

        if (request.getSymptomName() != null && !request.getSymptomName().isBlank()) {
            symptom.setSymptomName(request.getSymptomName().trim());
        }
        if (request.getSeverity() != null) {
            symptom.setSeverity(request.getSeverity());
        }
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            symptom.setDurationDays(request.getDurationDays());
        }
        if (request.getDescription() != null) {
            symptom.setDescription(request.getDescription().trim());
        }
        if (request.getBodySite() != null) {
            symptom.setBodySite(request.getBodySite().trim());
        }

        Symptom updated = symptomRepository.save(symptom);
        log.info("Updated symptom [{}] ({}) for case [{}]", updated.getId(), updated.getSymptomName(), updated.getCaseId());
        return SymptomMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void removeSymptom(UUID symptomId) {
        Symptom symptom = symptomRepository.findById(symptomId)
                .orElseThrow(() -> new ResourceNotFoundException("Symptom", symptomId));

        validateCaseActive(symptom.getCaseId());

        symptomRepository.delete(symptom);
        log.info("Removed symptom [{}] from case [{}]", symptomId, symptom.getCaseId());
    }

    // --- Public Cross-Module Contract (SymptomQueryService) ---

    @Override
    @Transactional(readOnly = true)
    public List<SymptomDto> getSymptomsForCase(UUID caseId) {
        return getSymptomsByCaseId(caseId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SymptomDto> getSymptomsBySeverity(UUID caseId, SymptomSeverity severity) {
        return getSymptomsByCaseId(caseId, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSevereOrCriticalSymptoms(UUID caseId) {
        if (caseId == null) {
            return false;
        }
        long count = symptomRepository.countByCaseIdAndSeverityIn(
                caseId, List.of(SymptomSeverity.SEVERE, SymptomSeverity.CRITICAL)
        );
        return count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSymptoms(UUID caseId) {
        if (caseId == null) {
            return 0;
        }
        return symptomRepository.countByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSymptoms(UUID caseId) {
        if (caseId == null) {
            return false;
        }
        return symptomRepository.existsByCaseId(caseId);
    }

    private void validateCaseActive(UUID caseId) {
        HealthcareCaseDto caseDto = patientQueryService.findCaseById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareCase", caseId));

        if (caseDto.getStatus() == CaseStatus.CLOSED || caseDto.getStatus() == CaseStatus.CANCELLED) {
            throw new ValidationException(String.format(
                    "Cannot modify symptoms for a case in terminal status [%s]", caseDto.getStatus()
            ));
        }
    }
}
