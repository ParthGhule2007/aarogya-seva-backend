package com.ruralhealthcare.calling.service;

import com.ruralhealthcare.calling.domain.FollowUpProfile;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.FollowUpProfileResponse;
import com.ruralhealthcare.calling.repository.FollowUpProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FollowUpProfileService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpProfileService.class);

    private final FollowUpProfileRepository repository;

    public FollowUpProfileService(FollowUpProfileRepository repository) {
        this.repository = repository;
    }

    public Optional<FollowUpProfile> getProfile(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        return repository.findByPhoneNumber(normalized);
    }

    @Transactional
    public FollowUpProfile initializeOrUpdateProfile(String phoneNumber, String chosenName, Language language, boolean consentGiven) {
        String normalized = normalizePhoneNumber(phoneNumber);

        FollowUpProfile profile = repository.findByPhoneNumber(normalized)
                .orElseGet(() -> new FollowUpProfile(normalized, chosenName, language, consentGiven));

        if (chosenName != null && !chosenName.isBlank()) {
            profile.setChosenName(chosenName.trim());
        }
        if (language != null) {
            profile.setPreferredLanguage(language);
        }
        if (consentGiven) {
            profile.setConsentGiven(true);
        }

        return repository.save(profile);
    }

    @Transactional
    public FollowUpProfile markFirstCallCompleted(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        FollowUpProfile profile = repository.findByPhoneNumber(normalized)
                .orElseGet(() -> new FollowUpProfile(normalized, null, Language.EN, false));

        profile.setFirstCallCompleted(true);
        log.info("[FollowUpProfileService] Marked first call completed for phone [{}]", normalized);
        return repository.save(profile);
    }

    @Transactional
    public FollowUpProfileResponse saveConcernSummary(String phoneNumber, String summary, boolean consentGiven) {
        String normalized = normalizePhoneNumber(phoneNumber);

        FollowUpProfile profile = repository.findByPhoneNumber(normalized)
                .orElseThrow(() -> new IllegalArgumentException("No profile found for phone number: " + normalized));

        if (!consentGiven && !profile.isConsentGiven()) {
            throw new IllegalStateException("Explicit consent is required to save a health concern summary.");
        }

        profile.setConcernSummary(summary != null ? summary.trim() : null);
        profile.setConsentGiven(true);
        profile = repository.save(profile);

        log.info("[FollowUpProfileService] Successfully saved concern summary with consent for [{}]", normalized);
        return FollowUpProfileResponse.fromProfile(profile);
    }

    @Transactional
    public FollowUpProfileResponse optOut(String phoneNumber, String reason) {
        String normalized = normalizePhoneNumber(phoneNumber);

        FollowUpProfile profile = repository.findByPhoneNumber(normalized)
                .orElseGet(() -> new FollowUpProfile(normalized, null, Language.EN, false));

        profile.setOptedOut(true);
        profile.setConsentGiven(false);
        profile = repository.save(profile);

        log.info("[FollowUpProfileService] Patient [{}] opted out from follow-up calls. Reason: [{}]", normalized, reason);
        return FollowUpProfileResponse.fromProfile(profile);
    }

    @Transactional
    public boolean deleteProfile(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        if (repository.existsByPhoneNumber(normalized)) {
            repository.deleteByPhoneNumber(normalized);
            log.info("[FollowUpProfileService] Permanently deleted follow-up profile for [{}]", normalized);
            return true;
        }
        return false;
    }

    public static String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String clean = rawPhone.replaceAll("[^0-9+]", "").trim();
        if (!clean.startsWith("+")) {
            // Assume Indian number if 10 digits without prefix
            if (clean.length() == 10) {
                clean = "+91" + clean;
            } else {
                clean = "+" + clean;
            }
        }
        return clean;
    }
}
