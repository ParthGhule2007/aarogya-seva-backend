package com.example.communicationservice.exception;

public class DuplicateCommunicationSessionException extends RuntimeException {

    public DuplicateCommunicationSessionException(String message) {
        super(message);
    }

    public DuplicateCommunicationSessionException(Long consultationId) {
        super("A communication session already exists for consultation ID: " + consultationId);
    }
}
