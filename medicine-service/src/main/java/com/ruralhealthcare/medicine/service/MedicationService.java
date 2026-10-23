package com.ruralhealthcare.medicine.service;

import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.dto.UpdateMedicationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MedicationService {

    MedicationResponse createMedication(CreateMedicationRequest request, MultipartFile image);

    MedicationResponse getMedicationById(UUID id);

    List<MedicationResponse> getMedicationsByPatientId(UUID patientId);

    MedicationResponse updateMedication(UUID id, UpdateMedicationRequest request);

    MedicationResponse completeMedication(UUID id);

    MedicationResponse discontinueMedication(UUID id);

    void deactivateMedication(UUID id);
}
