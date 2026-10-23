package com.example.consultationservice.service;

import com.example.consultationservice.dto.*;
import com.example.consultationservice.enums.ConsultationStatus;

import java.util.List;

public interface ConsultationService {

    ConsultationResponseDTO createConsultation(ConsultationRequestDTO request);

    ConsultationResponseDTO getConsultationById(Long consultationId);

    List<ConsultationResponseDTO> getConsultationsByUserId(String userId);

    List<ConsultationResponseDTO> getConsultationsByDoctorId(Long doctorId);

    List<ConsultationResponseDTO> getPendingConsultationsByDoctorId(Long doctorId);

    List<DoctorResponseDTO> getAvailableDoctors();

    ConsultationResponseDTO acceptConsultation(Long consultationId);

    ConsultationResponseDTO rejectConsultation(Long consultationId);

    ConsultationResponseDTO scheduleConsultation(Long consultationId, ScheduleConsultationDTO scheduleDto);

    ConsultationResponseDTO cancelConsultation(Long consultationId, CancelConsultationDTO cancelDto);

    ConsultationResponseDTO markAsReady(Long consultationId);

    ConsultationResponseDTO startConsultation(Long consultationId);

    ConsultationResponseDTO completeConsultation(Long consultationId);

    void validateTransition(ConsultationStatus currentStatus, ConsultationStatus newStatus);
}
