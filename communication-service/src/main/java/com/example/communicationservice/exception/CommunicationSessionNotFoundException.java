package com.example.communicationservice.exception;

public class CommunicationSessionNotFoundException extends RuntimeException {

    public CommunicationSessionNotFoundException(String message) {
        super(message);
    }

    public CommunicationSessionNotFoundException(Long id) {
        super("Communication session not found with ID: " + id);
    }
}
