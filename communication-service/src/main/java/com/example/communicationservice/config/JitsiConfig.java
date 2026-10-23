package com.example.communicationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JitsiConfig {

    @Value("${jitsi.base-url:https://meet.jit.si}")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Builds the complete Jitsi meeting URL cleanly by avoiding duplicate trailing slashes.
     *
     * @param roomId unique room ID
     * @return full meeting URL (e.g., https://meet.jit.si/consultation-UUID)
     */
    public String buildMeetingUrl(String roomId) {
        String cleanBase = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.trim().replaceAll("/+$", "")
                : "https://meet.jit.si";
        return cleanBase + "/" + roomId;
    }
}
