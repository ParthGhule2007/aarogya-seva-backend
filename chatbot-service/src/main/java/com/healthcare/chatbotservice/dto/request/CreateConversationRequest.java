package com.healthcare.chatbotservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateConversationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "username is required")
    private String username;

    @NotNull(message = "age is required")
    @Positive(message = "age must be positive")
    private Integer age;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(String userId, String username, Integer age) {
        this.userId = userId;
        this.username = username;
        this.age = age;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String username;
        private Integer age;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder age(Integer age) {
            this.age = age;
            return this;
        }

        public CreateConversationRequest build() {
            return new CreateConversationRequest(userId, username, age);
        }
    }
}
