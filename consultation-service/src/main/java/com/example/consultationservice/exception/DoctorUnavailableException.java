package com.example.consultationservice.exception;

public class DoctorUnavailableException extends RuntimeException {
    public DoctorUnavailableException(String message) {
        super(message);
    }

    public DoctorUnavailableException(Long doctorId) {
        super("Doctor with id " + doctorId + " is currently unavailable or busy");
    }
}
