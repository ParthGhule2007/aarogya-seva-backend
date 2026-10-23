package com.ruralhealthcare.sos.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FacilityContactDto {

    private UUID facilityId;
    private String facilityName;
    private List<UUID> recipientUserIds = new ArrayList<>();
    private String emergencyNumber;
    private boolean operational;

    public FacilityContactDto() {
    }

    public FacilityContactDto(UUID facilityId, String facilityName, List<UUID> recipientUserIds,
                              String emergencyNumber, boolean operational) {
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.recipientUserIds = recipientUserIds != null ? new ArrayList<>(recipientUserIds) : new ArrayList<>();
        this.emergencyNumber = emergencyNumber;
        this.operational = operational;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public List<UUID> getRecipientUserIds() {
        return recipientUserIds;
    }

    public void setRecipientUserIds(List<UUID> recipientUserIds) {
        this.recipientUserIds = recipientUserIds != null ? new ArrayList<>(recipientUserIds) : new ArrayList<>();
    }

    public List<UUID> getAuthorizedRecipientIds() {
        return getRecipientUserIds();
    }

    public void setAuthorizedRecipientIds(List<UUID> authorizedRecipientIds) {
        setRecipientUserIds(authorizedRecipientIds);
    }

    public String getEmergencyNumber() {
        return emergencyNumber;
    }

    public void setEmergencyNumber(String emergencyNumber) {
        this.emergencyNumber = emergencyNumber;
    }

    public String getContactPhone() {
        return emergencyNumber;
    }

    public void setContactPhone(String contactPhone) {
        this.emergencyNumber = contactPhone;
    }

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID facilityId;
        private String facilityName;
        private List<UUID> recipientUserIds = new ArrayList<>();
        private String emergencyNumber;
        private boolean operational = true;

        public Builder facilityId(UUID facilityId) {
            this.facilityId = facilityId;
            return this;
        }

        public Builder facilityName(String facilityName) {
            this.facilityName = facilityName;
            return this;
        }

        public Builder recipientUserIds(List<UUID> recipientUserIds) {
            if (recipientUserIds != null) {
                this.recipientUserIds = new ArrayList<>(recipientUserIds);
            }
            return this;
        }

        public Builder authorizedRecipientIds(List<UUID> authorizedRecipientIds) {
            return recipientUserIds(authorizedRecipientIds);
        }

        public Builder emergencyNumber(String emergencyNumber) {
            this.emergencyNumber = emergencyNumber;
            return this;
        }

        public Builder contactPhone(String contactPhone) {
            this.emergencyNumber = contactPhone;
            return this;
        }

        public Builder operational(boolean operational) {
            this.operational = operational;
            return this;
        }

        public FacilityContactDto build() {
            return new FacilityContactDto(facilityId, facilityName, recipientUserIds, emergencyNumber, operational);
        }
    }
}
