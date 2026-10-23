package com.ruralhealthcare.calling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SaveConcernSummaryRequest {

    @Schema(description = "Phone number of the patient", example = "+919876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be a valid format (10 to 15 digits, optional leading +)")
    private String phoneNumber;

    @Schema(description = "Brief, minimal patient-reported concern summary", example = "Mild sore throat and body ache for 2 days")
    @NotBlank(message = "Concern summary is required")
    @Size(max = 1000, message = "Concern summary must not exceed 1000 characters")
    private String concernSummary;

    @Schema(description = "Explicit patient consent confirmation to save the summary for follow-up", example = "true")
    private boolean consentGiven = false;

    public SaveConcernSummaryRequest() {
    }

    public SaveConcernSummaryRequest(String phoneNumber, String concernSummary, boolean consentGiven) {
        this.phoneNumber = phoneNumber;
        this.concernSummary = concernSummary;
        this.consentGiven = consentGiven;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getConcernSummary() {
        return concernSummary;
    }

    public void setConcernSummary(String concernSummary) {
        this.concernSummary = concernSummary;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(boolean consentGiven) {
        this.consentGiven = consentGiven;
    }
}
