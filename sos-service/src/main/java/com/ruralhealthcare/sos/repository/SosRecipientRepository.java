package com.ruralhealthcare.sos.repository;

import com.ruralhealthcare.sos.domain.SosRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SosRecipientRepository extends JpaRepository<SosRecipient, UUID> {

    List<SosRecipient> findBySosEventId(UUID sosEventId);

    List<SosRecipient> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}
