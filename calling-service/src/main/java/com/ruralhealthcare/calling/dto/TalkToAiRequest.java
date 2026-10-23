package com.ruralhealthcare.calling.dto;

import com.ruralhealthcare.calling.domain.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TalkToAiRequest {

    @Schema(description = "Patient destination phone number in E.164 or national format", example = "+919876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be a valid format (10 to 15 digits, optional leading +)")
    private String phoneNumber;

    @Schema(description = "Preferred language for voice conversation: en, hi, or mr", example = "hi", defaultValue = "en")
    private String language = "en";

    @Schema(description = "Optional chosen name for greeting", example = "Ramesh")
    private String chosenName;

    @Schema(description = "Explicit consent from patient to save a minimal concern summary for future follow-up", example = "true")
    private boolean consentToSaveSummary = false;

    public TalkToAiRequest() {
    }

    public TalkToAiRequest(String phoneNumber, String language, String chosenName, boolean consentToSaveSummary) {
        this.phoneNumber = phoneNumber;
        this.language = language != null ? language : "en";
        this.chosenName = chosenName;
        this.consentToSaveSummary = consentToSaveSummary;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Language resolveLanguage() {
        return Language.fromCode(this.language);
    }

    public String getChosenName() {
        return chosenName;
    }

    public void setChosenName(String chosenName) {
        this.chosenName = chosenName;
    }

    public boolean isConsentToSaveSummary() {
        return consentToSaveSummary;
    }

    public void setConsentToSaveSummary(boolean consentToSaveSummary) {
        this.consentToSaveSummary = consentToSaveSummary;
    }
}
