package com.example.consultationservice.exception;

public class InvalidConsultationStateException extends RuntimeException {
    public InvalidConsultationStateException(String message) {
        super(message);
    }
}
