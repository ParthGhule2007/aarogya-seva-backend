package com.example.doctorservice.entity;

import com.example.doctorservice.enums.DoctorAvailabilityStatus;
import com.example.doctorservice.enums.DoctorStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "doctors",
    indexes = {
        @Index(name = "idx_doctor_user_id", columnList = "user_id"),
        @Index(name = "idx_doctor_status_avail", columnList = "status, availability_status")
    }
)
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to external User entity managed by User Service.
     * Stored as a pure identifier without JPA relationships.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "qualification", length = 100)
    private String qualification;

    @Column(name = "hospital_name", length = 150)
    private String hospitalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DoctorStatus status = DoctorStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private DoctorAvailabilityStatus availabilityStatus = DoctorAvailabilityStatus.OFFLINE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Doctor() {
    }

    public Doctor(Long id, String userId, String name, String specialization, String licenseNumber,
                  Integer experienceYears, String qualification, String hospitalName,
                  DoctorStatus status, DoctorAvailabilityStatus availabilityStatus,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.qualification = qualification;
        this.hospitalName = hospitalName;
        this.status = status != null ? status : DoctorStatus.ACTIVE;
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus : DoctorAvailabilityStatus.OFFLINE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public DoctorStatus getStatus() {
        return status;
    }

    public void setStatus(DoctorStatus status) {
        this.status = status;
    }

    public DoctorAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
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
        private Long id;
        private String userId;
        private String name;
        private String specialization;
        private String licenseNumber;
        private Integer experienceYears;
        private String qualification;
        private String hospitalName;
        private DoctorStatus status = DoctorStatus.ACTIVE;
        private DoctorAvailabilityStatus availabilityStatus = DoctorAvailabilityStatus.OFFLINE;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder specialization(String specialization) {
            this.specialization = specialization;
            return this;
        }

        public Builder licenseNumber(String licenseNumber) {
            this.licenseNumber = licenseNumber;
            return this;
        }

        public Builder experienceYears(Integer experienceYears) {
            this.experienceYears = experienceYears;
            return this;
        }

        public Builder qualification(String qualification) {
            this.qualification = qualification;
            return this;
        }

        public Builder hospitalName(String hospitalName) {
            this.hospitalName = hospitalName;
            return this;
        }

        public Builder status(DoctorStatus status) {
            this.status = status;
            return this;
        }

        public Builder availabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
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

        public Doctor build() {
            return new Doctor(id, userId, name, specialization, licenseNumber, experienceYears, qualification, hospitalName, status, availabilityStatus, createdAt, updatedAt);
        }
    }
}
