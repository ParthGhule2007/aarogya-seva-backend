package com.ruralhealthcare.calling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.domain.CallStatus;
import com.ruralhealthcare.calling.domain.FollowUpProfile;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.FollowUpCallRequest;
import com.ruralhealthcare.calling.dto.TalkToAiRequest;
import com.ruralhealthcare.calling.repository.CallSessionRepository;
import com.ruralhealthcare.calling.repository.FollowUpProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CallSessionRepository sessionRepository;

    @Autowired
    private FollowUpProfileRepository profileRepository;

    @BeforeEach
    void cleanUp() {
        sessionRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Successfully initiates call in English")
    void testTalkToAiEnglishSuccess() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callSessionId", notNullValue()))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.callType").value("TALK_TO_AI"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.twilioCallSid", containsString("CA_mock_")))
                .andExpect(jsonPath("$.ultravoxCallId", notNullValue()));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Successfully initiates call in Hindi")
    void testTalkToAiHindiSuccess() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543211", "hi", "रामेश", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("hi"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Successfully initiates call in Marathi")
    void testTalkToAiMarathiSuccess() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543212", "mr", "सुरेश", false);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("mr"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Rejects invalid phone numbers")
    void testTalkToAiInvalidPhoneNumber() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("not-a-number", "en", "John", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.phoneNumber", notNullValue()));
    }

    @Test
    @DisplayName("Button 1: Concurrency guard blocks overlapping active calls to same number")
    void testTalkToAiDuplicateActiveCallBlocked() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", true);

        // First call initiates
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second call while first is still active -> 409 Conflict
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already currently in progress")));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Successfully initiates independently without prior call or profile")
    void testFollowUpSucceedsWithoutPriorCallOrProfile() throws Exception {
        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", "en");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callSessionId", notNullValue()))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.callType").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Successfully initiates with voluntary name and concern summary")
    void testFollowUpCallWithVoluntaryContext() throws Exception {
        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", "hi", "Pooja", "recovering from fever");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callSessionId", notNullValue()))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.callType").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.language").value("hi"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Rejects if patient opted out")
    void testFollowUpRejectsWhenOptedOut() throws Exception {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.EN, true);
        profile.setFirstCallCompleted(true);
        profile.setOptedOut(true);
        profileRepository.save(profile);

        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", "en");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("opted out")));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Initiates general check-in even when prior call had no consent to save summary")
    void testFollowUpSucceedsWhenNoPriorConsent() throws Exception {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.EN, false);
        profile.setFirstCallCompleted(true);
        profileRepository.save(profile);

        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", "en");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callSessionId", notNullValue()))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.callType").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Rejects invalid phone numbers")
    void testFollowUpRejectsInvalidPhone() throws Exception {
        FollowUpCallRequest request = new FollowUpCallRequest("123", "en");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.phoneNumber", containsString("valid format")));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Rejects unsupported language code")
    void testFollowUpRejectsUnsupportedLanguage() throws Exception {
        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", "fr");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported language")));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Successfully initiates when first call completed and consent granted")
    void testFollowUpCallSuccess() throws Exception {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.HI, true);
        profile.setFirstCallCompleted(true);
        profile.setConcernSummary("fever and body ache reported 3 days ago");
        profileRepository.save(profile);

        FollowUpCallRequest request = new FollowUpCallRequest("+919876543210", null);

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callSessionId", notNullValue()))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.callType").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.language").value("hi"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Twilio status callback updates call status and marks first call complete")
    void testTwilioStatusCallbackCompleted() throws Exception {
        // Place initial call
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", true);
        String responseJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String twilioSid = objectMapper.readTree(responseJson).get("twilioCallSid").asText();

        // Simulate Twilio status callback -> completed
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "125"))
                .andExpect(status().isOk());

        // Verify session updated
        var session = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.COMPLETED, session.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(125, session.getDurationSeconds());

        // Verify profile first_call_completed is true
        var profile = profileRepository.findByPhoneNumber("+919876543210").orElseThrow();
        assertTrue(profile.isFirstCallCompleted());
    }

    @Test
    @DisplayName("Ultravox tool execution webhook returns verified hospital search results")
    void testUltravoxToolSearchHospitals() throws Exception {
        Map<String, Object> toolPayload = Map.of("location", "Pune");

        mockMvc.perform(post("/api/v1/tools/search-hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toolPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.hospitals").isArray())
                .andExpect(jsonPath("$.hospitals[0].name", notNullValue()));
    }

    @Test
    @DisplayName("Patient opt-out and profile deletion endpoints work properly")
    void testOptOutAndDeleteProfile() throws Exception {
        FollowUpProfile profile = new FollowUpProfile("+919876543210", "Ramesh", Language.EN, true);
        profileRepository.save(profile);

        // Opt out
        mockMvc.perform(post("/api/v1/follow-up/opt-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phoneNumber", "+919876543210", "reason", "Not interested"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(true));

        // Delete profile
        mockMvc.perform(delete("/api/v1/follow-up/profile")
                        .param("phoneNumber", "+919876543210"))
                .andExpect(status().isNoContent());

        assertTrue(profileRepository.findByPhoneNumber("+919876543210").isEmpty());
    }

    @Test
    @DisplayName("Status lifecycle: initiated -> ringing -> in-progress -> completed")
    void testFullCallLifecycle() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        String resJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String twilioSid = objectMapper.readTree(resJson).get("twilioCallSid").asText();

        // 1. Ringing
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "ringing"))
                .andExpect(status().isOk());
        var s1 = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.RINGING, s1.getStatus());

        // 2. In-progress
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "in-progress"))
                .andExpect(status().isOk());
        var s2 = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.IN_PROGRESS, s2.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(s2.getStartedAt());

        // 3. Completed
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "45"))
                .andExpect(status().isOk());
        var s3 = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.COMPLETED, s3.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(45, s3.getDurationSeconds());
        org.junit.jupiter.api.Assertions.assertNotNull(s3.getEndedAt());
    }

    @Test
    @DisplayName("Twilio callback maps terminal statuses (busy, no-answer, canceled, failed)")
    void testTerminalStatuses() throws Exception {
        for (String twilioStatus : List.of("busy", "no-answer", "canceled", "failed")) {
            sessionRepository.deleteAll();
            TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
            String resJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String twilioSid = objectMapper.readTree(resJson).get("twilioCallSid").asText();

            mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                            .header("X-Twilio-Signature", "mock_valid_signature")
                            .param("CallSid", twilioSid)
                            .param("CallStatus", twilioStatus)
                            .param("ErrorMessage", "Terminal reason: " + twilioStatus))
                    .andExpect(status().isOk());

            var session = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
            assertFalse(session.getStatus().isActive());
            org.junit.jupiter.api.Assertions.assertTrue(session.getErrorMessage().contains(twilioStatus));
        }
    }

    @Test
    @DisplayName("Out-of-order callback protection: late in-progress does not overwrite terminal completed status")
    void testOutOfOrderCallbackDoesNotRevertTerminalStatus() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        String resJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String twilioSid = objectMapper.readTree(resJson).get("twilioCallSid").asText();

        // Arrive completed first
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "60"))
                .andExpect(status().isOk());

        var sessionBefore = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.COMPLETED, sessionBefore.getStatus());

        // Late out-of-order in-progress arrives
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "in-progress"))
                .andExpect(status().isOk());

        // Must still be COMPLETED
        var sessionAfter = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.COMPLETED, sessionAfter.getStatus());
        assertFalse(sessionAfter.getStatus().isActive());
    }

    @Test
    @DisplayName("Out-of-order callback protection: late initiated/ringing does not revert in-progress")
    void testOutOfOrderCallbackDoesNotRevertInProgress() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        String resJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String twilioSid = objectMapper.readTree(resJson).get("twilioCallSid").asText();

        // In-progress
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "in-progress"))
                .andExpect(status().isOk());

        // Late ringing
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "ringing"))
                .andExpect(status().isOk());

        var session = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    @DisplayName("Duplicate callbacks are idempotent")
    void testDuplicateCallbackIsIdempotent() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", true);
        String resJson = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String twilioSid = objectMapper.readTree(resJson).get("twilioCallSid").asText();

        // First completed callback
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "50"))
                .andExpect(status().isOk());

        // Duplicate completed callback
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", twilioSid)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "50"))
                .andExpect(status().isOk());

        var session = sessionRepository.findByTwilioCallSid(twilioSid).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CallStatus.COMPLETED, session.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(50, session.getDurationSeconds());
    }

    @Test
    @DisplayName("Completed call allows subsequent call retry while active call remains protected")
    void testCompletedCallAllowsRetryWhileActiveCallBlocked() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);

        // 1. Initial call created -> status INITIATED
        String res1 = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String sid1 = objectMapper.readTree(res1).get("twilioCallSid").asText();

        // 2. Active call duplicate guard blocks new call
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already currently in progress")));

        // 3. Callback arrives: completed
        mockMvc.perform(post("/api/v1/webhooks/twilio/status-callback")
                        .header("X-Twilio-Signature", "mock_valid_signature")
                        .param("CallSid", sid1)
                        .param("CallStatus", "completed")
                        .param("CallDuration", "30"))
                .andExpect(status().isOk());

        // 4. Ended call can now be retried without conflict!
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Sync endpoint synchronizes active call status with Twilio and frees duplicate guard")
    void testSyncCallStatusWithTwilio() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        String res = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String callId = objectMapper.readTree(res).get("callSessionId").asText();

        // Sync with mock twilio (returns completed)
        mockMvc.perform(post("/api/v1/calls/" + callId + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Now subsequent call can be placed
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Terminate endpoint cancels active call and frees duplicate guard")
    void testTerminateCall() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        String res = mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String callId = objectMapper.readTree(res).get("callSessionId").asText();

        // Terminate
        mockMvc.perform(post("/api/v1/calls/" + callId + "/terminate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Retry now succeeds
        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
