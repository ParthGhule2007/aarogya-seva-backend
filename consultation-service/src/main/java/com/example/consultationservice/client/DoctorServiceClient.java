package com.example.consultationservice.client;

import com.example.consultationservice.config.FeignConfig;
import com.example.consultationservice.dto.DoctorAvailabilityUpdateDTO;
import com.example.consultationservice.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "doctor-service", url = "${doctor-service.url:http://localhost:8086}", configuration = FeignConfig.class)
public interface DoctorServiceClient {

    @GetMapping("/api/doctors/available")
    List<DoctorResponseDTO> getAvailableDoctors();

    @GetMapping("/api/doctors/{doctorId}")
    DoctorResponseDTO getDoctorById(@PathVariable("doctorId") Long doctorId);

    @PatchMapping(value = "/api/doctors/{doctorId}/availability", consumes = "application/json")
    ResponseEntity<Void> updateDoctorAvailability(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody DoctorAvailabilityUpdateDTO request,
            @RequestHeader(value = "X-Service-Source", defaultValue = "Consultation-Service") String serviceSource
    );
}
