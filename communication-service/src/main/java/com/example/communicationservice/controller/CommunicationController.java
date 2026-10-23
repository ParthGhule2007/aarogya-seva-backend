package com.example.communicationservice.controller;

import com.example.communicationservice.dto.CommunicationSessionResponseDTO;
import com.example.communicationservice.dto.CreateCommunicationSessionDTO;
import com.example.communicationservice.service.CommunicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/communications")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<CommunicationSessionResponseDTO> createSession(
            @Valid @RequestBody CreateCommunicationSessionDTO request) {
        CommunicationSessionResponseDTO response = communicationService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<CommunicationSessionResponseDTO> getSessionById(@PathVariable Long id) {
        CommunicationSessionResponseDTO response = communicationService.getSessionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/consultation/{consultationId}")
    public ResponseEntity<CommunicationSessionResponseDTO> getSessionByConsultationId(
            @PathVariable Long consultationId) {
        CommunicationSessionResponseDTO response = communicationService.getSessionByConsultationId(consultationId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sessions/{id}/start")
    public ResponseEntity<CommunicationSessionResponseDTO> startSession(@PathVariable Long id) {
        CommunicationSessionResponseDTO response = communicationService.startSession(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sessions/{id}/end")
    public ResponseEntity<CommunicationSessionResponseDTO> endSession(@PathVariable Long id) {
        CommunicationSessionResponseDTO response = communicationService.endSession(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sessions/{id}/cancel")
    public ResponseEntity<CommunicationSessionResponseDTO> cancelSession(@PathVariable Long id) {
        CommunicationSessionResponseDTO response = communicationService.cancelSession(id);
        return ResponseEntity.ok(response);
    }
}
