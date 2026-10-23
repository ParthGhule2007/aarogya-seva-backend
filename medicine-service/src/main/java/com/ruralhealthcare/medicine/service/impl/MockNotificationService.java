package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "medicine.notification.provider", havingValue = "mock", matchIfMissing = true)
public class MockNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationService.class);

    @Override
    public void sendMedicineReminder(UUID patientId, UUID doseEventId, String title, String body) {
        log.info("🔔 [MOCK NOTIFICATION] Dispatched reminder to patient [{}], doseEvent [{}]: title='{}', body='{}'",
                patientId, doseEventId, title, body);
    }
}
