package com.ruralhealthcare.oauth.controller;

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
                "service", "aarogya-oauth-service",
                "message", "Aarogya Seva Native Google OAuth Service is running"
        ));
    }
}
