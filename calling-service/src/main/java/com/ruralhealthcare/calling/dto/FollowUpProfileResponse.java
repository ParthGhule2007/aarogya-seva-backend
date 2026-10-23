package com.ruralhealthcare.calling.dto;

import com.ruralhealthcare.calling.domain.FollowUpProfile;
import com.ruralhealthcare.calling.domain.Language;

import java.time.Instant;
import java.util.UUID;

public class FollowUpProfileResponse {

    private UUID id;
    private String phoneNumber;
    private String chosenName;
    private Language preferredLanguage;
    private String concernSummary;
    private boolean consentGiven;
    private boolean optedOut;
    private boolean firstCallCompleted;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant optedOutAt;

    public FollowUpProfileResponse() {
    }

    public static FollowUpProfileResponse fromProfile(FollowUpProfile profile) {
        FollowUpProfileResponse response = new FollowUpProfileResponse();
        response.id = profile.getId();
        response.phoneNumber = profile.getPhoneNumber();
        response.chosenName = profile.getChosenName();
        response.preferredLanguage = profile.getPreferredLanguage();
        response.concernSummary = profile.getConcernSummary();
        response.consentGiven = profile.isConsentGiven();
        response.optedOut = profile.isOptedOut();
        response.firstCallCompleted = profile.isFirstCallCompleted();
        response.createdAt = profile.getCreatedAt();
        response.updatedAt = profile.getUpdatedAt();
        response.optedOutAt = profile.getOptedOutAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getChosenName() {
        return chosenName;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getConcernSummary() {
        return concernSummary;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public boolean isOptedOut() {
        return optedOut;
    }

    public boolean isFirstCallCompleted() {
        return firstCallCompleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getOptedOutAt() {
        return optedOutAt;
    }
}
