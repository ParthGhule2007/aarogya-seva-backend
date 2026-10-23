package com.ruralhealthcare.calling.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> rootHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "aarogya-calling-service",
                "message", "Aarogya Seva AI Voice Calling Service is running"
        ));
    }
}
