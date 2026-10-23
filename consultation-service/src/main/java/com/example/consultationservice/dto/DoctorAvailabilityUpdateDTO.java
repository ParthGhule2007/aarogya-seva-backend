package com.example.consultationservice.dto;

public class DoctorAvailabilityUpdateDTO {

    private String availabilityStatus;

    public DoctorAvailabilityUpdateDTO() {
    }

    public DoctorAvailabilityUpdateDTO(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String availabilityStatus;

        public Builder availabilityStatus(String availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
            return this;
        }

        public DoctorAvailabilityUpdateDTO build() {
            return new DoctorAvailabilityUpdateDTO(availabilityStatus);
        }
    }
}
