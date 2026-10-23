package com.ruralhealthcare.calling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.TalkToAiRequest;
import com.ruralhealthcare.calling.repository.CallSessionRepository;
import com.ruralhealthcare.calling.repository.FollowUpProfileRepository;
import com.ruralhealthcare.calling.service.twilio.TwilioClient;
import com.ruralhealthcare.calling.service.ultravox.MockUltravoxClient;
import com.ruralhealthcare.calling.service.ultravox.UltravoxCallRequest;
import com.ruralhealthcare.calling.service.ultravox.UltravoxCallResponse;
import com.ruralhealthcare.calling.service.ultravox.UltravoxClient;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UltravoxInterruptionAndToolTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CallSessionRepository sessionRepository;

    @Autowired
    private FollowUpProfileRepository profileRepository;

    @Autowired
    private MockUltravoxClient mockUltravoxClient;

    @Autowired
    private PromptBuilderService promptBuilder;

    @Autowired
    private CallingProperties properties;

    @Autowired
    private TwilioClient twilioClient;

    @Autowired
    private FollowUpProfileService profileService;

    @BeforeEach
    void cleanUp() {
        sessionRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    @DisplayName("Ultravox call request configures VAD settings for sub-100ms interruption and natural pauses")
    void testCallCreationConfiguresUltravoxBargeInAndVAD() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq, "Ultravox request should be recorded by MockUltravoxClient");

        Map<String, Object> vad = ultravoxReq.getVadSettings();
        assertNotNull(vad, "vadSettings must be configured on Ultravox call creation");

        // Verify supported VAD barge-in and turn endpoint parameters
        assertEquals("0.384s", vad.get("turnEndpointDelay"), "turnEndpointDelay must allow natural pause (12*32ms)");
        assertEquals("0.064s", vad.get("minimumTurnDuration"), "minimumTurnDuration must allow fast speech detection");
        assertEquals("0.096s", vad.get("minimumInterruptionDuration"), "minimumInterruptionDuration must enable sub-100ms barge-in");
        assertEquals(0.3, vad.get("frameActivationThreshold"), "frameActivationThreshold must filter minor background noise");
    }

    @Test
    @DisplayName("Ultravox call request registers searchNearbyHospitals tool with valid HTTPS URL")
    void testCallCreationRegistersHospitalSearchToolWithHttps() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);

        List<Map<String, Object>> tools = ultravoxReq.getSelectedTools();
        assertNotNull(tools, "selectedTools must not be null");
        assertEquals(1, tools.size());

        Map<String, Object> toolDef = tools.get(0);
        assertTrue(toolDef.containsKey("temporaryTool"));

        @SuppressWarnings("unchecked")
        Map<String, Object> tempTool = (Map<String, Object>) toolDef.get("temporaryTool");
        assertEquals("searchNearbyHospitals", tempTool.get("modelToolName"));

        @SuppressWarnings("unchecked")
        Map<String, Object> http = (Map<String, Object>) tempTool.get("http");
        String baseUrl = (String) http.get("baseUrlPattern");
        assertNotNull(baseUrl);
        assertTrue(baseUrl.startsWith("https://"), "Tool webhook URL must use HTTPS to satisfy Ultravox requirement");
        assertTrue(baseUrl.endsWith("/api/v1/tools/search-hospitals"));
        assertEquals("POST", http.get("httpMethod"));
    }

    @Test
    @DisplayName("Real Ultravox client rejects non-HTTPS webhook URL with clear error message")
    void testRealUltravoxClientRejectsNonHttpsWebhookUrl() {
        // Create an un-mocked UltravoxClient
        UltravoxClient realClient = new UltravoxClient() {
            @Override
            public UltravoxCallResponse createCallSession(UltravoxCallRequest request) {
                return new UltravoxCallResponse("real_id", "wss://real.ultravox.ai/join");
            }

            @Override
            public boolean isMock() {
                return false;
            }
        };

        CallingProperties customProps = new CallingProperties();
        customProps.getTwilio().setWebhookBaseUrl("http://localhost:8089"); // non-https

        CallManagementService service = new CallManagementService(
                sessionRepository,
                profileService,
                promptBuilder,
                twilioClient,
                realClient,
                customProps
        );

        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "Ramesh", false);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.initiateTalkToAiCall(request));

        assertTrue(ex.getMessage().contains("Ultravox requires a public HTTPS URL for tool webhooks"));
        assertTrue(ex.getMessage().contains("CALLING_WEBHOOK_BASE_URL"));
    }

    @Test
    @DisplayName("Prompt builder includes barge-in and truthful hospital recommendation rules")
    void testPromptInstructionsIncludeBargeInAndTruthfulHospitals() {
        String prompt = promptBuilder.buildTalkToAiPrompt(Language.EN, "Ramesh", true);

        // Barge-in & interruption prompt rules
        assertTrue(prompt.contains("STOP your current response immediately"));
        assertTrue(prompt.contains("No, I meant Alandi in Pune, not Nanded"));
        assertTrue(prompt.contains("Do NOT resume or finish the old interrupted sentence"));

        // Hospital search & distance truthfulness rules
        assertTrue(prompt.contains("searchNearbyHospitals"));
        assertTrue(prompt.contains("approximate straight-line distance"));
        assertTrue(prompt.contains("NEVER invent or fabricate driving distance"));
        assertTrue(prompt.contains("tell the caller honestly and ask whether they want to expand the search radius"));
    }

    @Test
    @DisplayName("Ultravox tool webhook endpoint /api/v1/tools/search-hospitals executes reliably")
    void testUltravoxToolWebhookExecution() throws Exception {
        mockMvc.perform(post("/api/v1/tools/search-hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Alandi, Pune\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.queryLocation").value("Alandi, Pune, Maharashtra"))
                .andExpect(jsonPath("$.hospitals[0].name").value("Jaihind Multi & Super Speciality Hospital"))
                .andExpect(jsonPath("$.hospitals[0].distanceKm").value(1.8))
                .andExpect(jsonPath("$.hospitals[0].address").value("Near Markal Road, Alandi, Pune"));
    }
}
