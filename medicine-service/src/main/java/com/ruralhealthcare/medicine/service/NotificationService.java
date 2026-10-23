package com.ruralhealthcare.medicine.service;

import java.util.UUID;

public interface NotificationService {

    /**
     * Sends a push notification reminder to the patient's device.
     */
    void sendMedicineReminder(UUID patientId, UUID doseEventId, String title, String body);
}
