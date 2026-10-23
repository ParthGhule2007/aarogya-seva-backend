package com.ruralhealthcare.sos.client;

import com.ruralhealthcare.sos.dto.CaregiverDto;
import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for resolving patient caregiver details from the Patient Service.
 */
public interface PatientServiceClient {

    /**
     * Resolves the primary emergency contact / caregiver for a patient.
     *
     * @param patientId ID of the patient
     * @return Caregiver details if registered, otherwise empty
     */
    Optional<CaregiverDto> getCaregiverForPatient(UUID patientId);
}
