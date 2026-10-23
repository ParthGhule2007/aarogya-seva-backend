package com.ruralhealthcare.calling.service;

import com.ruralhealthcare.calling.domain.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilderService();
    }

    @Test
    @DisplayName("Button 1 Talk to AI: English prompt enforces AI identity, greetings, and non-diagnostic guidance")
    void testTalkToAiEnglishPrompt() {
        String prompt = promptBuilder.buildTalkToAiPrompt(Language.EN, "John", true);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Aarogya Mitra"));
        assertTrue(prompt.contains("NOT a licensed human doctor"));
        assertTrue(prompt.contains("searchNearbyHospitals"));
        assertTrue(prompt.contains("district"));
        assertTrue(prompt.contains("ambiguous"));
        assertTrue(prompt.contains("John"));
        assertTrue(prompt.contains("PRIMARY LANGUAGE: English"));
        assertTrue(prompt.contains("Hello John, I am Aarogya Mitra"));
        assertTrue(prompt.contains("English Emergency Directives"));
        assertTrue(prompt.contains("PROPER NAMES PRESERVATION"));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Hindi prompt includes natural Hindustani, greetings, emergency, and Hinglish code-switching")
    void testTalkToAiHindiPrompt() {
        String prompt = promptBuilder.buildTalkToAiPrompt(Language.HI, "रामेश", true);

        assertNotNull(prompt);
        assertTrue(prompt.contains("PRIMARY LANGUAGE: Hindi (हिन्दी)"));
        assertTrue(prompt.contains("Hinglish"));
        assertTrue(prompt.contains("108"));
        assertTrue(prompt.contains("रामेश"));
        assertTrue(prompt.contains("नमस्ते रामेश जी"));
        assertTrue(prompt.contains("नज़दीकी अस्पताल या स्वास्थ्य केंद्र खोजने के लिए"));
        assertTrue(prompt.contains("कृपया स्पष्ट करें कि आपका गाँव या शहर किस ज़िले में आता है"));
        assertTrue(prompt.contains("PROPER NAMES PRESERVATION"));
        assertTrue(prompt.contains("DO NOT translate, change, or transliterate proper hospital names"));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Marathi prompt includes Marathi guidance, greetings, and multilingual switching")
    void testTalkToAiMarathiPrompt() {
        String prompt = promptBuilder.buildTalkToAiPrompt(Language.MR, "सुरेश", false);

        assertNotNull(prompt);
        assertTrue(prompt.contains("PRIMARY LANGUAGE: Marathi (मराठी)"));
        assertTrue(prompt.contains("नमस्कार"));
        assertTrue(prompt.contains("108"));
        assertTrue(prompt.contains("सुरेश"));
        assertTrue(prompt.contains("नमस्कार सुरेश जी"));
        assertTrue(prompt.contains("जवळचे रुग्णालय किंवा आरोग्य केंद्र शोधण्यासाठी"));
        assertTrue(prompt.contains("कृपया स्पष्ट करा की आपले गाव किंवा शहर कोणत्या जिल्ह्यात येते"));
        assertTrue(prompt.contains("TRILINGUAL CAPABILITY & SEAMLESS LANGUAGE SWITCHING"));
    }

    @Test
    @DisplayName("Button 1 Talk to AI: Trilingual capability supports dynamic switching between EN, HI, and MR")
    void testTalkToAiTrilingualSwitchingRules() {
        String promptEn = promptBuilder.buildTalkToAiPrompt(Language.EN, null, false);
        assertTrue(promptEn.contains("If the caller speaks or responds in Hindi, immediately switch and respond in Hindi"));
        assertTrue(promptEn.contains("If the caller speaks or responds in Marathi, immediately switch and respond in Marathi"));
        assertTrue(promptEn.contains("If the caller speaks or responds in English, immediately switch and respond in English"));

        String promptHi = promptBuilder.buildTalkToAiPrompt(Language.HI, null, false);
        assertTrue(promptHi.contains("INITIAL GREETING (Hindi)"));
        assertTrue(promptHi.contains("Naturally support bilingual code-switching"));

        String promptMr = promptBuilder.buildTalkToAiPrompt(Language.MR, null, false);
        assertTrue(promptMr.contains("INITIAL GREETING (Marathi)"));
        assertTrue(promptMr.contains("Naturally support bilingual code-switching"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Injects minimal summary without external medical records")
    void testFollowUpPromptWithMinimalSummary() {
        String prompt = promptBuilder.buildFollowUpPrompt(Language.HI, "Anita", "mild cough and headache for 2 days");

        assertNotNull(prompt);
        assertTrue(prompt.contains("Anita"));
        assertTrue(prompt.contains("mild cough and headache for 2 days"));
        assertTrue(prompt.contains("follow-up check-in from Aarogya Mitra"));
        assertTrue(prompt.contains("PRIMARY LANGUAGE: Hindi (हिन्दी)"));
        assertFalse(prompt.contains("Core Service"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: Marathi prompt includes Marathi follow-up greeting and triage")
    void testFollowUpPromptMarathi() {
        String prompt = promptBuilder.buildFollowUpPrompt(Language.MR, "सुनीता", "fever and cold");

        assertNotNull(prompt);
        assertTrue(prompt.contains("सुनीता"));
        assertTrue(prompt.contains("fever and cold"));
        assertTrue(prompt.contains("नमस्कार सुनीता जी, हे आरोग्य मित्र कडून आपले फॉलो-अप चेक-इन आहे"));
        assertTrue(prompt.contains("PRIMARY LANGUAGE: Marathi (मराठी)"));
        assertTrue(prompt.contains("PROPER NAMES PRESERVATION"));
    }

    @Test
    @DisplayName("Button 2 Follow-up Call: English prompt includes English follow-up greeting and triage")
    void testFollowUpPromptEnglish() {
        String prompt = promptBuilder.buildFollowUpPrompt(Language.EN, "Rahul", "back pain");

        assertNotNull(prompt);
        assertTrue(prompt.contains("Hello Rahul, this is your follow-up check-in from Aarogya Mitra"));
        assertTrue(prompt.contains("back pain"));
        assertTrue(prompt.contains("PRIMARY LANGUAGE: English"));
    }

    @Test
    @DisplayName("Hospital search tool definition generated with valid JSON structure")
    void testHospitalSearchToolDefinition() {
        List<Map<String, Object>> tools = promptBuilder.buildHospitalSearchTool("http://localhost:8089");

        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertTrue(tools.get(0).containsKey("temporaryTool"));

        @SuppressWarnings("unchecked")
        Map<String, Object> tempTool = (Map<String, Object>) tools.get(0).get("temporaryTool");
        assertEquals("searchNearbyHospitals", tempTool.get("modelToolName"));
    }

    @Test
    @DisplayName("Hospital search tool definition strips trailing slashes in webhook base URL")
    void testHospitalSearchToolStripsTrailingSlash() {
        List<Map<String, Object>> tools = promptBuilder.buildHospitalSearchTool("https://my-ngrok-domain.ngrok-free.app///");

        assertNotNull(tools);
        @SuppressWarnings("unchecked")
        Map<String, Object> tempTool = (Map<String, Object>) tools.get(0).get("temporaryTool");
        @SuppressWarnings("unchecked")
        Map<String, Object> httpDetails = (Map<String, Object>) tempTool.get("http");
        assertEquals("https://my-ngrok-domain.ngrok-free.app/api/v1/tools/search-hospitals", httpDetails.get("baseUrlPattern"));
    }

    @Test
    @DisplayName("Independent Follow-up Call: Generates general check-in prompt when no prior summary exists")
    void testFollowUpPromptWithoutPriorSummaryEnglish() {
        String prompt = promptBuilder.buildFollowUpPrompt(Language.EN, "Suresh", null);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Hello Suresh, this is your follow-up check-in from Aarogya Mitra"));
        assertTrue(prompt.contains("General follow-up (no prior health records)"));
        assertTrue(prompt.contains("How are you feeling today, and how may I assist you with your health?"));
        assertFalse(prompt.contains("Context from their previous consultation"));
    }

    @Test
    @DisplayName("Independent Follow-up Call: Generates Hindi general check-in prompt when no prior summary exists")
    void testFollowUpPromptWithoutPriorSummaryHindi() {
        String prompt = promptBuilder.buildFollowUpPrompt(Language.HI, null, null);

        assertNotNull(prompt);
        assertTrue(prompt.contains("आरोग्य मित्र से आपका फॉलो-अप चेक-इन है"));
        assertTrue(prompt.contains("General follow-up (no prior health records)"));
        assertTrue(prompt.contains("आज आपकी तबीयत कैसी है?"));
        assertFalse(prompt.contains("Context from previous consultation"));
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
