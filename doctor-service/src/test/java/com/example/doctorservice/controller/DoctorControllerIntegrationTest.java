package com.example.doctorservice.controller;

import com.example.doctorservice.dto.*;
import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.enums.DoctorAvailabilityStatus;
import com.example.doctorservice.enums.DoctorStatus;
import com.example.doctorservice.repository.DoctorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
    }

    private DoctorRequestDTO createSampleRequest(String userId, String licenseNumber) {
        return DoctorRequestDTO.builder()
                .userId(userId)
                .name("Dr. Sharma")
                .specialization("General Physician")
                .licenseNumber(licenseNumber)
                .experienceYears(5)
                .qualification("MBBS, MD")
                .hospitalName("ABC Hospital")
                .build();
    }

    @Test
    @DisplayName("Scenario 1: Create doctor returns 201 Created and default status ACTIVE, OFFLINE")
    void testCreateDoctor_Success() throws Exception {
        DoctorRequestDTO request = createSampleRequest("102", "MED12345");

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value("102"))
                .andExpect(jsonPath("$.name").value("Dr. Sharma"))
                .andExpect(jsonPath("$.specialization").value("General Physician"))
                .andExpect(jsonPath("$.licenseNumber").value("MED12345"))
                .andExpect(jsonPath("$.experienceYears").value(5))
                .andExpect(jsonPath("$.qualification").value("MBBS, MD"))
                .andExpect(jsonPath("$.hospitalName").value("ABC Hospital"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availabilityStatus").value("OFFLINE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Scenario 2: Get doctor by doctorId returns 200 OK")
    void testGetDoctorById_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("105")
                .name("Dr. Aditi Patel")
                .specialization("Cardiologist")
                .licenseNumber("CARDIO889")
                .experienceYears(10)
                .qualification("MD, DM")
                .hospitalName("Heart Care Center")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        mockMvc.perform(get("/api/doctors/{doctorId}", doctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctor.getId()))
                .andExpect(jsonPath("$.userId").value("105"))
                .andExpect(jsonPath("$.name").value("Dr. Aditi Patel"))
                .andExpect(jsonPath("$.specialization").value("Cardiologist"))
                .andExpect(jsonPath("$.licenseNumber").value("CARDIO889"));
    }

    @Test
    @DisplayName("Scenario 3: Get doctor by userId returns 200 OK (String / UUID support)")
    void testGetDoctorByUserId_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("d1000000-0000-0000-0000-000000000001")
                .name("Dr. Rajesh Verma")
                .specialization("Neurologist")
                .licenseNumber("NEURO771")
                .experienceYears(12)
                .qualification("MBBS, MS")
                .hospitalName("Apollo Hospital")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        mockMvc.perform(get("/api/doctors/user/{userId}", "d1000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctor.getId()))
                .andExpect(jsonPath("$.userId").value("d1000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.name").value("Dr. Rajesh Verma"));
    }

    @Test
    @DisplayName("Scenario 4: Update doctor returns 200 OK")
    void testUpdateDoctor_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("300")
                .name("Dr. Old Name")
                .specialization("General Physician")
                .licenseNumber("LIC111")
                .experienceYears(3)
                .qualification("MBBS")
                .hospitalName("Old Hospital")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        DoctorRequestDTO updateRequest = DoctorRequestDTO.builder()
                .userId("300")
                .name("Dr. Updated Name")
                .specialization("Internal Medicine")
                .licenseNumber("LIC111-UPDATED")
                .experienceYears(4)
                .qualification("MBBS, MD")
                .hospitalName("New Hospital")
                .build();

        mockMvc.perform(put("/api/doctors/{doctorId}", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Updated Name"))
                .andExpect(jsonPath("$.specialization").value("Internal Medicine"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC111-UPDATED"))
                .andExpect(jsonPath("$.experienceYears").value(4))
                .andExpect(jsonPath("$.hospitalName").value("New Hospital"));
    }

    @Test
    @DisplayName("Scenario 5: Set doctor AVAILABLE returns 200 OK")
    void testSetDoctorAvailable_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("400")
                .name("Dr. Meera")
                .specialization("Dermatologist")
                .licenseNumber("DERMA400")
                .experienceYears(6)
                .qualification("MBBS, DVD")
                .hospitalName("Skin Clinic")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Scenario 6: Set doctor OFFLINE returns 200 OK")
    void testSetDoctorOffline_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("500")
                .name("Dr. Sneha")
                .specialization("Pediatrician")
                .licenseNumber("PED500")
                .experienceYears(8)
                .qualification("MBBS, DCH")
                .hospitalName("Child Care")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build());

        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build();

        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("OFFLINE"));
    }

    @Test
    @DisplayName("Scenario 7: Set doctor BUSY returns 200 OK")
    void testSetDoctorBusy_Success() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("600")
                .name("Dr. Rohan")
                .specialization("Orthopedic")
                .licenseNumber("ORTHO600")
                .experienceYears(11)
                .qualification("MS Ortho")
                .hospitalName("Bone Hospital")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build());

        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .availabilityStatus(DoctorAvailabilityStatus.BUSY)
                .build();

        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("BUSY"));
    }

    @Test
    @DisplayName("Scenario 8: Find available doctors returns only ACTIVE and AVAILABLE doctors")
    void testGetAvailableDoctors_OnlyActiveAndAvailable() throws Exception {
        // Active and Available (Should appear)
        doctorRepository.save(Doctor.builder()
                .userId("1001")
                .name("Dr. Available Active")
                .specialization("General Physician")
                .licenseNumber("DOC-AA-1")
                .experienceYears(5)
                .qualification("MBBS")
                .hospitalName("City Clinic")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build());

        // Active but OFFLINE (Should NOT appear)
        doctorRepository.save(Doctor.builder()
                .userId("1002")
                .name("Dr. Active Offline")
                .specialization("General Physician")
                .licenseNumber("DOC-AO-2")
                .experienceYears(4)
                .qualification("MBBS")
                .hospitalName("City Clinic")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        // Inactive and Available (Should NOT appear)
        doctorRepository.save(Doctor.builder()
                .userId("1003")
                .name("Dr. Inactive Available")
                .specialization("General Physician")
                .licenseNumber("DOC-IA-3")
                .experienceYears(4)
                .qualification("MBBS")
                .hospitalName("City Clinic")
                .status(DoctorStatus.INACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build());

        mockMvc.perform(get("/api/doctors/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Dr. Available Active"))
                .andExpect(jsonPath("$[0].availabilityStatus").value("AVAILABLE"))
                // Ensure no sensitive authentication fields are present
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].phoneNumber").doesNotExist());
    }

    @Test
    @DisplayName("Scenario 9: Try to make an INACTIVE doctor AVAILABLE returns 400 Bad Request")
    void testInactiveDoctorCannotBecomeAvailable() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("700")
                .name("Dr. Disabled")
                .specialization("Dentist")
                .licenseNumber("DENT700")
                .experienceYears(7)
                .qualification("BDS")
                .hospitalName("Smile Clinic")
                .status(DoctorStatus.INACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_AVAILABILITY_STATUS"))
                .andExpect(jsonPath("$.message", containsString("Cannot set availability to AVAILABLE: Doctor account is INACTIVE")));
    }

    @Test
    @DisplayName("Scenario 10: Request a non-existent doctor returns 404 Not Found")
    void testGetNonExistentDoctor() throws Exception {
        mockMvc.perform(get("/api/doctors/{doctorId}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("DOCTOR_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Doctor with id 99999 not found"))
                .andExpect(jsonPath("$.path").value("/api/doctors/99999"));
    }

    @Test
    @DisplayName("Scenario 11: Try duplicate license number returns 409 Conflict")
    void testDuplicateLicenseNumber() throws Exception {
        doctorRepository.save(Doctor.builder()
                .userId("801")
                .name("Dr. First")
                .specialization("ENT")
                .licenseNumber("DUP-LIC-99")
                .experienceYears(5)
                .qualification("MBBS")
                .hospitalName("ENT Care")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.OFFLINE)
                .build());

        DoctorRequestDTO duplicateRequest = createSampleRequest("802", "DUP-LIC-99");

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DOCTOR_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message", containsString("DUP-LIC-99")));
    }

    @Test
    @DisplayName("Rule 2: Arbitrary transition from BUSY to AVAILABLE is rejected without coordination")
    void testBusyDoctorCannotArbitrarilyBecomeAvailable() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("900")
                .name("Dr. Busy Doctor")
                .specialization("Cardiologist")
                .licenseNumber("BUSY-900")
                .experienceYears(10)
                .qualification("MD")
                .hospitalName("City Hospital")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.BUSY)
                .build());

        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build();

        // Without coordination -> 400 Bad Request
        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AVAILABILITY_STATUS"))
                .andExpect(jsonPath("$.message", containsString("Consultation Service must coordinate this transition")));

        // With Consultation Service coordination header -> 200 OK
        mockMvc.perform(patch("/api/doctors/{doctorId}/availability", doctor.getId())
                        .header("X-Service-Source", "Consultation-Service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Status Update: Deactivating doctor account resets availability to OFFLINE")
    void testDeactivateDoctorResetsAvailabilityToOffline() throws Exception {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .userId("950")
                .name("Dr. To Deactivate")
                .specialization("General Physician")
                .licenseNumber("DEACT-950")
                .experienceYears(4)
                .qualification("MBBS")
                .hospitalName("City Clinic")
                .status(DoctorStatus.ACTIVE)
                .availabilityStatus(DoctorAvailabilityStatus.AVAILABLE)
                .build());

        StatusRequestDTO statusRequest = StatusRequestDTO.builder()
                .status(DoctorStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/doctors/{doctorId}/status", doctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.availabilityStatus").value("OFFLINE"));
    }

    @Test
    @DisplayName("Validation: Missing required fields returns 400 Bad Request with field errors")
    void testValidationFailure() throws Exception {
        DoctorRequestDTO invalidRequest = DoctorRequestDTO.builder()
                .userId(null) // missing
                .name("") // blank
                .specialization("") // blank
                .licenseNumber("") // blank
                .experienceYears(-1) // negative
                .qualification("")
                .hospitalName("")
                .build();

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.experienceYears").exists());
    }
}
