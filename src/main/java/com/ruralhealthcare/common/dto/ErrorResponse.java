package com.ruralhealthcare.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String path;
    private String correlationId;
    private Instant timestamp = Instant.now();
    private List<ValidationErrorDetail> validationErrors;

    public ErrorResponse() {
    }

    public ErrorResponse(String errorCode, String message, String path, String correlationId,
                         Instant timestamp, List<ValidationErrorDetail> validationErrors) {
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.validationErrors = validationErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ValidationErrorDetail> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationErrorDetail> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public static class Builder {
        private String errorCode;
        private String message;
        private String path;
        private String correlationId;
        private Instant timestamp = Instant.now();
        private List<ValidationErrorDetail> validationErrors;

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder validationErrors(List<ValidationErrorDetail> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(errorCode, message, path, correlationId, timestamp, validationErrors);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationErrorDetail {
        private String field;
        private String rejectedValue;
        private String message;

        public ValidationErrorDetail() {
        }

        public ValidationErrorDetail(String field, String rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public static ValidationErrorDetailBuilder builder() {
            return new ValidationErrorDetailBuilder();
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(String rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static class ValidationErrorDetailBuilder {
            private String field;
            private String rejectedValue;
            private String message;

            public ValidationErrorDetailBuilder field(String field) {
                this.field = field;
                return this;
            }

            public ValidationErrorDetailBuilder rejectedValue(String rejectedValue) {
                this.rejectedValue = rejectedValue;
                return this;
            }

            public ValidationErrorDetailBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ValidationErrorDetail build() {
                return new ValidationErrorDetail(field, rejectedValue, message);
            }
        }
    }
}
