package com.example.communicationservice.service.impl;

import com.example.communicationservice.config.JitsiConfig;
import com.example.communicationservice.dto.CommunicationSessionResponseDTO;
import com.example.communicationservice.dto.CreateCommunicationSessionDTO;
import com.example.communicationservice.entity.CommunicationSession;
import com.example.communicationservice.enums.CommunicationStatus;
import com.example.communicationservice.exception.CommunicationSessionNotFoundException;
import com.example.communicationservice.exception.DuplicateCommunicationSessionException;
import com.example.communicationservice.exception.InvalidCommunicationStateException;
import com.example.communicationservice.repository.CommunicationSessionRepository;
import com.example.communicationservice.service.CommunicationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CommunicationServiceImpl implements CommunicationService {

    private final CommunicationSessionRepository repository;
    private final JitsiConfig jitsiConfig;

    public CommunicationServiceImpl(CommunicationSessionRepository repository, JitsiConfig jitsiConfig) {
        this.repository = repository;
        this.jitsiConfig = jitsiConfig;
    }

    @Override
    @Transactional
    public CommunicationSessionResponseDTO createSession(CreateCommunicationSessionDTO request) {
        // Step 1: Pre-check duplicate consultation session
        if (repository.existsByConsultationId(request.getConsultationId())) {
            throw new DuplicateCommunicationSessionException(request.getConsultationId());
        }

        // Step 2: Generate unpredictable UUID room ID
        String roomId = "consultation-" + UUID.randomUUID();

        // Step 3: Generate Jitsi meeting URL
        String meetingUrl = jitsiConfig.buildMeetingUrl(roomId);

        // Step 4: Construct entity
        CommunicationSession session = CommunicationSession.builder()
                .consultationId(request.getConsultationId())
                .userId(request.getUserId())
                .doctorId(request.getDoctorId())
                .roomId(roomId)
                .meetingUrl(meetingUrl)
                .communicationType(request.getCommunicationType())
                .status(CommunicationStatus.CREATED)
                .build();

        // Step 5: Save with duplicate constraint enforcement
        try {
            CommunicationSession saved = repository.save(session);
            return mapToResponseDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCommunicationSessionException(request.getConsultationId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationSessionResponseDTO getSessionById(Long id) {
        CommunicationSession session = repository.findById(id)
                .orElseThrow(() -> new CommunicationSessionNotFoundException(id));
        return mapToResponseDTO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationSessionResponseDTO getSessionByConsultationId(Long consultationId) {
        CommunicationSession session = repository.findByConsultationId(consultationId)
                .orElseThrow(() -> new CommunicationSessionNotFoundException(
                        "Communication session not found for consultation ID: " + consultationId));
        return mapToResponseDTO(session);
    }

    @Override
    @Transactional
    public CommunicationSessionResponseDTO startSession(Long id) {
        CommunicationSession session = repository.findById(id)
                .orElseThrow(() -> new CommunicationSessionNotFoundException(id));

        // State validation: Only CREATED -> ACTIVE is valid
        if (session.getStatus() != CommunicationStatus.CREATED) {
            throw new InvalidCommunicationStateException(
                    "Cannot start session with ID " + id + ". Current status is " + session.getStatus()
                            + ". Only CREATED sessions can be transitioned to ACTIVE.");
        }

        session.setStatus(CommunicationStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());

        CommunicationSession updated = repository.save(session);
        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional
    public CommunicationSessionResponseDTO endSession(Long id) {
        CommunicationSession session = repository.findById(id)
                .orElseThrow(() -> new CommunicationSessionNotFoundException(id));

        // State validation: Only ACTIVE -> ENDED is valid
        if (session.getStatus() != CommunicationStatus.ACTIVE) {
            throw new InvalidCommunicationStateException(
                    "Cannot end session with ID " + id + ". Current status is " + session.getStatus()
                            + ". Only ACTIVE sessions can be transitioned to ENDED.");
        }

        session.setStatus(CommunicationStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());

        CommunicationSession updated = repository.save(session);
        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional
    public CommunicationSessionResponseDTO cancelSession(Long id) {
        CommunicationSession session = repository.findById(id)
                .orElseThrow(() -> new CommunicationSessionNotFoundException(id));

        // State validation: Allowed from CREATED -> CANCELLED or ACTIVE -> CANCELLED
        if (session.getStatus() != CommunicationStatus.CREATED && session.getStatus() != CommunicationStatus.ACTIVE) {
            throw new InvalidCommunicationStateException(
                    "Cannot cancel session with ID " + id + ". Current status is " + session.getStatus()
                            + ". Only CREATED or ACTIVE sessions can be cancelled.");
        }

        session.setStatus(CommunicationStatus.CANCELLED);
        session.setEndedAt(LocalDateTime.now());

        CommunicationSession updated = repository.save(session);
        return mapToResponseDTO(updated);
    }

    private CommunicationSessionResponseDTO mapToResponseDTO(CommunicationSession session) {
        return CommunicationSessionResponseDTO.builder()
                .id(session.getId())
                .consultationId(session.getConsultationId())
                .userId(session.getUserId())
                .doctorId(session.getDoctorId())
                .roomId(session.getRoomId())
                .meetingUrl(session.getMeetingUrl())
                .communicationType(session.getCommunicationType())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
