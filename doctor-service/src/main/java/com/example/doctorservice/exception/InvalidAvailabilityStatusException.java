package com.example.doctorservice.exception;

public class InvalidAvailabilityStatusException extends RuntimeException {
    public InvalidAvailabilityStatusException(String message) {
        super(message);
    }
}
