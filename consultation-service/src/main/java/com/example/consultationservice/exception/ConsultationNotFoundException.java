package com.example.consultationservice.exception;

public class ConsultationNotFoundException extends RuntimeException {
    public ConsultationNotFoundException(String message) {
        super(message);
    }

    public ConsultationNotFoundException(Long id) {
        super("Consultation not found with id: " + id);
    }
}
