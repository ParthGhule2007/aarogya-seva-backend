package com.ruralhealthcare.sos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for the SOS Emergency Alert microservice.
 */
@Configuration
@ConfigurationProperties(prefix = "sos")
public class SosProperties {

    /**
     * Fixed server-side radius for nearby helper discovery in kilometers.
     * Clients can NEVER modify this value.
     */
    private double nearbyRadiusKm = 5.0;

    /**
     * Accidental duplicate SOS deduplication window in minutes.
     */
    private int deduplicationWindowMinutes = 5;

    private int deduplicationWindowSeconds = 300;

    private int maxMessageLength = 500;

    private final Clients clients = new Clients();
    private final Notification notification = new Notification();
    private final Firebase firebase = new Firebase();

    public double getNearbyRadiusKm() {
        return nearbyRadiusKm;
    }

    public void setNearbyRadiusKm(double nearbyRadiusKm) {
        this.nearbyRadiusKm = nearbyRadiusKm;
    }

    public int getDeduplicationWindowMinutes() {
        return deduplicationWindowMinutes;
    }

    public void setDeduplicationWindowMinutes(int deduplicationWindowMinutes) {
        this.deduplicationWindowMinutes = deduplicationWindowMinutes;
        this.deduplicationWindowSeconds = deduplicationWindowMinutes * 60;
    }

    public int getDeduplicationWindowSeconds() {
        return deduplicationWindowSeconds;
    }

    public void setDeduplicationWindowSeconds(int deduplicationWindowSeconds) {
        this.deduplicationWindowSeconds = deduplicationWindowSeconds;
        this.deduplicationWindowMinutes = Math.max(1, deduplicationWindowSeconds / 60);
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public Clients getClients() {
        return clients;
    }

    public Notification getNotification() {
        return notification;
    }

    public Firebase getFirebase() {
        return firebase;
    }

    public static class ServiceEndpoint {
        private String baseUrl = "http://localhost:8081";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;

        public ServiceEndpoint() {
        }

        public ServiceEndpoint(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Clients {
        private String mode = "mock";
        private ServiceEndpoint patientService = new ServiceEndpoint("http://localhost:8081");
        private ServiceEndpoint facilityService = new ServiceEndpoint("http://localhost:8081");
        private ServiceEndpoint userLocationService = new ServiceEndpoint("http://localhost:8081");

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public ServiceEndpoint getPatientService() {
            return patientService;
        }

        public void setPatientService(ServiceEndpoint patientService) {
            this.patientService = patientService;
        }

        public String getPatientServiceUrl() {
            return patientService != null ? patientService.getBaseUrl() : "http://localhost:8081";
        }

        public void setPatientServiceUrl(String patientServiceUrl) {
            if (this.patientService == null) {
                this.patientService = new ServiceEndpoint(patientServiceUrl);
            } else {
                this.patientService.setBaseUrl(patientServiceUrl);
            }
        }

        public ServiceEndpoint getFacilityService() {
            return facilityService;
        }

        public void setFacilityService(ServiceEndpoint facilityService) {
            this.facilityService = facilityService;
        }

        public String getFacilityServiceUrl() {
            return facilityService != null ? facilityService.getBaseUrl() : "http://localhost:8081";
        }

        public void setFacilityServiceUrl(String facilityServiceUrl) {
            if (this.facilityService == null) {
                this.facilityService = new ServiceEndpoint(facilityServiceUrl);
            } else {
                this.facilityService.setBaseUrl(facilityServiceUrl);
            }
        }

        public ServiceEndpoint getUserLocationService() {
            return userLocationService;
        }

        public void setUserLocationService(ServiceEndpoint userLocationService) {
            this.userLocationService = userLocationService;
        }

        public ServiceEndpoint getLocationService() {
            return userLocationService;
        }

        public void setLocationService(ServiceEndpoint locationService) {
            this.userLocationService = locationService;
        }

        public String getLocationServiceUrl() {
            return userLocationService != null ? userLocationService.getBaseUrl() : "http://localhost:8081";
        }

        public void setLocationServiceUrl(String locationServiceUrl) {
            if (this.userLocationService == null) {
                this.userLocationService = new ServiceEndpoint(locationServiceUrl);
            } else {
                this.userLocationService.setBaseUrl(locationServiceUrl);
            }
        }
    }

    public static class Notification {
        private String provider = "mock";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class Firebase {
        private String projectId;
        private String clientEmail;
        private String privateKey;
        private String credentialsPath;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getClientEmail() {
            return clientEmail;
        }

        public void setClientEmail(String clientEmail) {
            this.clientEmail = clientEmail;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }
    }
}
