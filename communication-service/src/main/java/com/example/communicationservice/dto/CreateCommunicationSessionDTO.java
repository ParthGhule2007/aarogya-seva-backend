package com.example.communicationservice.dto;

import com.example.communicationservice.enums.CommunicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCommunicationSessionDTO {

    @NotNull(message = "consultationId is required")
    private Long consultationId;

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "communicationType is required")
    private CommunicationType communicationType;

    public CreateCommunicationSessionDTO() {
    }

    public CreateCommunicationSessionDTO(Long consultationId, String userId, Long doctorId, CommunicationType communicationType) {
        this.consultationId = consultationId;
        this.userId = userId;
        this.doctorId = doctorId;
        this.communicationType = communicationType;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long consultationId;
        private String userId;
        private Long doctorId;
        private CommunicationType communicationType;

        public Builder consultationId(Long consultationId) {
            this.consultationId = consultationId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder doctorId(Long doctorId) {
            this.doctorId = doctorId;
            return this;
        }

        public Builder communicationType(CommunicationType communicationType) {
            this.communicationType = communicationType;
            return this;
        }

        public CreateCommunicationSessionDTO build() {
            return new CreateCommunicationSessionDTO(consultationId, userId, doctorId, communicationType);
        }
    }
}
