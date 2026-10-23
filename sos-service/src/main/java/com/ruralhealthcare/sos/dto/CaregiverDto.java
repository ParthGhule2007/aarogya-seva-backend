package com.ruralhealthcare.sos.dto;

import java.util.UUID;

public class CaregiverDto {

    private UUID caregiverId;
    private String name;
    private String contactNumber;
    private String relationship;

    public CaregiverDto() {
    }

    public CaregiverDto(UUID caregiverId, String name, String contactNumber, String relationship) {
        this.caregiverId = caregiverId;
        this.name = name;
        this.contactNumber = contactNumber;
        this.relationship = relationship;
    }

    public UUID getCaregiverId() {
        return caregiverId;
    }

    public void setCaregiverId(UUID caregiverId) {
        this.caregiverId = caregiverId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPhone() {
        return contactNumber;
    }

    public void setPhone(String phone) {
        this.contactNumber = phone;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID caregiverId;
        private String name;
        private String contactNumber;
        private String relationship;

        public Builder caregiverId(UUID caregiverId) {
            this.caregiverId = caregiverId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder contactNumber(String contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public Builder phone(String phone) {
            this.contactNumber = phone;
            return this;
        }

        public Builder relationship(String relationship) {
            this.relationship = relationship;
            return this;
        }

        public CaregiverDto build() {
            return new CaregiverDto(caregiverId, name, contactNumber, relationship);
        }
    }
}
