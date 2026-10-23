package com.example.consultationservice.dto;

import com.example.consultationservice.enums.ConsultationType;
import com.example.consultationservice.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ConsultationRequestDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotBlank(message = "Disease description is required")
    private String disease;

    @NotNull(message = "Urgency level is required")
    private Urgency urgency;

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;

    public ConsultationRequestDTO() {
    }

    public ConsultationRequestDTO(String userId, String username, Long doctorId, String disease, Urgency urgency, ConsultationType consultationType) {
        this.userId = userId;
        this.username = username;
        this.doctorId = doctorId;
        this.disease = disease;
        this.urgency = urgency;
        this.consultationType = consultationType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
        this.consultationType = consultationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String username;
        private Long doctorId;
        private String disease;
        private Urgency urgency;
        private ConsultationType consultationType;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder doctorId(Long doctorId) {
            this.doctorId = doctorId;
            return this;
        }

        public Builder disease(String disease) {
            this.disease = disease;
            return this;
        }

        public Builder urgency(Urgency urgency) {
            this.urgency = urgency;
            return this;
        }

        public Builder consultationType(ConsultationType consultationType) {
            this.consultationType = consultationType;
            return this;
        }

        public ConsultationRequestDTO build() {
            return new ConsultationRequestDTO(userId, username, doctorId, disease, urgency, consultationType);
        }
    }
}
