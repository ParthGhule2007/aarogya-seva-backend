package com.healthcare.chatbotservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "message cannot be empty")
    @Size(max = 2000, message = "message cannot exceed 2000 characters")
    private String message;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String message;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public SendMessageRequest build() {
            return new SendMessageRequest(userId, message);
        }
    }
}
