package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.common.exception.ValidationException;
import com.ruralhealthcare.patient.api.dto.CreatePatientRequest;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.api.dto.UpdatePatientRequest;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.patient.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HealthcareCaseRepository caseRepository;

    @Mock
    private com.ruralhealthcare.auth.repository.UserRepository userRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient testPatient;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        testPatient = Patient.builder()
                .id(patientId)
                .fullName("Ramesh Kumar")
                .gender(Gender.MALE)
                .age(42)
                .contactNumber("+91-9876543210")
                .villageTown("Rampur")
                .subDistrict("Mohanlalganj")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .preferredLanguage("Hindi")
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new patient")
    void testRegisterPatientSuccess() {
        CreatePatientRequest request = CreatePatientRequest.builder()
                .fullName("Ramesh Kumar")
                .gender(Gender.MALE)
                .age(42)
                .contactNumber("+91-9876543210")
                .villageTown("Rampur")
                .subDistrict("Mohanlalganj")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .preferredLanguage("Hindi")
                .build();

        when(patientRepository.existsByContactNumber("+91-9876543210")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        PatientDto result = patientService.registerPatient(request);

        assertNotNull(result);
        assertEquals("Ramesh Kumar", result.getFullName());
        assertEquals("+91-9876543210", result.getContactNumber());
        assertEquals("Rampur", result.getVillageTown());
    }

    @Test
    @DisplayName("Should reject patient registration if contact number already registered")
    void testRegisterPatientDuplicatePhone() {
        CreatePatientRequest request = CreatePatientRequest.builder()
                .fullName("Duplicate User")
                .gender(Gender.FEMALE)
                .contactNumber("+91-9876543210")
                .villageTown("Rampur")
                .subDistrict("Mohanlalganj")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .pincode("226301")
                .build();

        when(patientRepository.existsByContactNumber("+91-9876543210")).thenReturn(true);

        assertThrows(ValidationException.class, () -> patientService.registerPatient(request));
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve patient profile by ID")
    void testGetPatientById() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));

        Optional<PatientDto> result = patientService.getPatientById(patientId);

        assertTrue(result.isPresent());
        assertEquals("Ramesh Kumar", result.get().getFullName());
    }

    @Test
    @DisplayName("Should update patient profile successfully")
    void testUpdatePatient() {
        UpdatePatientRequest request = UpdatePatientRequest.builder()
                .villageTown("New Rampur Colony")
                .age(43)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientDto updated = patientService.updatePatient(patientId, request);

        assertNotNull(updated);
        assertEquals("New Rampur Colony", updated.getVillageTown());
        assertEquals(43, updated.getAge());
    }

    @Test
    @DisplayName("Should search patients by name fragment")
    void testSearchPatientsByName() {
        when(patientRepository.findByFullNameContainingIgnoreCase("ramesh"))
                .thenReturn(List.of(testPatient));

        List<PatientDto> results = patientService.searchPatientsByName("ramesh");

        assertEquals(1, results.size());
        assertEquals("Ramesh Kumar", results.get(0).getFullName());
    }
}
