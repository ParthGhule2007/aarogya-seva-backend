package com.ruralhealthcare.sos.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase App configuration for FCM notification delivery.
 * Only loaded when sos.notification.provider=fcm.
 */
@Configuration
@ConditionalOnProperty(name = "sos.notification.provider", havingValue = "fcm")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final SosProperties sosProperties;

    public FirebaseConfig(SosProperties sosProperties) {
        this.sosProperties = sosProperties;
    }

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        SosProperties.Firebase fb = sosProperties.getFirebase();
        try {
            GoogleCredentials credentials = null;

            java.io.File credentialsFile = null;
            if (StringUtils.hasText(fb.getCredentialsPath())) {
                java.io.File f = new java.io.File(fb.getCredentialsPath());
                if (f.exists()) {
                    credentialsFile = f;
                }
            }

            if (credentialsFile == null) {
                java.io.File[] candidates = new java.io.File[] {
                    new java.io.File("firebase-admin.json"),
                    new java.io.File("sos-service/firebase-admin.json"),
                    new java.io.File("../firebase-admin.json")
                };
                for (java.io.File c : candidates) {
                    if (c.exists()) {
                        credentialsFile = c;
                        break;
                    }
                }
            }

            if (credentialsFile != null) {
                log.info("Initializing Firebase Admin SDK using credentials file: {}", credentialsFile.getName());
                try (InputStream serviceAccount = new FileInputStream(credentialsFile)) {
                    credentials = GoogleCredentials.fromStream(serviceAccount);
                }
            } else if (StringUtils.hasText(fb.getProjectId()) &&
                       StringUtils.hasText(fb.getClientEmail()) &&
                       StringUtils.hasText(fb.getPrivateKey())) {
                log.info("Initializing Firebase Admin SDK using environment credentials for project: {}", fb.getProjectId());
                
                // Format private key appropriately (replacing escaped newlines if any)
                String normalizedKey = fb.getPrivateKey().replace("\\n", "\n");

                String jsonCredential = String.format(
                        "{\n" +
                        "  \"type\": \"service_account\",\n" +
                        "  \"project_id\": \"%s\",\n" +
                        "  \"client_email\": \"%s\",\n" +
                        "  \"private_key\": \"%s\"\n" +
                        "}",
                        fb.getProjectId(),
                        fb.getClientEmail(),
                        normalizedKey.replace("\"", "\\\"").replace("\n", "\\n")
                );

                try (InputStream stream = new ByteArrayInputStream(jsonCredential.getBytes(StandardCharsets.UTF_8))) {
                    credentials = GoogleCredentials.fromStream(stream);
                }
            } else {
                log.warn("No explicit Firebase credentials provided. Falling back to Application Default Credentials.");
                credentials = GoogleCredentials.getApplicationDefault();
            }

            String effectiveProjectId = StringUtils.hasText(fb.getProjectId())
                    ? fb.getProjectId()
                    : (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials
                        ? ((com.google.auth.oauth2.ServiceAccountCredentials) credentials).getProjectId()
                        : "aarogya-seva-e8f23");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(effectiveProjectId)
                    .build();

            return FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp with provided configuration: {}", e.getMessage(), e);
            throw new IllegalStateException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }
}
