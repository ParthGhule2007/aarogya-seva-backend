package com.ruralhealthcare.auth.api;

import com.ruralhealthcare.auth.dto.AuthResponse;
import com.ruralhealthcare.auth.dto.LoginRequest;
import com.ruralhealthcare.auth.dto.RegisterRequest;
import com.ruralhealthcare.auth.dto.UserDto;
import com.ruralhealthcare.auth.service.AuthService;
import com.ruralhealthcare.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and session verification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user",
            description = "Authenticates using username, phone number, or email and returns a signed JWT token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account deactivated")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Registers a new user (PATIENT, CAREGIVER, ASHA, etc.) and returns an active JWT token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User successfully registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username, phone, or email already registered")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile",
            description = "Returns current session profile from the Authorization Bearer JWT token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized caller")
    })
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", user));
    }

    @PutMapping("/link-entity")
    @Operation(summary = "Link domain entity to current user account",
            description = "Links patientId, doctorId, or facilityId to the authenticated user and reissues a JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> linkEntity(@Valid @RequestBody com.ruralhealthcare.auth.dto.LinkEntityRequest request) {
        AuthResponse response = authService.linkEntity(request);
        return ResponseEntity.ok(ApiResponse.ok("Entity linked successfully", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate or register via Google ID token",
            description = "Verifies Google OAuth2 ID token, provisions or links user, and returns signed JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateWithGoogle(@Valid @RequestBody com.ruralhealthcare.auth.dto.GoogleAuthRequest request) {
        AuthResponse response = authService.authenticateWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.ok("Google authentication successful", response));
    }

    @org.springframework.beans.factory.annotation.Value("${internal.service-key:internal-service-secret-key-rural-healthcare}")
    private String internalServiceKey;

    @PostMapping("/internal/google")
    @Operation(summary = "Internal service endpoint for Google OAuth delegation",
            description = "Protected internal endpoint called by oauth-service to lookup or provision users and issue JWTs.")
    public ResponseEntity<?> internalGoogleAuth(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody com.ruralhealthcare.auth.dto.InternalGoogleAuthRequest request) {
        if (!org.springframework.util.StringUtils.hasText(serviceKey) || !serviceKey.equals(internalServiceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Invalid internal service key", "code", "AUTH_FORBIDDEN"));
        }
        Object result = authService.processInternalGoogleAuth(request);
        return ResponseEntity.ok(result);
    }
}
