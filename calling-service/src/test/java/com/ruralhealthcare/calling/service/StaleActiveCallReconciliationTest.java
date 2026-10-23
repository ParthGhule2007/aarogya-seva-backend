package com.ruralhealthcare.calling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.domain.CallSession;
import com.ruralhealthcare.calling.domain.CallStatus;
import com.ruralhealthcare.calling.domain.CallType;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.FollowUpCallRequest;
import com.ruralhealthcare.calling.dto.TalkToAiRequest;
import com.ruralhealthcare.calling.repository.CallSessionRepository;
import com.ruralhealthcare.calling.repository.FollowUpProfileRepository;
import com.ruralhealthcare.calling.security.TwilioSignatureValidator;
import com.ruralhealthcare.calling.service.twilio.TwilioClient;
import com.twilio.security.RequestValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaleActiveCallReconciliationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CallSessionRepository sessionRepository;

    @Autowired
    private FollowUpProfileRepository profileRepository;

    @Autowired
    private FollowUpProfileService profileService;

    @Autowired
    private CallingProperties properties;

    @Autowired
    private TwilioSignatureValidator signatureValidator;

    @MockBean
    private TwilioClient twilioClient;

    @BeforeEach
    void setUp() {
        properties.getTwilio().setMockMode(true);
        sessionRepository.deleteAll();
        profileRepository.deleteAll();

        // Default twilioClient behavior
        when(twilioClient.isMock()).thenReturn(false);
        when(twilioClient.initiateOutboundCall(anyString(), anyString(), anyString()))
                .thenReturn("CA_mock_outbound_" + System.currentTimeMillis());
    }

    @AfterEach
    void tearDown() {
        properties.getTwilio().setMockMode(true);
    }

    @Test
    @DisplayName("Scenario 1: Caller hangs up and the session becomes terminal")
    void testCallerHangsUpSessionBecomesTerminal() throws Exception {
        String phone = "+919876543201";
        String sid = "CA_live_hangup_caller_101";

        CallSession session = new CallSession(phone, CallType.TALK_TO_AI, Language.EN);
        session.setTwilioCallSid(sid);
        session.setStatus(CallStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        // Caller hangs up -> Twilio dispatches completed callback with duration
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "42"))
                .andExpect(status().isOk());

        CallSession updated = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, updated.getStatus());
        assertEquals(42, updated.getDurationSeconds());
        assertFalse(updated.getStatus().isActive(), "Session must be non-active (terminal)");
    }

    @Test
    @DisplayName("Scenario 2: Recipient hangs up and the session becomes terminal")
    void testRecipientHangsUpSessionBecomesTerminal() throws Exception {
        String phone = "+919876543202";
        String sid = "CA_live_hangup_recipient_102";

        CallSession session = new CallSession(phone, CallType.TALK_TO_AI, Language.HI);
        session.setTwilioCallSid(sid);
        session.setStatus(CallStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        // Recipient hangs up -> Twilio dispatches completed callback
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "15"))
                .andExpect(status().isOk());

        CallSession updated = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, updated.getStatus());
        assertEquals(15, updated.getDurationSeconds());
        assertFalse(updated.getStatus().isActive());
    }

    @Test
    @DisplayName("Scenario 3: Completed callback is received twice (Idempotent)")
    void testCompletedCallbackReceivedTwiceIsIdempotent() throws Exception {
        String phone = "+919876543203";
        String sid = "CA_live_duplicate_callback_103";

        CallSession session = new CallSession(phone, CallType.TALK_TO_AI, Language.MR);
        session.setTwilioCallSid(sid);
        session.setStatus(CallStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        // First callback
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "28"))
                .andExpect(status().isOk());

        // Second identical callback (duplicate delivery)
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "28"))
                .andExpect(status().isOk());

        CallSession updated = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, updated.getStatus());
        assertEquals(28, updated.getDurationSeconds());
    }

    @Test
    @DisplayName("Scenario 4: A late ringing/in-progress callback arrives after completed (Out-of-order safety)")
    void testLateRingingCallbackIgnoredAfterCompleted() throws Exception {
        String phone = "+919876543204";
        String sid = "CA_live_late_ringing_104";

        CallSession session = new CallSession(phone, CallType.TALK_TO_AI, Language.EN);
        session.setTwilioCallSid(sid);
        session.setStatus(CallStatus.COMPLETED);
        session.setDurationSeconds(50);
        session = sessionRepository.save(session);

        // Delayed out-of-order callback: "ringing"
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "ringing"))
                .andExpect(status().isOk());

        CallSession afterRinging = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, afterRinging.getStatus(), "Terminal status must NOT revert to RINGING");

        // Delayed out-of-order callback: "in-progress"
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid)
                        .param("CallStatus", "in-progress"))
                .andExpect(status().isOk());

        CallSession afterInProgress = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, afterInProgress.getStatus(), "Terminal status must NOT revert to IN_PROGRESS");
    }

    @Test
    @DisplayName("Scenario 5: Callback signature validation succeeds behind the configured HTTPS tunnel")
    void testSignatureValidationSucceedsBehindHttpsTunnel() throws Exception {
        String testAuthToken = "0123456789abcdef0123456789abcdef";
        try {
            properties.getTwilio().setMockMode(false);
            properties.getTwilio().setAuthToken(testAuthToken);
            properties.getTwilio().setWebhookBaseUrl("https://health-test.ngrok-free.app");

            String publicUrl = "https://health-test.ngrok-free.app/api/v1/webhooks/twilio/status-callback";
            Map<String, String> params = Map.of(
                    "CallSid", "CA_tunnel_sig_test",
                    "CallStatus", "completed",
                    "CallDuration", "35"
            );

            // Compute valid Twilio signature for the public tunnel URL
            RequestValidator rv = new RequestValidator(testAuthToken);
            java.lang.reflect.Method m = RequestValidator.class.getDeclaredMethod("getValidationSignature", String.class, Map.class);
            m.setAccessible(true);
            String expectedSignature = (String) m.invoke(rv, publicUrl, params);

            // Request forwarded through ngrok tunnel into container
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhooks/twilio/status-callback");
            request.setServerName("localhost");
            request.setServerPort(8089);
            request.setScheme("http");
            request.addHeader("X-Forwarded-Proto", "https");
            request.addHeader("X-Forwarded-Host", "health-test.ngrok-free.app");

            boolean isValid = signatureValidator.validate(request, params, expectedSignature);
            assertTrue(isValid, "Signature must validate successfully against candidate URLs reconstructed from tunnel headers");
        } finally {
            properties.getTwilio().setMockMode(true);
        }
    }

    @Test
    @DisplayName("Scenario 6: A follow-up call is accepted after the previous call ends (Auto-reconciliation of stale session)")
    void testFollowUpCallAcceptedAfterPreviousCallEndsViaReconciliation() throws Exception {
        String phone = "+919876543206";
        String previousSid = "CA_live_previous_ended_106";

        // Setup profile: completed first call with consent
        profileService.initializeOrUpdateProfile(phone, "Ramesh", Language.EN, true);
        profileService.markFirstCallCompleted(phone);
        profileService.saveConcernSummary(phone, "recovering from flu", true);

        // Setup a previous session that ended in reality on the telephony network,
        // but was left in IN_PROGRESS in the DB because webhook callback was delayed/lost
        CallSession staleSession = new CallSession(phone, CallType.TALK_TO_AI, Language.EN);
        staleSession.setTwilioCallSid(previousSid);
        staleSession.setStatus(CallStatus.IN_PROGRESS);
        staleSession = sessionRepository.save(staleSession);

        // Authoritative Twilio status reports that previousSid is COMPLETED
        when(twilioClient.fetchCallStatus(previousSid)).thenReturn("completed");

        // Patient initiates Follow-up Call
        FollowUpCallRequest followUpReq = new FollowUpCallRequest(phone, "en");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(followUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INITIATED"));

        // Verify the old stale session was reconciled to COMPLETED
        CallSession reconciled = sessionRepository.findById(staleSession.getId()).orElseThrow();
        assertEquals(CallStatus.COMPLETED, reconciled.getStatus(), "Stale session must be reconciled to COMPLETED");
    }

    @Test
    @DisplayName("Scenario 7: A genuinely active call still correctly returns HTTP 409")
    void testGenuinelyActiveCallReturns409Conflict() throws Exception {
        String phone = "+919876543207";
        String activeSid = "CA_live_genuinely_active_107";

        CallSession activeSession = new CallSession(phone, CallType.TALK_TO_AI, Language.HI);
        activeSession.setTwilioCallSid(activeSid);
        activeSession.setStatus(CallStatus.IN_PROGRESS);
        sessionRepository.save(activeSession);

        // Twilio confirms call is genuinely still in-progress
        when(twilioClient.fetchCallStatus(activeSid)).thenReturn("in-progress");

        TalkToAiRequest newRequest = new TalkToAiRequest(phone, "hi", "सुनील", false);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already currently in progress")));
    }

    @Test
    @DisplayName("Scenario 8: Twilio status lookup fails without corrupting call state")
    void testTwilioLookupFailsWithoutCorruptingCallState() throws Exception {
        String phone = "+919876543208";
        String activeSid = "CA_live_twilio_down_108";

        CallSession activeSession = new CallSession(phone, CallType.TALK_TO_AI, Language.EN);
        activeSession.setTwilioCallSid(activeSid);
        activeSession.setStatus(CallStatus.IN_PROGRESS);
        sessionRepository.save(activeSession);

        // Twilio status lookup throws exception / network failure
        when(twilioClient.fetchCallStatus(activeSid)).thenThrow(new RuntimeException("Twilio API unreachable 503"));

        TalkToAiRequest newRequest = new TalkToAiRequest(phone, "en", "Pooja", false);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already currently in progress")));

        // Verify active call state is NOT corrupted and remains IN_PROGRESS
        CallSession current = sessionRepository.findById(activeSession.getId()).orElseThrow();
        assertEquals(CallStatus.IN_PROGRESS, current.getStatus(),
                "Session must remain IN_PROGRESS without corruption when Twilio is down");
    }
}
