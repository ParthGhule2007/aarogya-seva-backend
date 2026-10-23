package com.ruralhealthcare.sos.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the push notification message details sent across notification channels.
 * Prevents domain and service layers from directly coupling to Firebase SDK classes.
 */
public class NotificationMessage {

    private final String targetToken;
    private final String title;
    private final String body;
    private final Map<String, String> data;

    public NotificationMessage(String targetToken, String title, String body, Map<String, String> data) {
        this.targetToken = targetToken;
        this.title = title;
        this.body = body;
        this.data = data != null ? Collections.unmodifiableMap(new HashMap<>(data)) : Collections.emptyMap();
    }

    public String getTargetToken() {
        return targetToken;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getData() {
        return data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetToken;
        private String title;
        private String body;
        private Map<String, String> data = new HashMap<>();

        public Builder targetToken(String targetToken) {
            this.targetToken = targetToken;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder data(String key, String value) {
            this.data.put(key, value);
            return this;
        }

        public Builder data(Map<String, String> data) {
            if (data != null) {
                this.data.putAll(data);
            }
            return this;
        }

        public NotificationMessage build() {
            return new NotificationMessage(targetToken, title, body, data);
        }
    }
}
