package com.ruralhealthcare.calling.service;

import com.ruralhealthcare.calling.domain.FollowUpProfile;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.FollowUpProfileResponse;
import com.ruralhealthcare.calling.repository.FollowUpProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpProfileServiceTest {

    @Mock
    private FollowUpProfileRepository repository;

    private FollowUpProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new FollowUpProfileService(repository);
    }

    @Test
    @DisplayName("Phone number normalization handles 10-digit Indian numbers and prefixes")
    void testNormalizePhoneNumber() {
        assertEquals("+919876543210", FollowUpProfileService.normalizePhoneNumber("9876543210"));
        assertEquals("+919876543210", FollowUpProfileService.normalizePhoneNumber("+91 98765 43210"));
        assertEquals("+919876543210", FollowUpProfileService.normalizePhoneNumber("+91-9876543210"));
        assertEquals("+14155552671", FollowUpProfileService.normalizePhoneNumber("+14155552671"));
    }

    @Test
    @DisplayName("Saving health concern summary requires explicit consent")
    void testSaveConcernSummaryRequiresConsent() {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.HI, false);
        when(repository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(profile));

        assertThrows(IllegalStateException.class, () ->
                profileService.saveConcernSummary("+919876543210", "headache for 3 days", false)
        );
    }

    @Test
    @DisplayName("Saving health concern summary succeeds when consent is granted")
    void testSaveConcernSummaryWithConsent() {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.HI, false);
        when(repository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(profile));
        when(repository.save(any(FollowUpProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowUpProfileResponse response = profileService.saveConcernSummary("+919876543210", "mild throat pain", true);

        assertNotNull(response);
        assertEquals("mild throat pain", response.getConcernSummary());
        assertTrue(response.isConsentGiven());
        assertFalse(response.isOptedOut());
    }

    @Test
    @DisplayName("Patient opt-out disables future follow-up check-ins")
    void testOptOut() {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.HI, true);
        when(repository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(profile));
        when(repository.save(any(FollowUpProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowUpProfileResponse response = profileService.optOut("+919876543210", "Patient requested no more calls");

        assertNotNull(response);
        assertTrue(response.isOptedOut());
        assertFalse(response.isConsentGiven());
    }

    @Test
    @DisplayName("Delete profile removes stored record completely")
    void testDeleteProfile() {
        when(repository.existsByPhoneNumber("+919876543210")).thenReturn(true);

        boolean deleted = profileService.deleteProfile("+919876543210");

        assertTrue(deleted);
        verify(repository).deleteByPhoneNumber("+919876543210");
    }
}
