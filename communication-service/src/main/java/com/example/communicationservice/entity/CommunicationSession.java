package com.example.communicationservice.entity;

import com.example.communicationservice.enums.CommunicationStatus;
import com.example.communicationservice.enums.CommunicationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "communication_sessions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultation_id", columnNames = "consultation_id"),
        @UniqueConstraint(name = "uk_room_id", columnNames = "room_id")
    }
)
public class CommunicationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private Long consultationId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "room_id", nullable = false, unique = true)
    private String roomId;

    @Column(name = "meeting_url", nullable = false)
    private String meetingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type", nullable = false)
    private CommunicationType communicationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommunicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CommunicationSession() {
    }

    public CommunicationSession(Long id, Long consultationId, String userId, Long doctorId,
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

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CommunicationStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

        public CommunicationSession build() {
            return new CommunicationSession(id, consultationId, userId, doctorId, roomId, meetingUrl, communicationType, status, createdAt, startedAt, endedAt, updatedAt);
        }
    }
}
