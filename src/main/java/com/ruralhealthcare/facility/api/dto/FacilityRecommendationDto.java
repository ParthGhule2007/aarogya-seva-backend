package com.ruralhealthcare.facility.api.dto;

import com.ruralhealthcare.facility.domain.FacilityType;
import com.ruralhealthcare.facility.domain.MedicalServiceCode;

import java.util.Set;
import java.util.UUID;

public class FacilityRecommendationDto {

    private UUID facilityId;
    private String name;
    private FacilityType facilityType;
    private String facilityTypeDisplayName;
    private String addressSummary;
    private String villageTown;
    private String district;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String contactNumber;
    private String emergencyNumber;
    private boolean isOperational;
    private boolean isDemoData;
    private Set<MedicalServiceCode> availableServices;

    public FacilityRecommendationDto() {
    }

    public FacilityRecommendationDto(UUID facilityId, String name, FacilityType facilityType,
                                     String facilityTypeDisplayName, String addressSummary, String villageTown,
                                     String district, String state, String pincode, Double latitude,
                                     Double longitude, Double distanceKm, String contactNumber,
                                     String emergencyNumber, boolean isOperational, boolean isDemoData,
                                     Set<MedicalServiceCode> availableServices) {
        this.facilityId = facilityId;
        this.name = name;
        this.facilityType = facilityType;
        this.facilityTypeDisplayName = facilityTypeDisplayName;
        this.addressSummary = addressSummary;
        this.villageTown = villageTown;
        this.district = district;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.contactNumber = contactNumber;
        this.emergencyNumber = emergencyNumber;
        this.isOperational = isOperational;
        this.isDemoData = isDemoData;
        this.availableServices = availableServices;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
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

    public String getAddressSummary() {
        return addressSummary;
    }

    public void setAddressSummary(String addressSummary) {
        this.addressSummary = addressSummary;
    }

    public String getVillageTown() {
        return villageTown;
    }

    public void setVillageTown(String villageTown) {
        this.villageTown = villageTown;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
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

    public static class Builder {
        private UUID facilityId;
        private String name;
        private FacilityType facilityType;
        private String facilityTypeDisplayName;
        private String addressSummary;
        private String villageTown;
        private String district;
        private String state;
        private String pincode;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;
        private String contactNumber;
        private String emergencyNumber;
        private boolean isOperational;
        private boolean isDemoData;
        private Set<MedicalServiceCode> availableServices;

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
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

        public Builder addressSummary(String addressSummary) {
            this.addressSummary = addressSummary;
            return this;
        }

        public Builder villageTown(String villageTown) {
            this.villageTown = villageTown;
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

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
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

        public FacilityRecommendationDto build() {
            return new FacilityRecommendationDto(facilityId, name, facilityType, facilityTypeDisplayName,
                    addressSummary, villageTown, district, state, pincode, latitude, longitude,
                    distanceKm, contactNumber, emergencyNumber, isOperational, isDemoData, availableServices);
        }
    }
}
