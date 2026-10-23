package com.example.communicationservice.dto;

import com.example.communicationservice.enums.CommunicationStatus;
import com.example.communicationservice.enums.CommunicationType;

import java.time.LocalDateTime;

public class CommunicationSessionResponseDTO {

    private Long id;
    private Long consultationId;
    private String userId;
    private Long doctorId;
    private String roomId;
    private String meetingUrl;
    private CommunicationType communicationType;
    private CommunicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime updatedAt;

    public CommunicationSessionResponseDTO() {
    }

    public CommunicationSessionResponseDTO(Long id, Long consultationId, String userId, Long doctorId,
                                           String roomId, String meetingUrl, CommunicationType communicationType,
                                           CommunicationStatus status, LocalDateTime createdAt,
                                           LocalDateTime startedAt, LocalDateTime endedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.consultationId = consultationId;
        this.userId = userId;
        this.doctorId = doctorId;
        this.roomId = roomId;
        this.meetingUrl = meetingUrl;
        this.communicationType = communicationType;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public CommunicationStatus getStatus() {
        return status;
    }

    public void setStatus(CommunicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long consultationId;
        private String userId;
        private Long doctorId;
        private String roomId;
        private String meetingUrl;
        private CommunicationType communicationType;
        private CommunicationStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

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

        public Builder roomId(String roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder meetingUrl(String meetingUrl) {
            this.meetingUrl = meetingUrl;
            return this;
        }

        public Builder communicationType(CommunicationType communicationType) {
            this.communicationType = communicationType;
            return this;
        }

        public Builder status(CommunicationStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(LocalDateTime endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CommunicationSessionResponseDTO build() {
            return new CommunicationSessionResponseDTO(id, consultationId, userId, doctorId,
                    roomId, meetingUrl, communicationType, status,
                    createdAt, startedAt, endedAt, updatedAt);
        }
    }
}
