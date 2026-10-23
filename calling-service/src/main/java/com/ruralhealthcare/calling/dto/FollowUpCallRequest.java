package com.ruralhealthcare.calling.dto;

import com.ruralhealthcare.calling.domain.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class FollowUpCallRequest {

    @Schema(description = "Patient destination phone number in E.164 or national format", example = "+919876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be a valid format (10 to 15 digits, optional leading +)")
    private String phoneNumber;

    @Schema(description = "Optional language preference: en, hi, or mr. If omitted, uses previous preference if known, else English", example = "hi")
    private String language;

    @Schema(description = "Optional chosen name for greeting", example = "Ramesh")
    private String chosenName;

    @Schema(description = "Optional voluntary health concern notes or context provided for this follow-up call", example = "mild headache")
    private String concernSummary;

    public FollowUpCallRequest() {
    }

    public FollowUpCallRequest(String phoneNumber, String language) {
        this.phoneNumber = phoneNumber;
        this.language = language;
    }

    public FollowUpCallRequest(String phoneNumber, String language, String chosenName) {
        this.phoneNumber = phoneNumber;
        this.language = language;
        this.chosenName = chosenName;
    }

    public FollowUpCallRequest(String phoneNumber, String language, String chosenName, String concernSummary) {
        this.phoneNumber = phoneNumber;
        this.language = language;
        this.chosenName = chosenName;
        this.concernSummary = concernSummary;
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

    public String getChosenName() {
        return chosenName;
    }

    public void setChosenName(String chosenName) {
        this.chosenName = chosenName;
    }

    public String getConcernSummary() {
        return concernSummary;
    }

    public void setConcernSummary(String concernSummary) {
        this.concernSummary = concernSummary;
    }

    public Language resolveLanguage(Language defaultLang) {
        if (this.language != null && !this.language.isBlank()) {
            return Language.fromCode(this.language);
        }
        return defaultLang != null ? defaultLang : Language.EN;
    }
}
