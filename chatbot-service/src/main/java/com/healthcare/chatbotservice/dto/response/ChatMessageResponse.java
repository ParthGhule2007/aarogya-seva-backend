package com.healthcare.chatbotservice.dto.response;

import com.healthcare.chatbotservice.entity.MessageRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ChatMessageResponse {

    private UUID messageId;
    private MessageRole role;
    private String content;
    private OffsetDateTime createdAt;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(UUID messageId, MessageRole role, String content, OffsetDateTime createdAt) {
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID messageId;
        private MessageRole role;
        private String content;
        private OffsetDateTime createdAt;

        public Builder messageId(UUID messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder role(MessageRole role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ChatMessageResponse build() {
            return new ChatMessageResponse(messageId, role, content, createdAt);
        }
    }
}
