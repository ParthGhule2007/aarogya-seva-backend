package com.healthcare.chatbotservice.dto.request;

import java.util.List;

public class AiChatRequest {

    private Integer age;
    private String message;
    private List<AiHistoryItem> conversationHistory;

    public AiChatRequest() {
    }

    public AiChatRequest(Integer age, String message, List<AiHistoryItem> conversationHistory) {
        this.age = age;
        this.message = message;
        this.conversationHistory = conversationHistory;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiHistoryItem> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<AiHistoryItem> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer age;
        private String message;
        private List<AiHistoryItem> conversationHistory;

        public Builder age(Integer age) {
            this.age = age;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder conversationHistory(List<AiHistoryItem> conversationHistory) {
            this.conversationHistory = conversationHistory;
            return this;
        }

        public AiChatRequest build() {
            return new AiChatRequest(age, message, conversationHistory);
        }
    }

    public static class AiHistoryItem {
        private String role;
        private String content;

        public AiHistoryItem() {
        }

        public AiHistoryItem(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String role;
            private String content;

            public Builder role(String role) {
                this.role = role;
                return this;
            }

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public AiHistoryItem build() {
                return new AiHistoryItem(role, content);
            }
        }
    }
}
