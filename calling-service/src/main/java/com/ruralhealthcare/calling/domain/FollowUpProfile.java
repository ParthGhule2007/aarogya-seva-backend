package com.ruralhealthcare.calling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follow_up_profiles")
public class FollowUpProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "phone_number", nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(name = "chosen_name", length = 100)
    private String chosenName;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 10)
    private Language preferredLanguage = Language.EN;

    @Column(name = "concern_summary", length = 1000)
    private String concernSummary;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = false;

    @Column(name = "opted_out", nullable = false)
    private boolean optedOut = false;

    @Column(name = "first_call_completed", nullable = false)
    private boolean firstCallCompleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "opted_out_at")
    private Instant optedOutAt;

    protected FollowUpProfile() {
    }

    public FollowUpProfile(String phoneNumber, String chosenName, Language preferredLanguage, boolean consentGiven) {
        this.id = UUID.randomUUID();
        this.phoneNumber = phoneNumber;
        this.chosenName = chosenName;
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : Language.EN;
        this.consentGiven = consentGiven;
        this.optedOut = false;
        this.firstCallCompleted = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getChosenName() {
        return chosenName;
    }

    public void setChosenName(String chosenName) {
        this.chosenName = chosenName;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : Language.EN;
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

    public boolean isOptedOut() {
        return optedOut;
    }

    public void setOptedOut(boolean optedOut) {
        this.optedOut = optedOut;
        if (optedOut) {
            this.optedOutAt = Instant.now();
        }
    }

    public boolean isFirstCallCompleted() {
        return firstCallCompleted;
    }

    public void setFirstCallCompleted(boolean firstCallCompleted) {
        this.firstCallCompleted = firstCallCompleted;
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
