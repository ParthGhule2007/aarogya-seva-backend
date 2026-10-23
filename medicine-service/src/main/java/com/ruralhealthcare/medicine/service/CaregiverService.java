package com.ruralhealthcare.medicine.service;

import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CaregiverResponse;
import com.ruralhealthcare.medicine.dto.EscalationLogResponse;

import java.util.List;
import java.util.UUID;

public interface CaregiverService {

    CaregiverResponse createCaregiver(CaregiverRequest request);

    List<CaregiverResponse> getCaregiversForPatient(UUID patientId);

    CaregiverResponse updateCaregiver(UUID id, CaregiverRequest request);

    void deactivateCaregiver(UUID id);

    List<EscalationLogResponse> getEscalationHistoryForPatient(UUID patientId);
}
