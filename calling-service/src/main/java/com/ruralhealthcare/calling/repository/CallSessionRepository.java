package com.ruralhealthcare.calling.repository;

import com.ruralhealthcare.calling.domain.CallSession;
import com.ruralhealthcare.calling.domain.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    Optional<CallSession> findByTwilioCallSid(String twilioCallSid);

    Optional<CallSession> findByUltravoxCallId(String ultravoxCallId);

    List<CallSession> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    Optional<CallSession> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    boolean existsByPhoneNumberAndStatusIn(String phoneNumber, Collection<CallStatus> statuses);

    List<CallSession> findByPhoneNumberAndStatusInOrderByCreatedAtDesc(String phoneNumber, Collection<CallStatus> statuses);

    List<CallSession> findByStatusInOrderByCreatedAtAsc(Collection<CallStatus> statuses);
}
