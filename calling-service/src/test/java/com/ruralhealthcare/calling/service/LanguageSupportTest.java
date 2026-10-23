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
import com.ruralhealthcare.calling.service.ultravox.MockUltravoxClient;
import com.ruralhealthcare.calling.service.ultravox.UltravoxCallRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LanguageSupportTest {

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
    private CallingProperties properties;

    @Autowired
    private FollowUpProfileService profileService;

    @BeforeEach
    void cleanUp() {
        sessionRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    @DisplayName("Talk to AI in English: Uses verified English voice ID and languageHint 'en'")
    void testTalkToAiEnglishVoiceAndHint() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543210", "en", "John", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("en"));

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        assertEquals("73527457-256b-4291-a36e-10128315db4b", ultravoxReq.getVoice(),
                "English voice must match verified Ultravox voice ID");
        assertEquals("en", ultravoxReq.getLanguageHint(), "Ultravox languageHint must be 'en'");
        assertTrue(ultravoxReq.getSystemPrompt().contains("PRIMARY LANGUAGE: English"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("Hello John, I am Aarogya Mitra"));
    }

    @Test
    @DisplayName("Talk to AI in Hindi: Uses verified Hindi voice ID and languageHint 'hi'")
    void testTalkToAiHindiVoiceAndHint() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543211", "hi", "रामेश", true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("hi"));

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        assertEquals("f90da51d-8133-4d19-aa0f-4ec99e14cb85", ultravoxReq.getVoice(),
                "Hindi voice must match verified Ultravox Hindi voice ID");
        assertEquals("hi", ultravoxReq.getLanguageHint(), "Ultravox languageHint must be 'hi'");
        assertTrue(ultravoxReq.getSystemPrompt().contains("PRIMARY LANGUAGE: Hindi (हिन्दी)"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("नमस्ते रामेश जी"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("108"));
    }

    @Test
    @DisplayName("Talk to AI in Marathi: Uses verified Marathi voice ID and languageHint 'mr'")
    void testTalkToAiMarathiVoiceAndHint() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543212", "mr", "सुरेश", false);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("mr"));

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        assertEquals("26b36187-9e6c-481c-90e3-a991baac0496", ultravoxReq.getVoice(),
                "Marathi voice must match verified Ultravox Marathi voice ID");
        assertEquals("mr", ultravoxReq.getLanguageHint(), "Ultravox languageHint must be 'mr'");
        assertTrue(ultravoxReq.getSystemPrompt().contains("PRIMARY LANGUAGE: Marathi (मराठी)"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("नमस्कार सुरेश जी"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("108"));
    }

    @Test
    @DisplayName("Language switching instructions: Prompts instruct seamless switching across EN, HI, MR")
    void testLanguageSwitchingInstructionsInPrompt() throws Exception {
        TalkToAiRequest request = new TalkToAiRequest("+919876543213", "hi", null, true);

        mockMvc.perform(post("/api/v1/calls/talk-to-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        String prompt = ultravoxReq.getSystemPrompt();

        assertTrue(prompt.contains("TRILINGUAL CAPABILITY & SEAMLESS LANGUAGE SWITCHING"));
        assertTrue(prompt.contains("If the caller speaks or responds in Hindi, immediately switch and respond in Hindi"));
        assertTrue(prompt.contains("If the caller speaks or responds in Marathi, immediately switch and respond in Marathi"));
        assertTrue(prompt.contains("If the caller speaks or responds in English, immediately switch and respond in English"));
        assertTrue(prompt.contains("Naturally support bilingual code-switching"));
        assertTrue(prompt.contains("PROPER NAMES PRESERVATION"));
    }

    @Test
    @DisplayName("Voice configuration fallback: Missing, blank, or unknown language cleanly falls back to English voice")
    void testVoiceFallbackForMissingOrInvalidConfig() {
        CallingProperties.UltravoxProperties ultravoxProps = new CallingProperties.UltravoxProperties();

        // 1. Normal configured voices
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage("en"));
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_HINDI_VOICE, ultravoxProps.getVoiceForLanguage("hi"));
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_MARATHI_VOICE, ultravoxProps.getVoiceForLanguage("mr"));

        // 2. Case insensitivity and whitespace trimming
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_HINDI_VOICE, ultravoxProps.getVoiceForLanguage(" HI "));
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_MARATHI_VOICE, ultravoxProps.getVoiceForLanguage("Mr"));

        // 3. Unknown language code -> falls back to configured English voice
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage("fr"));
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage("unknown"));

        // 4. Null or empty language code -> falls back to English voice
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage(null));
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage(""));

        // 5. Blank entry in map -> falls back to English voice
        Map<String, String> brokenMap = new HashMap<>();
        brokenMap.put("en", "custom-english-voice");
        brokenMap.put("hi", "   ");
        ultravoxProps.setVoice(brokenMap);
        assertEquals("custom-english-voice", ultravoxProps.getVoiceForLanguage("hi"));

        // 6. Completely empty voice map -> falls back to default constant
        ultravoxProps.setVoice(new HashMap<>());
        assertEquals(CallingProperties.UltravoxProperties.DEFAULT_ENGLISH_VOICE, ultravoxProps.getVoiceForLanguage("en"));
    }

    @Test
    @DisplayName("Follow-up Call in Marathi: Preserves saved language and configures Marathi voice")
    void testFollowUpCallInMarathi() throws Exception {
        String phone = "+919876543214";

        // Precondition: Set up completed first call profile with Marathi preference
        profileService.initializeOrUpdateProfile(phone, "सुनीता", Language.MR, true);
        profileService.markFirstCallCompleted(phone);
        profileService.saveConcernSummary(phone, "डोकेदुखी आणि ताप", true);

        FollowUpCallRequest request = new FollowUpCallRequest(phone, null); // Uses saved MR preference

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("mr"));

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        assertEquals("26b36187-9e6c-481c-90e3-a991baac0496", ultravoxReq.getVoice());
        assertEquals("mr", ultravoxReq.getLanguageHint());
        assertTrue(ultravoxReq.getSystemPrompt().contains("नमस्कार सुनीता जी, हे आरोग्य मित्र कडून आपले फॉलो-अप चेक-इन आहे"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("डोकेदुखी आणि ताप"));
    }

    @Test
    @DisplayName("Follow-up Call with Language Override: Caller can switch language for follow-up")
    void testFollowUpCallWithLanguageOverride() throws Exception {
        String phone = "+919876543215";

        // First call was in English
        profileService.initializeOrUpdateProfile(phone, "Anita", Language.EN, true);
        profileService.markFirstCallCompleted(phone);
        profileService.saveConcernSummary(phone, "chest congestion", true);

        // Follow-up requested with Hindi override
        FollowUpCallRequest request = new FollowUpCallRequest(phone, "hi");

        mockMvc.perform(post("/api/v1/calls/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("hi"));

        UltravoxCallRequest ultravoxReq = mockUltravoxClient.getLastRequest();
        assertNotNull(ultravoxReq);
        assertEquals("f90da51d-8133-4d19-aa0f-4ec99e14cb85", ultravoxReq.getVoice());
        assertEquals("hi", ultravoxReq.getLanguageHint());
        assertTrue(ultravoxReq.getSystemPrompt().contains("PRIMARY LANGUAGE: Hindi (हिन्दी)"));
        assertTrue(ultravoxReq.getSystemPrompt().contains("chest congestion"));
    }
}
