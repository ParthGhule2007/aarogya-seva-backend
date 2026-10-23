package com.ruralhealthcare.calling.service;

import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.repository.CallSessionRepository;
import com.ruralhealthcare.calling.service.twilio.TwilioClient;
import com.ruralhealthcare.calling.service.ultravox.UltravoxClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipientValidationTest {

    private CallManagementService callService;
    private CallingProperties properties;

    @BeforeEach
    void setUp() {
        CallSessionRepository sessionRepo = mock(CallSessionRepository.class);
        FollowUpProfileService profileService = mock(FollowUpProfileService.class);
        PromptBuilderService promptBuilder = mock(PromptBuilderService.class);
        TwilioClient twilioClient = mock(TwilioClient.class);
        UltravoxClient ultravoxClient = mock(UltravoxClient.class);

        properties = new CallingProperties();
        properties.getTwilio().setMockMode(false);
        properties.getTwilio().setVerifiedRecipients(List.of("+918010381892", "+919373581673"));

        callService = new CallManagementService(
                sessionRepo,
                profileService,
                promptBuilder,
                twilioClient,
                ultravoxClient,
                properties
        );
    }

    @Test
    @DisplayName("Dummy number 9422366324 is rejected and falls back dynamically to verified list")
    void testDummyNumberRejectedAndFallsBackToVerified() {
        String resolved = callService.resolveAndValidateRecipient("+919422366324");
        assertEquals("+918010381892", resolved);
        assertNotEquals("+919422366324", resolved);
    }

    @Test
    @DisplayName("Empty recipient dynamically loads from configured verified recipients")
    void testEmptyRecipientLoadsFromVerifiedList() {
        String resolved = callService.resolveAndValidateRecipient("");
        assertEquals("+918010381892", resolved);
    }

    @Test
    @DisplayName("Unverified recipient throws clear validation error in live mode")
    void testUnverifiedRecipientThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                callService.resolveAndValidateRecipient("+919876543210")
        );
        assertTrue(ex.getMessage().contains("Twilio verified recipient list"));
    }

    @Test
    @DisplayName("Valid verified recipient from TWILIO_VERIFIED_RECIPIENTS is accepted")
    void testValidVerifiedRecipientAccepted() {
        String resolved = callService.resolveAndValidateRecipient("+919373581673");
        assertEquals("+919373581673", resolved);
    }
}
