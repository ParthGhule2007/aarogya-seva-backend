package com.example.doctorservice.dto;

import com.example.doctorservice.enums.DoctorAvailabilityStatus;

/**
 * Sanitized response for public / consultation discovery.
 * Explicitly excludes sensitive information, contact numbers, and authentication data.
 */
public class AvailableDoctorResponseDTO {

    private Long id;
    private String userId;
    private String name;
    private String specialization;
    private Integer experienceYears;
    private String qualification;
    private String hospitalName;
    private DoctorAvailabilityStatus availabilityStatus;

    public AvailableDoctorResponseDTO() {
    }

    public AvailableDoctorResponseDTO(Long id, String userId, String name, String specialization,
                                      Integer experienceYears, String qualification,
                                      String hospitalName, DoctorAvailabilityStatus availabilityStatus) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.specialization = specialization;
        this.experienceYears = experienceYears;
        this.qualification = qualification;
        this.hospitalName = hospitalName;
        this.availabilityStatus = availabilityStatus;
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

    public DoctorAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String userId;
        private String name;
        private String specialization;
        private Integer experienceYears;
        private String qualification;
        private String hospitalName;
        private DoctorAvailabilityStatus availabilityStatus;

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

        public Builder availabilityStatus(DoctorAvailabilityStatus availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
            return this;
        }

        public AvailableDoctorResponseDTO build() {
            return new AvailableDoctorResponseDTO(id, userId, name, specialization, experienceYears, qualification, hospitalName, availabilityStatus);
        }
    }
}
