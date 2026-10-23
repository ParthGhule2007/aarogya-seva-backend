package com.example.consultationservice.client;

import com.example.consultationservice.config.FeignConfig;
import com.example.consultationservice.dto.CommunicationSessionResponseDTO;
import com.example.consultationservice.dto.CreateCommunicationSessionRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "communication-service",
        url = "${communication-service.url:http://localhost:8084}",
        configuration = FeignConfig.class
)
public interface CommunicationServiceClient {

    @PostMapping("/api/communications/sessions")
    CommunicationSessionResponseDTO createSession(@RequestBody CreateCommunicationSessionRequestDTO request);

    @GetMapping("/api/communications/sessions/consultation/{consultationId}")
    CommunicationSessionResponseDTO getSessionByConsultationId(@PathVariable("consultationId") Long consultationId);

    @PutMapping("/api/communications/sessions/{id}/start")
    CommunicationSessionResponseDTO startSession(@PathVariable("id") Long id);

    @PutMapping("/api/communications/sessions/{id}/end")
    CommunicationSessionResponseDTO endSession(@PathVariable("id") Long id);

    @PutMapping("/api/communications/sessions/{id}/cancel")
    CommunicationSessionResponseDTO cancelSession(@PathVariable("id") Long id);
}
