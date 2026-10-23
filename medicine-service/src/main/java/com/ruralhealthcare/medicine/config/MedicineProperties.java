package com.ruralhealthcare.medicine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "medicine")
public class MedicineProperties {

    private int missedDoseThreshold = 2;
    private int reminderGracePeriodMinutes = 60;
    private Scheduler scheduler = new Scheduler();
    private Storage storage = new Storage();
    private Notification notification = new Notification();
    private Firebase firebase = new Firebase();
    private Ocr ocr = new Ocr();
    private Security security = new Security();

    public int getMissedDoseThreshold() {
        return missedDoseThreshold;
    }

    public void setMissedDoseThreshold(int missedDoseThreshold) {
        this.missedDoseThreshold = missedDoseThreshold;
    }

    public int getReminderGracePeriodMinutes() {
        return reminderGracePeriodMinutes;
    }

    public void setReminderGracePeriodMinutes(int reminderGracePeriodMinutes) {
        this.reminderGracePeriodMinutes = reminderGracePeriodMinutes;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Firebase getFirebase() {
        return firebase;
    }

    public void setFirebase(Firebase firebase) {
        this.firebase = firebase;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public void setOcr(Ocr ocr) {
        this.ocr = ocr;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class Scheduler {
        private boolean enabled = true;
        private long rateMs = 60000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getRateMs() {
            return rateMs;
        }

        public void setRateMs(long rateMs) {
            this.rateMs = rateMs;
        }
    }

    public static class Storage {
        private String provider = "auto"; // 's3', 'local', or 'auto'
        private S3 s3 = new S3();
        private Local local = new Local();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public static class S3 {
            private String bucket = "aarogya-medicine-images";
            private String region = "ap-south-1";
            private String accessKey;
            private String secretKey;
            private int presignedExpiryMinutes = 120;

            public String getBucket() {
                return bucket;
            }

            public void setBucket(String bucket) {
                this.bucket = bucket;
            }

            public String getRegion() {
                return region;
            }

            public void setRegion(String region) {
                this.region = region;
            }

            public String getAccessKey() {
                return accessKey;
            }

            public void setAccessKey(String accessKey) {
                this.accessKey = accessKey;
            }

            public String getSecretKey() {
                return secretKey;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public int getPresignedExpiryMinutes() {
                return presignedExpiryMinutes;
            }

            public void setPresignedExpiryMinutes(int presignedExpiryMinutes) {
                this.presignedExpiryMinutes = presignedExpiryMinutes;
            }
        }

        public static class Local {
            private String directory = "./storage/medicine-images";

            public String getDirectory() {
                return directory;
            }

            public void setDirectory(String directory) {
                this.directory = directory;
            }
        }
    }

    public static class Notification {
        private String provider = "mock";
        private String emailProvider = "mock"; // 'mock' or 'gmail'

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getEmailProvider() {
            return emailProvider;
        }

        public void setEmailProvider(String emailProvider) {
            this.emailProvider = emailProvider;
        }
    }

    public static class Firebase {
        private String credentialsPath = "firebase-admin.json";
        private String projectId = "aarogya-seva-e8f23";

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }
    }

    public static class Ocr {
        private String provider = "mock"; // 'mock' or 'textract'

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class Security {
        private boolean requireGatewayToken = false;
        private String gatewayToken;
        private boolean requireAuthentication = true;

        public boolean isRequireGatewayToken() {
            return requireGatewayToken;
        }

        public void setRequireGatewayToken(boolean requireGatewayToken) {
            this.requireGatewayToken = requireGatewayToken;
        }

        public String getGatewayToken() {
            return gatewayToken;
        }

        public void setGatewayToken(String gatewayToken) {
            this.gatewayToken = gatewayToken;
        }

        public boolean isRequireAuthentication() {
            return requireAuthentication;
        }

        public void setRequireAuthentication(boolean requireAuthentication) {
            this.requireAuthentication = requireAuthentication;
        }
    }
}
