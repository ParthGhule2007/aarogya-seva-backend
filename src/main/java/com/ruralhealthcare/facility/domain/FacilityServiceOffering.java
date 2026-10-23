package com.ruralhealthcare.facility.domain;

import com.ruralhealthcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "facility_service_offerings")
public class FacilityServiceOffering extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private HealthcareFacility facility;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_code", nullable = false, length = 50)
    private MedicalServiceCode serviceCode;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    protected FacilityServiceOffering() {
    }

    public FacilityServiceOffering(UUID id, HealthcareFacility facility, MedicalServiceCode serviceCode, boolean isAvailable) {
        this.id = id != null ? id : UUID.randomUUID();
        this.facility = facility;
        this.serviceCode = serviceCode;
        this.isAvailable = isAvailable;
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

    public HealthcareFacility getFacility() {
        return facility;
    }

    public void setFacility(HealthcareFacility facility) {
        this.facility = facility;
    }

    public MedicalServiceCode getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(MedicalServiceCode serviceCode) {
        this.serviceCode = serviceCode;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacilityServiceOffering that)) return false;
        return Objects.equals(facility != null ? facility.getId() : null, that.facility != null ? that.facility.getId() : null)
                && serviceCode == that.serviceCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(facility != null ? facility.getId() : null, serviceCode);
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private HealthcareFacility facility;
        private MedicalServiceCode serviceCode;
        private boolean isAvailable = true;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder facility(HealthcareFacility facility) {
            this.facility = facility;
            return this;
        }

        public Builder serviceCode(MedicalServiceCode serviceCode) {
            this.serviceCode = serviceCode;
            return this;
        }

        public Builder isAvailable(boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public FacilityServiceOffering build() {
            return new FacilityServiceOffering(id, facility, serviceCode, isAvailable);
        }
    }
}
