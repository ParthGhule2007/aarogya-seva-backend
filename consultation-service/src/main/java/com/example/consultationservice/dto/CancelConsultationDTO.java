package com.example.consultationservice.dto;

import com.example.consultationservice.enums.CancelledBy;
import jakarta.validation.constraints.NotNull;

public class CancelConsultationDTO {

    @NotNull(message = "cancelledBy is required (USER, DOCTOR, or SYSTEM)")
    private CancelledBy cancelledBy;

    public CancelConsultationDTO() {
    }

    public CancelConsultationDTO(CancelledBy cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public CancelledBy getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(CancelledBy cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CancelledBy cancelledBy;

        public Builder cancelledBy(CancelledBy cancelledBy) {
            this.cancelledBy = cancelledBy;
            return this;
        }

        public CancelConsultationDTO build() {
            return new CancelConsultationDTO(cancelledBy);
        }
    }
}
