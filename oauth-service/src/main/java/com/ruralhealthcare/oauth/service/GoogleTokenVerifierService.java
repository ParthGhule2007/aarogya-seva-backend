package com.ruralhealthcare.oauth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final String googleWebClientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${google.web-client-id:}") String googleWebClientId) {
        this.googleWebClientId = googleWebClientId != null ? googleWebClientId.trim() : "";
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );

        if (StringUtils.hasText(this.googleWebClientId)) {
            builder.setAudience(Collections.singletonList(this.googleWebClientId));
        }

        this.verifier = builder.build();
    }

    public record VerifiedGoogleUser(String sub, String email, String name) {}

    public VerifiedGoogleUser verify(String idTokenString) {
        if (!StringUtils.hasText(idTokenString)) {
            throw new IllegalArgumentException("Google ID token cannot be blank");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString.trim());
        } catch (Exception ex) {
            log.warn("[GoogleTokenVerifierService] Verification failed: {}", ex.getMessage());
            throw new SecurityException("Google ID token signature or format is invalid: " + ex.getMessage(), ex);
        }

        if (idToken == null) {
            log.warn("[GoogleTokenVerifierService] Verifier returned null for provided ID token");
            throw new SecurityException("Invalid or expired Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        if (payload.getEmailVerified() != null && !payload.getEmailVerified()) {
            throw new SecurityException("Google email is not verified");
        }

        String sub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        log.info("[GoogleTokenVerifierService] Successfully verified Google user email [{}] sub [{}]", email, sub);
        return new VerifiedGoogleUser(sub, email, name);
    }
}
