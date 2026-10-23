package com.example.doctorservice.dto;

import com.example.doctorservice.enums.DoctorStatus;
import jakarta.validation.constraints.NotNull;

public class StatusRequestDTO {

    @NotNull(message = "status is required")
    private DoctorStatus status;

    public StatusRequestDTO() {
    }

    public StatusRequestDTO(DoctorStatus status) {
        this.status = status;
    }

    public DoctorStatus getStatus() {
        return status;
    }

    public void setStatus(DoctorStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DoctorStatus status;

        public Builder status(DoctorStatus status) {
            this.status = status;
            return this;
        }

        public StatusRequestDTO build() {
            return new StatusRequestDTO(status);
        }
    }
}
