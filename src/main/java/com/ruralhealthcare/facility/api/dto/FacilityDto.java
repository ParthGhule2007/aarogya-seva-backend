package com.ruralhealthcare.facility.api.dto;

import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class FacilityDto {

    private UUID id;
    private String name;
    private FacilityType facilityType;
    private String facilityTypeDisplayName;
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
    private boolean isOperational;
    private boolean isDemoData;
    private Set<MedicalServiceCode> availableServices;
    private Instant createdAt;
    private Instant updatedAt;

    public FacilityDto() {
    }

    public FacilityDto(UUID id, String name, FacilityType facilityType, String facilityTypeDisplayName,
                       Double latitude, Double longitude, String addressLine1, String addressLine2,
                       String villageTown, String subDistrict, String district, String state,
                       String pincode, String contactNumber, String emergencyNumber, boolean isOperational,
                       boolean isDemoData, Set<MedicalServiceCode> availableServices,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.facilityType = facilityType;
        this.facilityTypeDisplayName = facilityTypeDisplayName;
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
        this.isDemoData = isDemoData;
        this.availableServices = availableServices;
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

    public String getFacilityTypeDisplayName() {
        return facilityTypeDisplayName;
    }

    public void setFacilityTypeDisplayName(String facilityTypeDisplayName) {
        this.facilityTypeDisplayName = facilityTypeDisplayName;
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

    @com.fasterxml.jackson.annotation.JsonProperty("isOperational")
    public boolean isOperational() {
        return isOperational;
    }

    public void setOperational(boolean operational) {
        isOperational = operational;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isDemoData")
    public boolean isDemoData() {
        return isDemoData;
    }

    public void setDemoData(boolean demoData) {
        isDemoData = demoData;
    }

    public Set<MedicalServiceCode> getAvailableServices() {
        return availableServices;
    }

    public void setAvailableServices(Set<MedicalServiceCode> availableServices) {
        this.availableServices = availableServices;
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

    public static class Builder {
        private UUID id;
        private String name;
        private FacilityType facilityType;
        private String facilityTypeDisplayName;
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
        private boolean isOperational;
        private boolean isDemoData;
        private Set<MedicalServiceCode> availableServices;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder facilityType(FacilityType facilityType) {
            this.facilityType = facilityType;
            return this;
        }

        public Builder facilityTypeDisplayName(String facilityTypeDisplayName) {
            this.facilityTypeDisplayName = facilityTypeDisplayName;
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

        public Builder isOperational(boolean isOperational) {
            this.isOperational = isOperational;
            return this;
        }

        public Builder isDemoData(boolean isDemoData) {
            this.isDemoData = isDemoData;
            return this;
        }

        public Builder availableServices(Set<MedicalServiceCode> availableServices) {
            this.availableServices = availableServices;
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

        public FacilityDto build() {
            return new FacilityDto(id, name, facilityType, facilityTypeDisplayName, latitude, longitude,
                    addressLine1, addressLine2, villageTown, subDistrict, district, state, pincode,
                    contactNumber, emergencyNumber, isOperational, isDemoData, availableServices,
                    createdAt, updatedAt);
        }
    }
}
