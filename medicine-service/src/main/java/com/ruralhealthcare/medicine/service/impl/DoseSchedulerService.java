package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.service.DoseEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "medicine.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DoseSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(DoseSchedulerService.class);

    private final DoseEventService doseEventService;

    public DoseSchedulerService(DoseEventService doseEventService) {
        this.doseEventService = doseEventService;
    }

    @Scheduled(fixedRateString = "${medicine.scheduler.rate-ms:60000}")
    public void processDoseSchedules() {
        try {
            // 1. Maintain today's dose events for all active medications
            doseEventService.generateTodayDoseEvents();

            // 2. Trigger notifications for pending doses reaching scheduled time
            doseEventService.triggerPendingReminders();

            // 3. Mark doses missed after configured grace period & evaluate consecutive missed streak
            doseEventService.checkGracePeriodAndMarkMissed();
        } catch (Exception e) {
            log.error("Error in background dose schedule processor: {}", e.getMessage(), e);
        }
    }
}
