package com.example.consultationservice.dto;

public class CreateCommunicationSessionRequestDTO {

    private Long consultationId;
    private String userId;
    private Long doctorId;
    private String communicationType;

    public CreateCommunicationSessionRequestDTO() {
    }

    public CreateCommunicationSessionRequestDTO(Long consultationId, String userId, Long doctorId, String communicationType) {
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

    public String getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(String communicationType) {
        this.communicationType = communicationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long consultationId;
        private String userId;
        private Long doctorId;
        private String communicationType;

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

        public Builder communicationType(String communicationType) {
            this.communicationType = communicationType;
            return this;
        }

        public CreateCommunicationSessionRequestDTO build() {
            return new CreateCommunicationSessionRequestDTO(consultationId, userId, doctorId, communicationType);
        }
    }
}
