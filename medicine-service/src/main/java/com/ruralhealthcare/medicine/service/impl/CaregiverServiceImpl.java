package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.domain.Caregiver;
import com.ruralhealthcare.medicine.domain.CaregiverEscalationLog;
import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CaregiverResponse;
import com.ruralhealthcare.medicine.dto.EscalationLogResponse;
import com.ruralhealthcare.medicine.exception.ResourceNotFoundException;
import com.ruralhealthcare.medicine.repository.CaregiverEscalationLogRepository;
import com.ruralhealthcare.medicine.repository.CaregiverRepository;
import com.ruralhealthcare.medicine.security.SecurityUtils;
import com.ruralhealthcare.medicine.service.CaregiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaregiverServiceImpl implements CaregiverService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverServiceImpl.class);

    private final CaregiverRepository caregiverRepository;
    private final CaregiverEscalationLogRepository escalationLogRepository;
    private final SecurityUtils securityUtils;

    public CaregiverServiceImpl(CaregiverRepository caregiverRepository,
                                CaregiverEscalationLogRepository escalationLogRepository,
                                SecurityUtils securityUtils) {
        this.caregiverRepository = caregiverRepository;
        this.escalationLogRepository = escalationLogRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public CaregiverResponse createCaregiver(CaregiverRequest request) {
        securityUtils.assertPatientAccess(request.getPatientId());

        Caregiver caregiver = Caregiver.builder()
                .patientId(request.getPatientId())
                .name(request.getName())
                .relationship(request.getRelationship())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .preferredChannel(request.getPreferredChannel())
                .isActive(true)
                .build();

        Caregiver saved = caregiverRepository.save(caregiver);
        log.info("Registered caregiver [{}] for patient [{}] with channel [{}]",
                saved.getId(), saved.getPatientId(), saved.getPreferredChannel());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaregiverResponse> getCaregiversForPatient(UUID patientId) {
        securityUtils.assertPatientAccess(patientId);
        return caregiverRepository.findByPatientIdAndIsActiveTrue(patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CaregiverResponse updateCaregiver(UUID id, CaregiverRequest request) {
        Caregiver caregiver = caregiverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caregiver not found with id: " + id));

        securityUtils.assertPatientAccess(caregiver.getPatientId());

        if (request.getName() != null) caregiver.setName(request.getName());
        if (request.getRelationship() != null) caregiver.setRelationship(request.getRelationship());
        if (request.getPhoneNumber() != null) caregiver.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) caregiver.setEmail(request.getEmail());
        if (request.getPreferredChannel() != null) caregiver.setPreferredChannel(request.getPreferredChannel());

        Caregiver updated = caregiverRepository.save(caregiver);
        log.info("Updated caregiver [{}] for patient [{}]", updated.getId(), updated.getPatientId());
        return mapToResponse(updated);
    }

    @Override
    public void deactivateCaregiver(UUID id) {
        Caregiver caregiver = caregiverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caregiver not found with id: " + id));

        securityUtils.assertPatientAccess(caregiver.getPatientId());
        caregiver.setActive(false);
        caregiverRepository.save(caregiver);
        log.info("Deactivated caregiver [{}] for patient [{}]", id, caregiver.getPatientId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalationLogResponse> getEscalationHistoryForPatient(UUID patientId) {
        securityUtils.assertPatientAccess(patientId);
        List<CaregiverEscalationLog> logs = escalationLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return logs.stream().map(this::mapToEscalationResponse).collect(Collectors.toList());
    }

    private CaregiverResponse mapToResponse(Caregiver caregiver) {
        return new CaregiverResponse(
                caregiver.getId(),
                caregiver.getPatientId(),
                caregiver.getName(),
                caregiver.getRelationship(),
                caregiver.getPhoneNumber(),
                caregiver.getEmail(),
                caregiver.getPreferredChannel(),
                caregiver.isActive(),
                caregiver.getCreatedAt(),
                caregiver.getUpdatedAt()
        );
    }

    private EscalationLogResponse mapToEscalationResponse(CaregiverEscalationLog log) {
        return new EscalationLogResponse(
                log.getId(),
                log.getPatientId(),
                log.getMedicationId(),
                log.getMedicationName(),
                log.getStreakStartDoseId(),
                log.getTriggeringDoseId(),
                log.getConsecutiveMissedCount(),
                log.getChannel(),
                log.getRecipient(),
                log.getDeliveryStatus(),
                log.getMessageContent(),
                log.getProviderReference(),
                log.getDeliveryAttemptedAt(),
                log.getCreatedAt()
        );
    }
}
