package com.ruralhealthcare.calling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "calling")
public class CallingProperties {

    private String publicBaseUrl;
    private TwilioProperties twilio = new TwilioProperties();
    private UltravoxProperties ultravox = new UltravoxProperties();
    private HospitalSearchProperties hospitalSearch = new HospitalSearchProperties();

    public String getPublicBaseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl;
        }
        if (twilio.getWebhookBaseUrl() != null && !twilio.getWebhookBaseUrl().isBlank()) {
            return twilio.getWebhookBaseUrl();
        }
        return "http://localhost:8089";
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public TwilioProperties getTwilio() {
        return twilio;
    }

    public void setTwilio(TwilioProperties twilio) {
        this.twilio = twilio;
    }

    public UltravoxProperties getUltravox() {
        return ultravox;
    }

    public void setUltravox(UltravoxProperties ultravox) {
        this.ultravox = ultravox;
    }

    public HospitalSearchProperties getHospitalSearch() {
        return hospitalSearch;
    }

    public void setHospitalSearch(HospitalSearchProperties hospitalSearch) {
        this.hospitalSearch = hospitalSearch;
    }

    public static class TwilioProperties {
        private String accountSid = "AC_mock_twilio_sid";
        private String authToken = "mock_twilio_auth_token";
        private String phoneNumber = "+15005550006";
        private String webhookBaseUrl = "http://localhost:8089";
        private java.util.List<String> verifiedRecipients = new java.util.ArrayList<>();
        private boolean mockMode = true;

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getWebhookBaseUrl() {
            return webhookBaseUrl;
        }

        public void setWebhookBaseUrl(String webhookBaseUrl) {
            this.webhookBaseUrl = webhookBaseUrl;
        }

        public java.util.List<String> getVerifiedRecipients() {
            return verifiedRecipients;
        }

        public void setVerifiedRecipients(java.util.List<String> verifiedRecipients) {
            this.verifiedRecipients = verifiedRecipients;
        }

        public boolean isMockMode() {
            return mockMode;
        }

        public void setMockMode(boolean mockMode) {
            this.mockMode = mockMode;
        }
    }

    public static class UltravoxProperties {
        private String apiKey = "mock_ultravox_api_key";
        private String apiUrl = "https://api.ultravox.ai/api";
        private String model = "ultravox-v0.7";
        private double temperature = 0.3;
        public static final String DEFAULT_ENGLISH_VOICE = "73527457-256b-4291-a36e-10128315db4b";
        public static final String DEFAULT_HINDI_VOICE = "f90da51d-8133-4d19-aa0f-4ec99e14cb85";
        public static final String DEFAULT_MARATHI_VOICE = "26b36187-9e6c-481c-90e3-a991baac0496";

        private Map<String, String> voice = new HashMap<>(Map.of(
                "en", DEFAULT_ENGLISH_VOICE,
                "hi", DEFAULT_HINDI_VOICE,
                "mr", DEFAULT_MARATHI_VOICE
        ));
        private boolean mockMode = true;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Map<String, String> getVoice() {
            return voice;
        }

        public void setVoice(Map<String, String> voice) {
            this.voice = voice;
        }

        public String getVoiceForLanguage(String langCode) {
            if (langCode != null && voice != null) {
                String candidate = voice.get(langCode.trim().toLowerCase());
                if (candidate != null && !candidate.isBlank()) {
                    return candidate.trim();
                }
            }
            if (voice != null) {
                String enVoice = voice.get("en");
                if (enVoice != null && !enVoice.isBlank()) {
                    return enVoice.trim();
                }
            }
            return DEFAULT_ENGLISH_VOICE;
        }

        public boolean isVoiceConfiguredForLanguage(String langCode) {
            if (langCode == null || voice == null) {
                return false;
            }
            String candidate = voice.get(langCode.trim().toLowerCase());
            return candidate != null && !candidate.isBlank();
        }

        public boolean isMockMode() {
            return mockMode;
        }

        public void setMockMode(boolean mockMode) {
            this.mockMode = mockMode;
        }

        private VadProperties vad = new VadProperties();

        public VadProperties getVad() {
            return vad;
        }

        public void setVad(VadProperties vad) {
            this.vad = vad;
        }

        public static class VadProperties {
            private String turnEndpointDelay = "0.384s";
            private String minimumTurnDuration = "0.064s";
            private String minimumInterruptionDuration = "0.096s";
            private double frameActivationThreshold = 0.3;

            public Map<String, Object> toMap() {
                Map<String, Object> map = new HashMap<>();
                map.put("turnEndpointDelay", turnEndpointDelay);
                map.put("minimumTurnDuration", minimumTurnDuration);
                map.put("minimumInterruptionDuration", minimumInterruptionDuration);
                map.put("frameActivationThreshold", frameActivationThreshold);
                return map;
            }

            public String getTurnEndpointDelay() {
                return turnEndpointDelay;
            }

            public void setTurnEndpointDelay(String turnEndpointDelay) {
                this.turnEndpointDelay = turnEndpointDelay;
            }

            public String getMinimumTurnDuration() {
                return minimumTurnDuration;
            }

            public void setMinimumTurnDuration(String minimumTurnDuration) {
                this.minimumTurnDuration = minimumTurnDuration;
            }

            public String getMinimumInterruptionDuration() {
                return minimumInterruptionDuration;
            }

            public void setMinimumInterruptionDuration(String minimumInterruptionDuration) {
                this.minimumInterruptionDuration = minimumInterruptionDuration;
            }

            public double getFrameActivationThreshold() {
                return frameActivationThreshold;
            }

            public void setFrameActivationThreshold(double frameActivationThreshold) {
                this.frameActivationThreshold = frameActivationThreshold;
            }
        }
    }

    public static class HospitalSearchProperties {
        private String provider = "geoapify";
        private String nominatimUrl = "https://nominatim.openstreetmap.org";
        private String overpassUrl = "https://overpass-api.de/api/interpreter";
        private String overpassFallbackUrl = "https://lz4.overpass-api.de/api/interpreter";
        private double searchRadiusKm = 25.0;
        private double fallbackRadiusKm = 10.0;
        private int maxResults = 5;
        private int timeoutMs = 10000;
        private GeoapifyProperties geoapify = new GeoapifyProperties();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public GeoapifyProperties getGeoapify() {
            return geoapify;
        }

        public void setGeoapify(GeoapifyProperties geoapify) {
            this.geoapify = geoapify;
        }

        public String getNominatimUrl() {
            return nominatimUrl;
        }

        public void setNominatimUrl(String nominatimUrl) {
            this.nominatimUrl = nominatimUrl;
        }

        public String getOverpassUrl() {
            return overpassUrl;
        }

        public void setOverpassUrl(String overpassUrl) {
            this.overpassUrl = overpassUrl;
        }

        public String getOverpassFallbackUrl() {
            return overpassFallbackUrl;
        }

        public void setOverpassFallbackUrl(String overpassFallbackUrl) {
            this.overpassFallbackUrl = overpassFallbackUrl;
        }

        public double getSearchRadiusKm() {
            return searchRadiusKm;
        }

        public void setSearchRadiusKm(double searchRadiusKm) {
            this.searchRadiusKm = searchRadiusKm;
        }

        public double getFallbackRadiusKm() {
            return fallbackRadiusKm;
        }

        public void setFallbackRadiusKm(double fallbackRadiusKm) {
            this.fallbackRadiusKm = fallbackRadiusKm;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public static class GeoapifyProperties {
            private String apiKey = "";
            private String apiUrl = "https://api.geoapify.com/v2/places";
            private String categories = "healthcare.hospital,healthcare.clinic_or_praxis";
            private double initialRadiusKm = 10.0;
            private double expandedRadiusKm = 25.0;
            private int minResultsToExpand = 2;
            private int limit = 5;
            private int timeoutMs = 8000;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey != null ? apiKey.trim() : "";
            }

            public String getApiUrl() {
                return apiUrl;
            }

            public void setApiUrl(String apiUrl) {
                this.apiUrl = apiUrl;
            }

            public String getCategories() {
                return categories;
            }

            public void setCategories(String categories) {
                this.categories = categories;
            }

            public double getInitialRadiusKm() {
                return initialRadiusKm;
            }

            public void setInitialRadiusKm(double initialRadiusKm) {
                this.initialRadiusKm = initialRadiusKm;
            }

            public double getExpandedRadiusKm() {
                return expandedRadiusKm;
            }

            public void setExpandedRadiusKm(double expandedRadiusKm) {
                this.expandedRadiusKm = expandedRadiusKm;
            }

            public int getMinResultsToExpand() {
                return minResultsToExpand;
            }

            public void setMinResultsToExpand(int minResultsToExpand) {
                this.minResultsToExpand = minResultsToExpand;
            }

            public int getLimit() {
                return limit;
            }

            public void setLimit(int limit) {
                this.limit = limit;
            }

            public int getTimeoutMs() {
                return timeoutMs;
            }

            public void setTimeoutMs(int timeoutMs) {
                this.timeoutMs = timeoutMs;
            }
        }
    }
}
