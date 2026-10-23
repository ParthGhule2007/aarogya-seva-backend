package com.ruralhealthcare.calling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OptOutRequest {

    @Schema(description = "Phone number to opt out from future automated follow-up calls", example = "+919876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be a valid format (10 to 15 digits, optional leading +)")
    private String phoneNumber;

    @Schema(description = "Optional reason for opt-out", example = "No longer needed")
    private String reason;

    public OptOutRequest() {
    }

    public OptOutRequest(String phoneNumber, String reason) {
        this.phoneNumber = phoneNumber;
        this.reason = reason;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
