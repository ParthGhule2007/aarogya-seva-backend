package com.example.doctorservice.enums;

/**
 * Represents the real-time availability status of a doctor for consultations.
 * AVAILABLE: Doctor is currently online and willing to receive consultation requests.
 * BUSY: Doctor is currently in an active consultation.
 * OFFLINE: Doctor is currently not available / off-duty.
 */
public enum DoctorAvailabilityStatus {
    AVAILABLE,
    BUSY,
    OFFLINE
}
