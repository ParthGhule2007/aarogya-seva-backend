package com.healthcare.chatbotservice.dto.response;

public class AiChatResponse {

    private boolean success;
    private AiResponseData data;

    public AiChatResponse() {
    }

    public AiChatResponse(boolean success, AiResponseData data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public AiResponseData getData() {
        return data;
    }

    public void setData(AiResponseData data) {
        this.data = data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private AiResponseData data;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder data(AiResponseData data) {
            this.data = data;
            return this;
        }

        public AiChatResponse build() {
            return new AiChatResponse(success, data);
        }
    }

    public static class AiResponseData {
        private String response;

        public AiResponseData() {
        }

        public AiResponseData(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String response;

            public Builder response(String response) {
                this.response = response;
                return this;
            }

            public AiResponseData build() {
                return new AiResponseData(response);
            }
        }
    }
}
