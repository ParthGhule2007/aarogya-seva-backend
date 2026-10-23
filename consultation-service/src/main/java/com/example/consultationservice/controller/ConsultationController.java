package com.example.consultationservice.controller;

import com.example.consultationservice.dto.*;
import com.example.consultationservice.service.ConsultationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private static final Logger log = LoggerFactory.getLogger(ConsultationController.class);
    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    /**
     * 8.1 Create consultation
     * POST /api/consultations
     */
    @PostMapping
    public ResponseEntity<ConsultationResponseDTO> createConsultation(@Valid @RequestBody ConsultationRequestDTO request) {
        log.info("Received request to create consultation for user: {}, doctor: {}", request.getUserId(), request.getDoctorId());
        ConsultationResponseDTO created = consultationService.createConsultation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 9. Find available doctors via Doctor Service
     * GET /api/consultations/available-doctors
     */
    @GetMapping("/available-doctors")
    public ResponseEntity<List<DoctorResponseDTO>> getAvailableDoctors() {
        log.info("Received request to fetch available doctors");
        List<DoctorResponseDTO> doctors = consultationService.getAvailableDoctors();
        return ResponseEntity.ok(doctors);
    }

    /**
     * 10. Get consultation by ID
     * GET /api/consultations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponseDTO> getConsultationById(@PathVariable("id") Long id) {
        log.info("Received request to get consultation ID: {}", id);
        ConsultationResponseDTO consultation = consultationService.getConsultationById(id);
        return ResponseEntity.ok(consultation);
    }

    /**
     * 11. Get user's consultations
     * GET /api/consultations/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConsultationResponseDTO>> getConsultationsByUserId(@PathVariable("userId") String userId) {
        log.info("Received request to get consultations for user: {}", userId);
        List<ConsultationResponseDTO> list = consultationService.getConsultationsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * 12. Get doctor's pending requests
     * GET /api/consultations/doctor/{doctorId}/pending
     */
    @GetMapping("/doctor/{doctorId}/pending")
    public ResponseEntity<List<ConsultationResponseDTO>> getPendingConsultationsByDoctorId(@PathVariable("doctorId") Long doctorId) {
        log.info("Received request to get pending consultations for doctor: {}", doctorId);
        List<ConsultationResponseDTO> list = consultationService.getPendingConsultationsByDoctorId(doctorId);
        return ResponseEntity.ok(list);
    }

    /**
     * Get all consultations for doctor
     * GET /api/consultations/doctor/{doctorId}
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<ConsultationResponseDTO>> getConsultationsByDoctorId(@PathVariable("doctorId") Long doctorId) {
        log.info("Received request to get all consultations for doctor: {}", doctorId);
        List<ConsultationResponseDTO> list = consultationService.getConsultationsByDoctorId(doctorId);
        return ResponseEntity.ok(list);
    }

    /**
     * 13. Doctor accepts consultation
     * PUT /api/consultations/{id}/accept
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<ConsultationResponseDTO> acceptConsultation(@PathVariable("id") Long id) {
        log.info("Received request to accept consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.acceptConsultation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 14. Doctor rejects consultation
     * PUT /api/consultations/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ConsultationResponseDTO> rejectConsultation(@PathVariable("id") Long id) {
        log.info("Received request to reject consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.rejectConsultation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 15. Doctor schedules consultation
     * PUT /api/consultations/{id}/schedule
     */
    @PutMapping("/{id}/schedule")
    public ResponseEntity<ConsultationResponseDTO> scheduleConsultation(
            @PathVariable("id") Long id,
            @Valid @RequestBody ScheduleConsultationDTO scheduleDto) {
        log.info("Received request to schedule consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.scheduleConsultation(id, scheduleDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 16. User cancels consultation
     * PUT /api/consultations/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ConsultationResponseDTO> cancelConsultation(
            @PathVariable("id") Long id,
            @Valid @RequestBody CancelConsultationDTO cancelDto) {
        log.info("Received request to cancel consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.cancelConsultation(id, cancelDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 17. Consultation becomes READY
     * PUT /api/consultations/{id}/ready
     */
    @PutMapping("/{id}/ready")
    public ResponseEntity<ConsultationResponseDTO> markAsReady(@PathVariable("id") Long id) {
        log.info("Received request to mark consultation ID: {} as READY", id);
        ConsultationResponseDTO response = consultationService.markAsReady(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 18. Start consultation (READY -> IN_CALL)
     * PUT /api/consultations/{id}/start
     */
    @PutMapping("/{id}/start")
    public ResponseEntity<ConsultationResponseDTO> startConsultation(@PathVariable("id") Long id) {
        log.info("Received request to start consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.startConsultation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 19. Complete consultation (IN_CALL -> COMPLETED)
     * PUT /api/consultations/{id}/complete
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ConsultationResponseDTO> completeConsultation(@PathVariable("id") Long id) {
        log.info("Received request to complete consultation ID: {}", id);
        ConsultationResponseDTO response = consultationService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }
}
