package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.patient.api.dto.CreatePatientRequest;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.api.dto.UpdatePatientRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientService {

    PatientDto registerPatient(CreatePatientRequest request);

    Optional<PatientDto> getPatientById(UUID id);

    Optional<PatientDto> getPatientByContactNumber(String contactNumber);

    PatientDto updatePatient(UUID id, UpdatePatientRequest request);

    List<PatientDto> searchPatientsByName(String nameFragment);

    List<PatientDto> getAllPatients();
}
