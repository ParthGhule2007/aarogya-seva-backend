package com.ruralhealthcare.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.model")
public class AiConfigProperties {

    /**
     * AI Model provider: "mock" or "external". Defaults to "external".
     */
    private String provider = "external";

    /**
     * Base URL for the external AI model API.
     */
    private String baseUrl = "https://parthgule-arogya-seva.hf.space";

    /**
     * Predict endpoint path. Defaults to verified Gradio clinical tele-triage endpoint.
     */
    private String predictPath = "/gradio_api/call/run_clinical_triage";

    /**
     * Optional API key or bearer token.
     */
    private String apiKey = "";

    /**
     * Model version identifier.
     */
    private String version = "aarogya-clinical-engine-v2.0";

    /**
     * HTTP connect timeout in milliseconds.
     */
    private int connectTimeoutMs = 10000;

    /**
     * HTTP read timeout in milliseconds.
     */
    private int readTimeoutMs = 45000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPredictPath() {
        return predictPath;
    }

    public void setPredictPath(String predictPath) {
        this.predictPath = predictPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
