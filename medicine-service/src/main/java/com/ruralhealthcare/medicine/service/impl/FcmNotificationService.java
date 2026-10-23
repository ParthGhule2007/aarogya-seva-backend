package com.ruralhealthcare.medicine.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "medicine.notification.provider", havingValue = "fcm")
public class FcmNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

    private final MedicineProperties properties;
    private FirebaseApp firebaseApp;

    public FcmNotificationService(MedicineProperties properties) {
        this.properties = properties;
        initializeFirebase();
    }

    private synchronized void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            this.firebaseApp = FirebaseApp.getInstance();
            return;
        }

        try {
            File credentialsFile = new File(properties.getFirebase().getCredentialsPath());
            if (!credentialsFile.exists()) {
                File[] candidates = new File[]{
                        new File("firebase-admin.json"),
                        new File("../firebase-admin.json"),
                        new File("medicine-service/firebase-admin.json")
                };
                for (File c : candidates) {
                    if (c.exists()) {
                        credentialsFile = c;
                        break;
                    }
                }
            }

            GoogleCredentials credentials;
            if (credentialsFile.exists()) {
                log.info("Initializing Firebase in Medicine Service using credentials file: {}", credentialsFile.getName());
                try (InputStream is = new FileInputStream(credentialsFile)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                log.warn("No explicit Firebase credentials found, falling back to ApplicationDefaultCredentials");
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(properties.getFirebase().getProjectId())
                    .build();

            this.firebaseApp = FirebaseApp.initializeApp(options, "medicine-firebase-app");
            log.info("Successfully initialized Firebase in Medicine Service");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase in Medicine Service: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendMedicineReminder(UUID patientId, UUID doseEventId, String title, String body) {
        if (firebaseApp == null) {
            log.warn("Firebase not initialized. Cannot send push notification to patient [{}]", patientId);
            return;
        }

        try {
            // Topic-based or token-based dispatch
            String topic = "patient-" + patientId;
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .putData("doseEventId", doseEventId.toString())
                    .putData("patientId", patientId.toString())
                    .putData("type", "MEDICINE_REMINDER")
                    .build();

            String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
            log.info("Successfully dispatched FCM reminder to topic [{}]. Message ID: {}", topic, response);
        } catch (Exception e) {
            log.error("FCM dispatch error to patient [{}]: {}", patientId, e.getMessage());
        }
    }
}
