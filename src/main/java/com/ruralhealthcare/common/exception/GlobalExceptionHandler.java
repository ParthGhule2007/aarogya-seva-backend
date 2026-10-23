package com.ruralhealthcare.common.exception;

import com.ruralhealthcare.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Business exception [{}]: {} on path {}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        List<ErrorResponse.ValidationErrorDetail> errors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(ErrorResponse.ValidationErrorDetail.builder()
                    .field(fieldError.getField())
                    .rejectedValue(fieldError.getRejectedValue() != null ? String.valueOf(fieldError.getRejectedValue()) : "null")
                    .message(fieldError.getDefaultMessage())
                    .build());
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("VALIDATION_FAILED")
                .message("Input validation failed for the request payload")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        List<ErrorResponse.ValidationErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(cv -> ErrorResponse.ValidationErrorDetail.builder()
                        .field(cv.getPropertyPath().toString())
                        .rejectedValue(String.valueOf(cv.getInvalidValue()))
                        .message(cv.getMessage())
                        .build())
                .toList();

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("CONSTRAINT_VIOLATION")
                .message("Request parameter constraint violated")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Illegal argument error on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Malformed JSON or invalid enum value on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Malformed JSON request or invalid parameter value";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            message = ex.getCause().getMessage();
        } else if (ex.getMessage() != null) {
            message = ex.getMessage();
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("MALFORMED_REQUEST")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String correlationId = getOrCreateCorrelationId(request);
        log.error("Unhandled internal server error [CID: {}] on path {}: ", correlationId, request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred while processing the request. Please contact support with the correlation ID.")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
