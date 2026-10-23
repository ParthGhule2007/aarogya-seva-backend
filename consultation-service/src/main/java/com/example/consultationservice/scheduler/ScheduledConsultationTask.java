package com.example.consultationservice.scheduler;

import com.example.consultationservice.entity.Consultation;
import com.example.consultationservice.enums.ConsultationStatus;
import com.example.consultationservice.repository.ConsultationRepository;
import com.example.consultationservice.service.ConsultationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledConsultationTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledConsultationTask.class);
    private final ConsultationRepository consultationRepository;
    private final ConsultationService consultationService;

    public ScheduledConsultationTask(ConsultationRepository consultationRepository,
                                     ConsultationService consultationService) {
        this.consultationRepository = consultationRepository;
        this.consultationService = consultationService;
    }

    @Scheduled(fixedDelayString = "${consultation.scheduler.fixed-delay-ms:30000}")
    public void processScheduledConsultations() {
        LocalDateTime now = LocalDateTime.now();
        List<Consultation> dueConsultations = consultationRepository
                .findByStatusAndScheduledAtLessThanEqual(ConsultationStatus.SCHEDULED, now);

        if (!dueConsultations.isEmpty()) {
            log.info("Found {} scheduled consultations due for transition to READY at {}",
                    dueConsultations.size(), now);
            for (Consultation consultation : dueConsultations) {
                try {
                    consultationService.markAsReady(consultation.getId());
                    log.info("Transitioned scheduled consultation {} to READY", consultation.getId());
                } catch (Exception e) {
                    log.error("Failed to transition scheduled consultation {} to READY: {}",
                            consultation.getId(), e.getMessage());
                }
            }
        }
    }
}
