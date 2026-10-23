package com.ruralhealthcare.oauth.service;

import com.ruralhealthcare.oauth.dto.GoogleAuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final GoogleTokenVerifierService verifierService;
    private final RestClient restClient;
    private final String internalServiceKey;
    private final String coreBaseUrl;

    public OAuthService(GoogleTokenVerifierService verifierService,
                        RestClient.Builder restClientBuilder,
                        @Value("${core.base-url:http://localhost:8081}") String coreBaseUrl,
                        @Value("${internal.service-key:internal-service-secret-key-rural-healthcare}") String internalServiceKey) {
        this.verifierService = verifierService;
        this.coreBaseUrl = coreBaseUrl;
        this.internalServiceKey = internalServiceKey;
        this.restClient = restClientBuilder.baseUrl(coreBaseUrl).build();
    }

    public Object processGoogleAuth(GoogleAuthRequest request) {
        // Step 1: Cryptographically verify Google ID Token
        GoogleTokenVerifierService.VerifiedGoogleUser verifiedUser = verifierService.verify(request.getIdToken());

        // Step 2: Assemble payload for Core's internal endpoint
        Map<String, Object> internalPayload = new HashMap<>();
        internalPayload.put("email", verifiedUser.email());
        internalPayload.put("fullName", verifiedUser.name());
        internalPayload.put("googleSub", verifiedUser.sub());
        internalPayload.put("role", request.getRole());
        internalPayload.put("phoneNumber", request.getPhoneNumber());
        internalPayload.put("age", request.getAge());
        internalPayload.put("gender", request.getGender());
        internalPayload.put("villageTown", request.getVillageTown());
        internalPayload.put("subDistrict", request.getSubDistrict());
        internalPayload.put("district", request.getDistrict());
        internalPayload.put("state", request.getState());
        internalPayload.put("pincode", request.getPincode());
        internalPayload.put("heightCm", request.getHeightCm());
        internalPayload.put("weightKg", request.getWeightKg());
        internalPayload.put("medicalHistory", request.getMedicalHistory());
        internalPayload.put("specialization", request.getSpecialization());
        internalPayload.put("licenseNumber", request.getLicenseNumber());
        internalPayload.put("facilityName", request.getFacilityName());
        internalPayload.put("facilityType", request.getFacilityType());

        log.info("[OAuthService] Delegating user verification to Core [{}] with X-Internal-Service-Key", coreBaseUrl);

        // Step 3: Delegate to Core
        return restClient.post()
                .uri("/api/v1/auth/internal/google")
                .header("X-Internal-Service-Key", internalServiceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(internalPayload)
                .retrieve()
                .body(Object.class);
    }
}
