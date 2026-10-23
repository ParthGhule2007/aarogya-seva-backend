package com.ruralhealthcare.ai.exception;

import com.ruralhealthcare.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AiServiceException extends BusinessException {

    public AiServiceException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static AiServiceException caseNotFound(UUID caseId) {
        return new AiServiceException(
                "Healthcare case not found with ID: " + caseId,
                "CASE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static AiServiceException insufficientCaseData(String message) {
        return new AiServiceException(
                message,
                "INSUFFICIENT_CASE_DATA",
                HttpStatus.BAD_REQUEST
        );
    }

    public static AiServiceException serviceUnavailable(String message, Throwable cause) {
        AiServiceException ex = new AiServiceException(
                message,
                "AI_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE
        );
        if (cause != null) {
            ex.initCause(cause);
        }
        return ex;
    }

    public static AiServiceException processingFailed(String message, Throwable cause) {
        AiServiceException ex = new AiServiceException(
                message,
                "AI_PROCESSING_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        if (cause != null) {
            ex.initCause(cause);
        }
        return ex;
    }

    public static AiServiceException invalidResponse(String message) {
        return new AiServiceException(
                message,
                "INVALID_AI_RESPONSE",
                HttpStatus.BAD_GATEWAY
        );
    }
}
