package com.example.communicationservice.service;

import com.example.communicationservice.dto.CommunicationSessionResponseDTO;
import com.example.communicationservice.dto.CreateCommunicationSessionDTO;

public interface CommunicationService {

    CommunicationSessionResponseDTO createSession(CreateCommunicationSessionDTO request);

    CommunicationSessionResponseDTO getSessionById(Long id);

    CommunicationSessionResponseDTO getSessionByConsultationId(Long consultationId);

    CommunicationSessionResponseDTO startSession(Long id);

    CommunicationSessionResponseDTO endSession(Long id);

    CommunicationSessionResponseDTO cancelSession(Long id);
}
