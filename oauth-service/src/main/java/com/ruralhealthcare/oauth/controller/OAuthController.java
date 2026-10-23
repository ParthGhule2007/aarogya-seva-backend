package com.ruralhealthcare.oauth.controller;

import com.ruralhealthcare.oauth.dto.GoogleAuthRequest;
import com.ruralhealthcare.oauth.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "OAuth Service", description = "Native Google OAuth2 authentication & registration endpoints")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate or register via Google ID token",
            description = "Validates the Google ID token and issues user JWT via Core service delegation.")
    public ResponseEntity<?> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        Object response = oAuthService.processGoogleAuth(request);
        return ResponseEntity.ok(response);
    }
}
