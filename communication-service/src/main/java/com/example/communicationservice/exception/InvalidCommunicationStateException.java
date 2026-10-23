package com.example.communicationservice.exception;

public class InvalidCommunicationStateException extends RuntimeException {

    public InvalidCommunicationStateException(String message) {
        super(message);
    }
}
