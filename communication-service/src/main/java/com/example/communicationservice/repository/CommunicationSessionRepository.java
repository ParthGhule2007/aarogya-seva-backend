package com.example.communicationservice.repository;

import com.example.communicationservice.entity.CommunicationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunicationSessionRepository extends JpaRepository<CommunicationSession, Long> {

    Optional<CommunicationSession> findByConsultationId(Long consultationId);

    boolean existsByConsultationId(Long consultationId);

    Optional<CommunicationSession> findByRoomId(String roomId);
}
