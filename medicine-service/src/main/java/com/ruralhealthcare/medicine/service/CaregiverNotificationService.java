package com.ruralhealthcare.medicine.service;

import java.util.UUID;

public interface CaregiverNotificationService {

    /**
     * Escalates to patient's caregiver when consecutive missed doses reach the threshold.
     */
    void notifyCaregiverConsecutiveMissedDoses(UUID patientId, UUID medicationId, int consecutiveMissedCount);

    void notifyCaregiverConsecutiveMissedDoses(UUID patientId, UUID medicationId, String medicationName,
                                               UUID streakStartDoseId, UUID triggeringDoseId, int consecutiveMissedCount);
}
