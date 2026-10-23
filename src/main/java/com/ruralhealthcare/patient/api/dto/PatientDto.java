package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PatientDto {

    private UUID id;
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String contactNumber;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String villageTown;
    private String subDistrict;
    private String district;
    private String state;
    private String pincode;
    private String bloodGroup;
    private String preferredLanguage;
    private Double heightCm;
    private Double weightKg;
    private String medicalHistory;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;
    private Instant updatedAt;

    public PatientDto() {
    }

    public PatientDto(UUID id, String fullName, Gender gender, LocalDate dateOfBirth, Integer age,
                      String contactNumber, String emergencyContactName, String emergencyContactNumber,
                      String villageTown, String subDistrict, String district, String state,
                      String pincode, String bloodGroup, String preferredLanguage, Double latitude,
                      Double longitude, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.contactNumber = contactNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactNumber = emergencyContactNumber;
        this.villageTown = villageTown;
        this.subDistrict = subDistrict;
        this.district = district;
        this.state = state;
        this.pincode = pincode;
        this.bloodGroup = bloodGroup;
        this.preferredLanguage = preferredLanguage;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getVillageTown() {
        return villageTown;
    }

    public void setVillageTown(String villageTown) {
        this.villageTown = villageTown;
    }

    public String getSubDistrict() {
        return subDistrict;
    }

    public void setSubDistrict(String subDistrict) {
        this.subDistrict = subDistrict;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public static class Builder {
        private UUID id;
        private String fullName;
        private Gender gender;
        private LocalDate dateOfBirth;
        private Integer age;
        private String contactNumber;
        private String emergencyContactName;
        private String emergencyContactNumber;
        private String villageTown;
        private String subDistrict;
        private String district;
        private String state;
        private String pincode;
        private String bloodGroup;
        private String preferredLanguage;
        private Double heightCm;
        private Double weightKg;
        private String medicalHistory;
        private Double latitude;
        private Double longitude;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder age(Integer age) {
            this.age = age;
            return this;
        }

        public Builder contactNumber(String contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public Builder emergencyContactName(String emergencyContactName) {
            this.emergencyContactName = emergencyContactName;
            return this;
        }

        public Builder emergencyContactNumber(String emergencyContactNumber) {
            this.emergencyContactNumber = emergencyContactNumber;
            return this;
        }

        public Builder villageTown(String villageTown) {
            this.villageTown = villageTown;
            return this;
        }

        public Builder subDistrict(String subDistrict) {
            this.subDistrict = subDistrict;
            return this;
        }

        public Builder district(String district) {
            this.district = district;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder pincode(String pincode) {
            this.pincode = pincode;
            return this;
        }

        public Builder bloodGroup(String bloodGroup) {
            this.bloodGroup = bloodGroup;
            return this;
        }

        public Builder preferredLanguage(String preferredLanguage) {
            this.preferredLanguage = preferredLanguage;
            return this;
        }

        public Builder heightCm(Double heightCm) {
            this.heightCm = heightCm;
            return this;
        }

        public Builder weightKg(Double weightKg) {
            this.weightKg = weightKg;
            return this;
        }

        public Builder medicalHistory(String medicalHistory) {
            this.medicalHistory = medicalHistory;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PatientDto build() {
            PatientDto dto = new PatientDto(id, fullName, gender, dateOfBirth, age, contactNumber,
                    emergencyContactName, emergencyContactNumber, villageTown, subDistrict,
                    district, state, pincode, bloodGroup, preferredLanguage, latitude, longitude,
                    createdAt, updatedAt);
            dto.setHeightCm(heightCm);
            dto.setWeightKg(weightKg);
            dto.setMedicalHistory(medicalHistory);
            return dto;
        }
    }
}
