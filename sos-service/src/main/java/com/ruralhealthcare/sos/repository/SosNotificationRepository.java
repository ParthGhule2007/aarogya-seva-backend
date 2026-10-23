package com.ruralhealthcare.sos.repository;

import com.ruralhealthcare.sos.domain.SosNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SosNotificationRepository extends JpaRepository<SosNotification, UUID> {

    List<SosNotification> findBySosEventId(UUID sosEventId);
}
