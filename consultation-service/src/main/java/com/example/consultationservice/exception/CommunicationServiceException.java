package com.example.consultationservice.exception;

public class CommunicationServiceException extends RuntimeException {

    public CommunicationServiceException(String message) {
        super(message);
    }

    public CommunicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
