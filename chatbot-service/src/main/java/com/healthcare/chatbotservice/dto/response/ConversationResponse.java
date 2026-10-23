package com.healthcare.chatbotservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponse {

    private UUID conversationId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ConversationResponse() {
    }

    public ConversationResponse(UUID conversationId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.conversationId = conversationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID conversationId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder conversationId(UUID conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ConversationResponse build() {
            return new ConversationResponse(conversationId, createdAt, updatedAt);
        }
    }
}
