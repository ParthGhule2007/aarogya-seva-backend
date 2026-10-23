package com.example.consultationservice.service;

import com.example.consultationservice.client.CommunicationServiceClient;
import com.example.consultationservice.client.DoctorServiceClient;
import com.example.consultationservice.dto.*;
import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.ConsultationStatus;
import com.example.consultationservice.exception.*;
import com.example.consultationservice.repository.ConsultationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationServiceImpl.class);
    private static final String SERVICE_SOURCE_HEADER = "Consultation-Service";

    private final ConsultationRepository consultationRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final CommunicationServiceClient communicationServiceClient;

    @Value("${jitsi.base-url:https://meet.jit.si}")
    private String jitsiBaseUrl = "https://meet.jit.si";

    public ConsultationServiceImpl(ConsultationRepository consultationRepository,
                                   DoctorServiceClient doctorServiceClient,
                                   CommunicationServiceClient communicationServiceClient) {
        this.consultationRepository = consultationRepository;
        this.doctorServiceClient = doctorServiceClient;
        this.communicationServiceClient = communicationServiceClient;
    }

    @Override
    @Transactional
    public ConsultationResponseDTO createConsultation(ConsultationRequestDTO request) {
        log.info("Creating consultation request for user: {} and doctor: {}", request.getUserId(), request.getDoctorId());

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        if (request.getDoctorId() == null || request.getDoctorId() <= 0) {
            throw new IllegalArgumentException("Invalid doctor ID");
        }

        String resolvedDoctorName = null;
        try {
            DoctorResponseDTO doctor = doctorServiceClient.getDoctorById(request.getDoctorId());
            if (doctor != null && doctor.getName() != null) {
                resolvedDoctorName = doctor.getName();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve doctor details from Doctor Service for doctorId {}: {}",
                    request.getDoctorId(), e.getMessage());
        }

        Consultation consultation = Consultation.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .doctorId(request.getDoctorId())
                .doctorName(resolvedDoctorName)
                .disease(request.getDisease())
                .urgency(request.getUrgency())
                .consultationType(request.getConsultationType())
                .status(ConsultationStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation created with ID: {}", saved.getId());
        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponseDTO getConsultationById(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));
        return toResponseDTO(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> getConsultationsByUserId(String userId) {
        return consultationRepository.findByUserIdOrderByRequestedAtDesc(userId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> getConsultationsByDoctorId(Long doctorId) {
        return consultationRepository.findByDoctorIdOrderByRequestedAtDesc(doctorId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> getPendingConsultationsByDoctorId(Long doctorId) {
        return consultationRepository.findByDoctorIdAndStatusOrderByRequestedAtAsc(doctorId, ConsultationStatus.REQUESTED)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorResponseDTO> getAvailableDoctors() {
        try {
            return doctorServiceClient.getAvailableDoctors();
        } catch (Exception e) {
            log.error("Failed to query available doctors from Doctor Service: {}", e.getMessage());
            throw new DoctorServiceException("Unable to fetch available doctors from Doctor Service: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ConsultationResponseDTO acceptConsultation(Long consultationId) {
        log.info("Doctor attempting to accept consultation ID: {}", consultationId);
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.ACCEPTED);

        // Update Doctor Service availability to BUSY
        try {
            doctorServiceClient.updateDoctorAvailability(
                    consultation.getDoctorId(),
                    new DoctorAvailabilityUpdateDTO("BUSY"),
                    SERVICE_SOURCE_HEADER
            );
            log.info("Successfully updated doctor {} availability to BUSY", consultation.getDoctorId());
        } catch (Exception e) {
            log.error("Doctor Service rejected or failed availability update for doctor {}: {}",
                    consultation.getDoctorId(), e.getMessage());
            throw new DoctorUnavailableException(
                    "Doctor " + consultation.getDoctorId() + " cannot be set to BUSY: " + e.getMessage()
            );
        }

        // Call Communication Service to create communication session
        CommunicationSessionResponseDTO commSession;
        try {
            CreateCommunicationSessionRequestDTO sessionRequest = CreateCommunicationSessionRequestDTO.builder()
                    .consultationId(consultation.getId())
                    .userId(consultation.getUserId())
                    .doctorId(consultation.getDoctorId())
                    .communicationType(consultation.getConsultationType() != null
                            ? consultation.getConsultationType().name()
                            : "VIDEO")
                    .build();
            commSession = communicationServiceClient.createSession(sessionRequest);
            log.info("Communication session created for consultation {}: sessionId={}, roomId={}, meetingUrl={}",
                    consultation.getId(),
                    commSession != null ? commSession.getId() : null,
                    commSession != null ? commSession.getRoomId() : null,
                    commSession != null ? commSession.getMeetingUrl() : null);
        } catch (Exception e) {
            log.error("Communication Service failed to create session for consultation {}: {}. Compensating by reverting doctor {} to AVAILABLE.",
                    consultation.getId(), e.getMessage(), consultation.getDoctorId());
            // Compensating action: revert doctor to AVAILABLE
            try {
                doctorServiceClient.updateDoctorAvailability(
                        consultation.getDoctorId(),
                        new DoctorAvailabilityUpdateDTO("AVAILABLE"),
                        SERVICE_SOURCE_HEADER
                );
                log.info("Successfully rolled back doctor {} availability to AVAILABLE", consultation.getDoctorId());
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback doctor {} availability to AVAILABLE: {}",
                        consultation.getDoctorId(), rollbackEx.getMessage());
            }
            throw new CommunicationServiceException("Communication Service failed to create session: " + e.getMessage(), e);
        }

        if (commSession != null && commSession.getRoomId() != null) {
            consultation.setRoomId(commSession.getRoomId());
        }

        consultation.setStatus(ConsultationStatus.ACCEPTED);
        consultation.setAcceptedAt(LocalDateTime.now());

        Consultation saved = consultationRepository.save(consultation);
        ConsultationResponseDTO response = toResponseDTO(saved);
        if (commSession != null && commSession.getMeetingUrl() != null) {
            response.setMeetingUrl(commSession.getMeetingUrl());
        }
        return response;
    }

    @Override
    @Transactional
    public ConsultationResponseDTO rejectConsultation(Long consultationId) {
        log.info("Doctor attempting to reject consultation ID: {}", consultationId);
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.REJECTED);

        consultation.setStatus(ConsultationStatus.REJECTED);
        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConsultationResponseDTO scheduleConsultation(Long consultationId, ScheduleConsultationDTO scheduleDto) {
        log.info("Scheduling consultation ID: {} for {}", consultationId, scheduleDto.getScheduledAt());
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.SCHEDULED);

        if (scheduleDto.getScheduledAt() == null || scheduleDto.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new InvalidScheduleTimeException("Scheduled time must be in the future");
        }

        consultation.setStatus(ConsultationStatus.SCHEDULED);
        consultation.setScheduledAt(scheduleDto.getScheduledAt());

        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConsultationResponseDTO cancelConsultation(Long consultationId, CancelConsultationDTO cancelDto) {
        log.info("Cancelling consultation ID: {} by {}", consultationId, cancelDto.getCancelledBy());
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        ConsultationStatus previousStatus = consultation.getStatus();
        validateTransition(previousStatus, ConsultationStatus.CANCELLED);

        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultation.setCancelledAt(LocalDateTime.now());
        consultation.setCancelledBy(cancelDto.getCancelledBy());

        // If the consultation was already accepted (doctor was marked BUSY), coordinate release
        if (previousStatus == ConsultationStatus.ACCEPTED) {
            try {
                doctorServiceClient.updateDoctorAvailability(
                        consultation.getDoctorId(),
                        new DoctorAvailabilityUpdateDTO("AVAILABLE"),
                        SERVICE_SOURCE_HEADER
                );
                log.info("Notified Doctor Service to reset doctor {} to AVAILABLE upon cancellation", consultation.getDoctorId());
            } catch (Exception e) {
                log.warn("Failed to notify Doctor Service to revert doctor {} to AVAILABLE: {}",
                        consultation.getDoctorId(), e.getMessage());
            }
        }

        // Cancel communication session in Communication Service if exists
        if (consultation.getRoomId() != null || previousStatus == ConsultationStatus.ACCEPTED) {
            try {
                CommunicationSessionResponseDTO session = communicationServiceClient.getSessionByConsultationId(consultationId);
                if (session != null && session.getId() != null) {
                    communicationServiceClient.cancelSession(session.getId());
                    log.info("Cancelled communication session {} for consultation {}", session.getId(), consultationId);
                }
            } catch (Exception e) {
                log.warn("Could not cancel communication session for consultation {}: {}", consultationId, e.getMessage());
            }
        }

        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConsultationResponseDTO markAsReady(Long consultationId) {
        log.info("Marking consultation ID: {} as READY", consultationId);
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.READY);

        consultation.setStatus(ConsultationStatus.READY);
        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConsultationResponseDTO startConsultation(Long consultationId) {
        log.info("Starting consultation ID: {}", consultationId);
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.IN_CALL);

        // Start communication session in Communication Service if exists
        try {
            CommunicationSessionResponseDTO session = communicationServiceClient.getSessionByConsultationId(consultationId);
            if (session != null && session.getId() != null) {
                communicationServiceClient.startSession(session.getId());
                log.info("Started communication session {} for consultation {}", session.getId(), consultationId);
            }
        } catch (Exception e) {
            log.warn("Could not start communication session for consultation {}: {}", consultationId, e.getMessage());
        }

        consultation.setStatus(ConsultationStatus.IN_CALL);
        consultation.setStartedAt(LocalDateTime.now());

        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConsultationResponseDTO completeConsultation(Long consultationId) {
        log.info("Completing consultation ID: {}", consultationId);
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        validateTransition(consultation.getStatus(), ConsultationStatus.COMPLETED);

        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setCompletedAt(LocalDateTime.now());

        // Notify Doctor Service to release doctor to AVAILABLE
        try {
            doctorServiceClient.updateDoctorAvailability(
                    consultation.getDoctorId(),
                    new DoctorAvailabilityUpdateDTO("AVAILABLE"),
                    SERVICE_SOURCE_HEADER
            );
            log.info("Released doctor {} to AVAILABLE in Doctor Service", consultation.getDoctorId());
        } catch (Exception e) {
            log.error("Failed to notify Doctor Service to mark doctor {} AVAILABLE: {}",
                    consultation.getDoctorId(), e.getMessage());
            throw new DoctorServiceException(
                    "Failed to release doctor availability in Doctor Service: " + e.getMessage(), e
            );
        }

        // End communication session in Communication Service if exists
        try {
            CommunicationSessionResponseDTO session = communicationServiceClient.getSessionByConsultationId(consultationId);
            if (session != null && session.getId() != null) {
                communicationServiceClient.endSession(session.getId());
                log.info("Ended communication session {} for consultation {}", session.getId(), consultationId);
            }
        } catch (Exception e) {
            log.warn("Could not end communication session for consultation {}: {}", consultationId, e.getMessage());
        }

        Consultation saved = consultationRepository.save(consultation);
        return toResponseDTO(saved);
    }

    @Override
    public void validateTransition(ConsultationStatus currentStatus, ConsultationStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case REQUESTED -> newStatus == ConsultationStatus.ACCEPTED
                    || newStatus == ConsultationStatus.SCHEDULED
                    || newStatus == ConsultationStatus.REJECTED
                    || newStatus == ConsultationStatus.CANCELLED;
            case ACCEPTED -> newStatus == ConsultationStatus.READY
                    || newStatus == ConsultationStatus.CANCELLED;
            case SCHEDULED -> newStatus == ConsultationStatus.READY
                    || newStatus == ConsultationStatus.CANCELLED;
            case READY -> newStatus == ConsultationStatus.IN_CALL;
            case IN_CALL -> newStatus == ConsultationStatus.COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };

        if (!valid) {
            throw new InvalidConsultationStateException(
                    String.format("Invalid state transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    private ConsultationResponseDTO toResponseDTO(Consultation consultation) {
        String meetingUrl = buildMeetingUrl(consultation.getRoomId());
        return ConsultationResponseDTO.builder()
                .consultationId(consultation.getId())
                .id(consultation.getId())
                .userId(consultation.getUserId())
                .username(consultation.getUsername())
                .doctorId(consultation.getDoctorId())
                .doctorName(consultation.getDoctorName())
                .disease(consultation.getDisease())
                .urgency(consultation.getUrgency())
                .consultationType(consultation.getConsultationType())
                .status(consultation.getStatus())
                .requestedAt(consultation.getRequestedAt())
                .acceptedAt(consultation.getAcceptedAt())
                .scheduledAt(consultation.getScheduledAt())
                .startedAt(consultation.getStartedAt())
                .completedAt(consultation.getCompletedAt())
                .cancelledAt(consultation.getCancelledAt())
                .cancelledBy(consultation.getCancelledBy())
                .roomId(consultation.getRoomId())
                .meetingUrl(meetingUrl)
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }

    private String buildMeetingUrl(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        String cleanBase = (jitsiBaseUrl != null && !jitsiBaseUrl.isBlank())
                ? jitsiBaseUrl.trim().replaceAll("/+$", "")
                : "https://meet.jit.si";
        return cleanBase + "/" + roomId;
    }
}
