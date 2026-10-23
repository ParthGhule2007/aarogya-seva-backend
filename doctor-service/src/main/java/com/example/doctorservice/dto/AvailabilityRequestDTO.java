package com.example.doctorservice.dto;

import com.example.doctorservice.enums.DoctorAvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public class AvailabilityRequestDTO {

    @NotNull(message = "availabilityStatus is required")
    private DoctorAvailabilityStatus availabilityStatus;

    public AvailabilityRequestDTO() {
    }

    public AvailabilityRequestDTO(DoctorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public DoctorAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DoctorAvailabilityStatus availabilityStatus;

        public Builder availabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
            return this;
        }

        public AvailabilityRequestDTO build() {
            return new AvailabilityRequestDTO(availabilityStatus);
        }
    }
}
