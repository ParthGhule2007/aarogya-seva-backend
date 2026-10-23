package com.ruralhealthcare.referral.api.dto;

import com.ruralhealthcare.referral.domain.ReferralStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateReferralStatusRequest {

    @NotNull(message = "Status is required")
    private ReferralStatus status;

    @Size(max = 2000, message = "Facility response must not exceed 2000 characters")
    private String facilityResponse;

    public UpdateReferralStatusRequest() {
    }

    public UpdateReferralStatusRequest(ReferralStatus status, String facilityResponse) {
        this.status = status;
        this.facilityResponse = facilityResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public String getFacilityResponse() {
        return facilityResponse;
    }

    public void setFacilityResponse(String facilityResponse) {
        this.facilityResponse = facilityResponse;
    }

    public static class Builder {
        private ReferralStatus status;
        private String facilityResponse;

        public Builder status(ReferralStatus status) {
            this.status = status;
            return this;
        }

        public Builder facilityResponse(String facilityResponse) {
            this.facilityResponse = facilityResponse;
            return this;
        }

        public UpdateReferralStatusRequest build() {
            return new UpdateReferralStatusRequest(status, facilityResponse);
        }
    }
}
