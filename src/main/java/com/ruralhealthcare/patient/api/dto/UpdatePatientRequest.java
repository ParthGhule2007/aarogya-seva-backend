package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdatePatientRequest {

    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    private String fullName;

    private Gender gender;

    private LocalDate dateOfBirth;

    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 130, message = "Age cannot exceed 130 years")
    private Integer age;

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0-9]{10,12}$", message = "Contact number must be a valid 10-12 digit telephone/mobile format")
    private String contactNumber;

    @Size(max = 150, message = "Emergency contact name cannot exceed 150 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0-9]{10,12}$", message = "Emergency contact number must be a valid format")
    private String emergencyContactNumber;

    @Size(max = 100, message = "Village/Town cannot exceed 100 characters")
    private String villageTown;

    @Size(max = 100, message = "Sub-district cannot exceed 100 characters")
    private String subDistrict;

    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit Indian postal code")
    private String pincode;

    @Size(max = 10, message = "Blood group cannot exceed 10 characters")
    private String bloodGroup;

    @Size(max = 50, message = "Preferred language cannot exceed 50 characters")
    private String preferredLanguage;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    public UpdatePatientRequest() {
    }

    public UpdatePatientRequest(String fullName, Gender gender, LocalDate dateOfBirth, Integer age,
                                String contactNumber, String emergencyContactName, String emergencyContactNumber,
                                String villageTown, String subDistrict, String district, String state,
                                String pincode, String bloodGroup, String preferredLanguage,
                                Double latitude, Double longitude) {
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
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {
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
        private Double latitude;
        private Double longitude;

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

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public UpdatePatientRequest build() {
            return new UpdatePatientRequest(fullName, gender, dateOfBirth, age, contactNumber,
                    emergencyContactName, emergencyContactNumber, villageTown, subDistrict,
                    district, state, pincode, bloodGroup, preferredLanguage, latitude, longitude);
        }
    }
}
