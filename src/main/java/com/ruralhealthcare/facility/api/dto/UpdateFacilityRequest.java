package com.ruralhealthcare.facility.api.dto;

import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class UpdateFacilityRequest {

    @Size(min = 3, max = 150, message = "Facility name must be between 3 and 150 characters")
    private String name;

    private FacilityType facilityType;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    private String addressLine1;
    private String addressLine2;
    private String villageTown;
    private String subDistrict;
    private String district;
    private String state;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit Indian postal code")
    private String pincode;

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0-9]{10,12}$", message = "Contact number must be a valid telephone/mobile format")
    private String contactNumber;

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0-9]{10,12}$", message = "Emergency number must be a valid telephone/mobile format")
    private String emergencyNumber;

    private Boolean isOperational;

    private Set<MedicalServiceCode> activeServices;

    public UpdateFacilityRequest() {
    }

    public UpdateFacilityRequest(String name, FacilityType facilityType, Double latitude, Double longitude,
                                 String addressLine1, String addressLine2, String villageTown, String subDistrict,
                                 String district, String state, String pincode, String contactNumber,
                                 String emergencyNumber, Boolean isOperational, Set<MedicalServiceCode> activeServices) {
        this.name = name;
        this.facilityType = facilityType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.villageTown = villageTown;
        this.subDistrict = subDistrict;
        this.district = district;
        this.state = state;
        this.pincode = pincode;
        this.contactNumber = contactNumber;
        this.emergencyNumber = emergencyNumber;
        this.isOperational = isOperational;
        this.activeServices = activeServices;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
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

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
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

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmergencyNumber() {
        return emergencyNumber;
    }

    public void setEmergencyNumber(String emergencyNumber) {
        this.emergencyNumber = emergencyNumber;
    }

    public Boolean getIsOperational() {
        return isOperational;
    }

    public void setIsOperational(Boolean operational) {
        isOperational = operational;
    }

    public Set<MedicalServiceCode> getActiveServices() {
        return activeServices;
    }

    public void setActiveServices(Set<MedicalServiceCode> activeServices) {
        this.activeServices = activeServices;
    }

    public static class Builder {
        private String name;
        private FacilityType facilityType;
        private Double latitude;
        private Double longitude;
        private String addressLine1;
        private String addressLine2;
        private String villageTown;
        private String subDistrict;
        private String district;
        private String state;
        private String pincode;
        private String contactNumber;
        private String emergencyNumber;
        private Boolean isOperational;
        private Set<MedicalServiceCode> activeServices;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder facilityType(FacilityType facilityType) {
            this.facilityType = facilityType;
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

        public Builder addressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public Builder addressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
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

        public Builder contactNumber(String contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public Builder emergencyNumber(String emergencyNumber) {
            this.emergencyNumber = emergencyNumber;
            return this;
        }

        public Builder isOperational(Boolean isOperational) {
            this.isOperational = isOperational;
            return this;
        }

        public Builder activeServices(Set<MedicalServiceCode> activeServices) {
            this.activeServices = activeServices;
            return this;
        }

        public UpdateFacilityRequest build() {
            return new UpdateFacilityRequest(name, facilityType, latitude, longitude, addressLine1,
                    addressLine2, villageTown, subDistrict, district, state, pincode, contactNumber,
                    emergencyNumber, isOperational, activeServices);
        }
    }
}
