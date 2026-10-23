package com.ruralhealthcare.facility.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import com.ruralhealthcare.common.domain.GeoLocation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "healthcare_facilities")
public class HealthcareFacility extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 50)
    private FacilityType facilityType;

    @Embedded
    private GeoLocation geoLocation;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "village_town", nullable = false, length = 100)
    private String villageTown;

    @Column(name = "sub_district", nullable = false, length = 100)
    private String subDistrict;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pincode", nullable = false, length = 20)
    private String pincode;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(name = "emergency_number", length = 30)
    private String emergencyNumber;

    @Column(name = "is_operational", nullable = false)
    private boolean isOperational = true;

    @Column(name = "is_demo_data", nullable = false)
    private boolean isDemoData = false;

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<FacilityServiceOffering> serviceOfferings = new HashSet<>();

    protected HealthcareFacility() {
    }

    public HealthcareFacility(UUID id, String name, FacilityType facilityType, GeoLocation geoLocation,
                              String addressLine1, String addressLine2, String villageTown, String subDistrict,
                              String district, String state, String pincode, String contactNumber,
                              String emergencyNumber, boolean isOperational, boolean isDemoData,
                              Set<FacilityServiceOffering> serviceOfferings) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.facilityType = facilityType;
        this.geoLocation = geoLocation;
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
        this.serviceOfferings = serviceOfferings != null ? serviceOfferings : new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addServiceOffering(FacilityServiceOffering offering) {
        serviceOfferings.add(offering);
        offering.setFacility(this);
    }

    public void removeServiceOffering(FacilityServiceOffering offering) {
        serviceOfferings.remove(offering);
        offering.setFacility(null);
    }

    public boolean hasService(MedicalServiceCode serviceCode) {
        return serviceOfferings.stream()
                .anyMatch(offering -> offering.getServiceCode() == serviceCode && offering.isAvailable());
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

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
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

    public boolean isOperational() {
        return isOperational;
    }

    public void setOperational(boolean operational) {
        isOperational = operational;
    }

    public boolean isDemoData() {
        return isDemoData;
    }

    public void setDemoData(boolean demoData) {
        isDemoData = demoData;
    }

    public Set<FacilityServiceOffering> getServiceOfferings() {
        return serviceOfferings;
    }

    public void setServiceOfferings(Set<FacilityServiceOffering> serviceOfferings) {
        this.serviceOfferings = serviceOfferings;
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String name;
        private FacilityType facilityType;
        private GeoLocation geoLocation;
        private String addressLine1;
        private String addressLine2;
        private String villageTown;
        private String subDistrict;
        private String district;
        private String state;
        private String pincode;
        private String contactNumber;
        private String emergencyNumber;
        private boolean isOperational = true;
        private boolean isDemoData = false;
        private Set<FacilityServiceOffering> serviceOfferings = new HashSet<>();

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

        public Builder geoLocation(GeoLocation geoLocation) {
            this.geoLocation = geoLocation;
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

        public Builder serviceOfferings(Set<FacilityServiceOffering> serviceOfferings) {
            this.serviceOfferings = serviceOfferings;
            return this;
        }

        public HealthcareFacility build() {
            return new HealthcareFacility(id, name, facilityType, geoLocation, addressLine1, addressLine2,
                    villageTown, subDistrict, district, state, pincode, contactNumber, emergencyNumber,
                    isOperational, isDemoData, serviceOfferings);
        }
    }
}
