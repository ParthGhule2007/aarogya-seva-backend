package com.ruralhealthcare.medicine.service;

import com.ruralhealthcare.medicine.dto.DoseEventResponse;

import java.util.List;
import java.util.UUID;

public interface DoseEventService {

    List<DoseEventResponse> getTodayDosesForPatient(UUID patientId);

    List<DoseEventResponse> getDoseHistoryForPatient(UUID patientId);

    DoseEventResponse markDoseTaken(UUID doseEventId);

    DoseEventResponse markDoseMissed(UUID doseEventId);

    DoseEventResponse markDoseSkipped(UUID doseEventId);

    void generateTodayDoseEvents();

    void triggerPendingReminders();

    void checkGracePeriodAndMarkMissed();

    int countConsecutiveMissedDoses(UUID medicationId);
}
