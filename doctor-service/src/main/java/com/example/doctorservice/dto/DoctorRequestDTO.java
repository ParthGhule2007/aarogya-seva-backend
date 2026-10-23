package com.example.doctorservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DoctorRequestDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Doctor name cannot be blank")
    @Size(min = 2, max = 100, message = "Doctor name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Specialization cannot be blank")
    @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
    private String specialization;

    @NotBlank(message = "License number cannot be blank")
    @Size(min = 3, max = 50, message = "License number must be between 3 and 50 characters")
    private String licenseNumber;

    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears;

    @NotBlank(message = "Qualification cannot be blank")
    @Size(max = 100, message = "Qualification cannot exceed 100 characters")
    private String qualification;

    @NotBlank(message = "Hospital name cannot be blank")
    @Size(max = 150, message = "Hospital name cannot exceed 150 characters")
    private String hospitalName;

    public DoctorRequestDTO() {
    }

    public DoctorRequestDTO(String userId, String name, String specialization, String licenseNumber,
                            Integer experienceYears, String qualification, String hospitalName) {
        this.userId = userId;
        this.name = name;
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.qualification = qualification;
        this.hospitalName = hospitalName;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String name;
        private String specialization;
        private String licenseNumber;
        private Integer experienceYears;
        private String qualification;
        private String hospitalName;

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

        public DoctorRequestDTO build() {
            return new DoctorRequestDTO(userId, name, specialization, licenseNumber, experienceYears, qualification, hospitalName);
        }
    }
}
