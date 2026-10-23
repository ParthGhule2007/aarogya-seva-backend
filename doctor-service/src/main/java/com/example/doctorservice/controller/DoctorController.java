package com.example.doctorservice.controller;

import com.example.doctorservice.dto.*;
import com.example.doctorservice.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    // Constructor injection (no field injection)
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * 1. Create Doctor
     * POST /api/doctors
     */
    @PostMapping
    public ResponseEntity<DoctorResponseDTO> createDoctor(@Valid @RequestBody DoctorRequestDTO request) {
        DoctorResponseDTO createdDoctor = doctorService.createDoctor(request);
        return new ResponseEntity<>(createdDoctor, HttpStatus.CREATED);
    }

    /**
     * List all doctors, sorted with AVAILABLE doctors first.
     * GET /api/doctors
     */
    @GetMapping
    public ResponseEntity<List<DoctorResponseDTO>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    /**
     * 7. Find Available Doctors
     * GET /api/doctors/available
     * Returns only ACTIVE and AVAILABLE doctors without sensitive information.
     */
    @GetMapping("/available")
    public ResponseEntity<List<AvailableDoctorResponseDTO>> getAvailableDoctors() {
        List<AvailableDoctorResponseDTO> availableDoctors = doctorService.findAvailableDoctors();
        return ResponseEntity.ok(availableDoctors);
    }

    /**
     * 2. Get Doctor by Doctor ID
     * GET /api/doctors/{doctorId}
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorResponseDTO> getDoctorById(@PathVariable Long doctorId) {
        DoctorResponseDTO doctor = doctorService.getDoctorById(doctorId);
        return ResponseEntity.ok(doctor);
    }

    /**
     * 3. Get Doctor by User Service User ID
     * GET /api/doctors/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<DoctorResponseDTO> getDoctorByUserId(@PathVariable String userId) {
        DoctorResponseDTO doctor = doctorService.getDoctorByUserId(userId);
        return ResponseEntity.ok(doctor);
    }

    /**
     * 4. Update Doctor Profile
     * PUT /api/doctors/{doctorId}
     */
    @PutMapping("/{doctorId}")
    public ResponseEntity<DoctorResponseDTO> updateDoctor(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorRequestDTO request
    ) {
        DoctorResponseDTO updatedDoctor = doctorService.updateDoctor(doctorId, request);
        return ResponseEntity.ok(updatedDoctor);
    }

    /**
     * 5. Update Doctor Account Status (Soft activation / deactivation)
     * PATCH /api/doctors/{doctorId}/status
     */
    @PatchMapping("/{doctorId}/status")
    public ResponseEntity<DoctorResponseDTO> updateStatus(
            @PathVariable Long doctorId,
            @Valid @RequestBody StatusRequestDTO request
    ) {
        DoctorResponseDTO updatedDoctor = doctorService.updateStatus(doctorId, request);
        return ResponseEntity.ok(updatedDoctor);
    }

    /**
     * 6. Update Doctor Availability
     * PATCH /api/doctors/{doctorId}/availability
     *
     * Coordination rule:
     * If currently BUSY, transition to AVAILABLE must be coordinated by Consultation Service.
     * Coordination can be signaled via header 'X-Service-Source: Consultation-Service' or '?coordinated=true'.
     */
    @PatchMapping("/{doctorId}/availability")
    public ResponseEntity<DoctorResponseDTO> updateAvailability(
            @PathVariable Long doctorId,
            @Valid @RequestBody AvailabilityRequestDTO request,
            @RequestHeader(value = "X-Service-Source", required = false) String serviceSource,
            @RequestParam(value = "coordinated", required = false, defaultValue = "false") boolean coordinatedParam
    ) {
        boolean isCoordinated = coordinatedParam || "Consultation-Service".equalsIgnoreCase(serviceSource);
        DoctorResponseDTO updatedDoctor = doctorService.updateAvailability(doctorId, request, isCoordinated);
        return ResponseEntity.ok(updatedDoctor);
    }
}
