package com.example.consultationservice.dto;

import com.example.consultationservice.enums.CancelledBy;
import com.example.consultationservice.enums.ConsultationStatus;
import com.example.consultationservice.enums.ConsultationType;
import com.example.consultationservice.enums.Urgency;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationResponseDTO {

    private Long consultationId;
    private Long id;
    private String userId;
    private String username;
    private Long doctorId;
    private String doctorName;
    private String disease;
    private Urgency urgency;
    private ConsultationType consultationType;
    private ConsultationStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private CancelledBy cancelledBy;
    private String roomId;
    private String meetingUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConsultationResponseDTO() {
    }

    public ConsultationResponseDTO(Long consultationId, Long id, String userId, String username,
                                   Long doctorId, String doctorName, String disease, Urgency urgency,
                                   ConsultationType consultationType, ConsultationStatus status,
                                   LocalDateTime requestedAt, LocalDateTime acceptedAt,
                                   LocalDateTime scheduledAt, LocalDateTime startedAt,
                                   LocalDateTime completedAt, LocalDateTime cancelledAt,
                                   CancelledBy cancelledBy, String roomId, String meetingUrl,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.consultationId = consultationId;
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.disease = disease;
        this.urgency = urgency;
        this.consultationType = consultationType;
        this.status = status;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.roomId = roomId;
        this.meetingUrl = meetingUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public CancelledBy getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(CancelledBy cancelledBy) {
        this.cancelledBy = cancelledBy;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
        private Long consultationId;
        private Long id;
        private String userId;
        private String username;
        private Long doctorId;
        private String doctorName;
        private String disease;
        private Urgency urgency;
        private ConsultationType consultationType;
        private ConsultationStatus status;
        private LocalDateTime requestedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime scheduledAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private CancelledBy cancelledBy;
        private String roomId;
        private String meetingUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder consultationId(Long consultationId) {
            this.consultationId = consultationId;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

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

        public Builder doctorName(String doctorName) {
            this.doctorName = doctorName;
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

        public Builder status(ConsultationStatus status) {
            this.status = status;
            return this;
        }

        public Builder requestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public Builder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public Builder scheduledAt(LocalDateTime scheduledAt) {
            this.scheduledAt = scheduledAt;
            return this;
        }

        public Builder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder cancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public Builder cancelledBy(CancelledBy cancelledBy) {
            this.cancelledBy = cancelledBy;
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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ConsultationResponseDTO build() {
            return new ConsultationResponseDTO(consultationId, id, userId, username, doctorId,
                    doctorName, disease, urgency, consultationType, status,
                    requestedAt, acceptedAt, scheduledAt, startedAt, completedAt, cancelledAt,
                    cancelledBy, roomId, meetingUrl, createdAt, updatedAt);
        }
    }
}
