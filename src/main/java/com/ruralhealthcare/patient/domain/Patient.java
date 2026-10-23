package com.ruralhealthcare.patient.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import com.ruralhealthcare.common.domain.GeoLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "age")
    private Integer age;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_number", length = 30)
    private String emergencyContactNumber;

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

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "preferred_language", nullable = false, length = 50)
    private String preferredLanguage = "Hindi";

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Embedded
    private GeoLocation geoLocation;

    protected Patient() {
    }

    public Patient(UUID id, String fullName, Gender gender, LocalDate dateOfBirth, Integer age,
                   String contactNumber, String emergencyContactName, String emergencyContactNumber,
                   String villageTown, String subDistrict, String district, String state,
                   String pincode, String bloodGroup, String preferredLanguage, GeoLocation geoLocation) {
        this.id = id != null ? id : UUID.randomUUID();
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
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : "Hindi";
        this.geoLocation = geoLocation;
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

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
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
        private UUID id = UUID.randomUUID();
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
        private String preferredLanguage = "Hindi";
        private Double heightCm;
        private Double weightKg;
        private String medicalHistory;
        private GeoLocation geoLocation;

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

        public Builder geoLocation(GeoLocation geoLocation) {
            this.geoLocation = geoLocation;
            return this;
        }

        public Patient build() {
            Patient p = new Patient(id, fullName, gender, dateOfBirth, age, contactNumber,
                    emergencyContactName, emergencyContactNumber, villageTown, subDistrict,
                    district, state, pincode, bloodGroup, preferredLanguage, geoLocation);
            p.setHeightCm(heightCm);
            p.setWeightKg(weightKg);
            p.setMedicalHistory(medicalHistory);
            return p;
        }
    }
}
