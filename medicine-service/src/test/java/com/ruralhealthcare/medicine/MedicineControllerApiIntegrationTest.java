package com.ruralhealthcare.medicine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import com.ruralhealthcare.medicine.domain.NotificationChannel;
import com.ruralhealthcare.medicine.dto.CaregiverRequest;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.service.CaregiverService;
import com.ruralhealthcare.medicine.service.DoseEventService;
import com.ruralhealthcare.medicine.service.MedicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicineControllerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private DoseEventService doseEventService;

    @Autowired
    private CaregiverService caregiverService;

    private static final byte[] VALID_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Test
    @DisplayName("API: POST /api/v1/medications creates medicine in Mode A (metadata + image)")
    void testCreateMedicationMultipartApi() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest metadata = new CreateMedicationRequest(
                patientId, "Paracetamol 650", "Paracetamol",
                BigDecimal.valueOf(1.0), DosageUnit.TABLET, 2,
                List.of(LocalTime.of(9, 0), LocalTime.of(21, 0)),
                LocalDate.now(), LocalDate.now().plusDays(5),
                FoodInstruction.AFTER_FOOD, "Take after meals"
        );

        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "paracetamol.png", "image/png", VALID_PNG
        );

        mockMvc.perform(multipart("/api/v1/medications")
                        .file(metaPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Paracetamol 650")))
                .andExpect(jsonPath("$.foodInstruction", is("AFTER_FOOD")))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    @DisplayName("API: POST /api/v1/medications with Content-Type application/json (JSON only, no photo)")
    void testCreateMedicationJsonOnlyApi() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId, "Metformin 500mg", null,
                BigDecimal.valueOf(1.0), DosageUnit.TABLET, 2,
                List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                LocalDate.now(), LocalDate.now().plusDays(30),
                FoodInstruction.WITH_FOOD, "Take with breakfast and dinner"
        );

        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Metformin 500mg")))
                .andExpect(jsonPath("$.foodInstruction", is("WITH_FOOD")))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("API: POST /api/v1/medications multipart without image part")
    void testCreateMedicationMultipartWithoutImage() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest metadata = new CreateMedicationRequest(
                patientId, "Atorvastatin 20mg", null,
                BigDecimal.valueOf(1.0), DosageUnit.TABLET, 1,
                List.of(LocalTime.of(22, 0)),
                LocalDate.now(), null,
                FoodInstruction.NONE, "Night"
        );

        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata)
        );

        mockMvc.perform(multipart("/api/v1/medications")
                        .file(metaPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Atorvastatin 20mg")))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("API: POST /api/v1/medications multipart with application/octet-stream metadata part")
    void testCreateMedicationMultipartOctetStreamMetadata() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest metadata = new CreateMedicationRequest(
                patientId, "Omeprazole 20mg", null,
                BigDecimal.valueOf(1.0), DosageUnit.CAPSULE, 1,
                List.of(LocalTime.of(7, 30)),
                LocalDate.now(), null,
                FoodInstruction.EMPTY_STOMACH, "Before breakfast"
        );

        // Submitting with application/octet-stream as curl or Postman does by default
        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "metadata.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                objectMapper.writeValueAsBytes(metadata)
        );

        mockMvc.perform(multipart("/api/v1/medications")
                        .file(metaPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Omeprazole 20mg")))
                .andExpect(jsonPath("$.foodInstruction", is("EMPTY_STOMACH")));
    }

    @Test
    @DisplayName("API: POST /api/v1/medications with unsupported media type returns HTTP 415, not 500")
    void testUnsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<medication><name>Test</name></medication>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.error", is("UNSUPPORTED_MEDIA_TYPE")));

        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.error", is("UNSUPPORTED_MEDIA_TYPE")));
    }

    @Test
    @DisplayName("API: POST /api/v1/medications/extract-ocr returns simulated response with isMock=true")
    void testExtractOcrApi() throws Exception {
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "strip.png", "image/png", VALID_PNG
        );

        mockMvc.perform(multipart("/api/v1/medications/extract-ocr")
                        .file(imagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMock", is(true)))
                .andExpect(jsonPath("$.provider", is("MOCK_SIMULATED")))
                .andExpect(jsonPath("$.rawExtractedText", containsString("[SIMULATED OCR]")))
                .andExpect(jsonPath("$.confidence").doesNotExist());
    }

    @Test
    @DisplayName("API: Complete and discontinue medication course")
    void testCompleteAndDiscontinueEndpoints() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest metadata = new CreateMedicationRequest(
                patientId, "Cough Syrup", null,
                BigDecimal.valueOf(10.0), DosageUnit.ML, 1,
                List.of(LocalTime.of(21, 0)),
                LocalDate.now(), null,
                FoodInstruction.NONE, "Night"
        );
        MedicationResponse created = medicationService.createMedication(metadata, null);

        mockMvc.perform(put("/api/v1/medications/{id}/complete", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(put("/api/v1/medications/{id}/discontinue", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISCONTINUED")));
    }

    @Test
    @DisplayName("API: POST /api/v1/caregivers and GET /api/v1/caregivers/patient/{patientId}")
    void testCaregiverEndpoints() throws Exception {
        UUID patientId = UUID.randomUUID();
        CaregiverRequest request = new CaregiverRequest(
                patientId, "Anita Patel", "Daughter", "+919811223344", "anita@example.com", NotificationChannel.EMAIL
        );

        mockMvc.perform(post("/api/v1/caregivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Anita Patel")))
                .andExpect(jsonPath("$.preferredChannel", is("EMAIL")));

        mockMvc.perform(get("/api/v1/caregivers/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Anita Patel")));
    }

    @Test
    @DisplayName("API: GET /api/v1/caregivers/escalations/patient/{patientId} returns escalation audit history")
    void testGetEscalationHistoryApi() throws Exception {
        UUID patientId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/caregivers/escalations/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));
    }

    @Test
    @DisplayName("API: Malformed JSON syntax returns HTTP 400 Bad Request, not HTTP 500")
    void testMalformedJsonSyntaxReturns400() throws Exception {
        String malformedJson = "{ \"patientId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\" \"name\": \"test\" }";

        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("API: Invalid UUID string format returns HTTP 400 Bad Request, not HTTP 500")
    void testInvalidUuidFormatReturns400() throws Exception {
        String invalidUuidJson = "{\n" +
                "  \"patientId\": \"1fffbb55-c260-4b8c-ad3b-1153a7ceb7f7D\",\n" +
                "  \"name\": \"Paracetamol\",\n" +
                "  \"dosageAmount\": 1.0,\n" +
                "  \"dosageUnit\": \"TABLET\",\n" +
                "  \"timesPerDay\": 1,\n" +
                "  \"reminderTimes\": [\"08:00:00\"],\n" +
                "  \"startDate\": \"2026-10-12\"\n" +
                "}";

        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUuidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("patientId")));
    }

    @Test
    @DisplayName("API: Create medicine with arbitrary number of reminder times (e.g. 3 times a day)")
    void testCreateMedicineWithMultipleReminderTimes() throws Exception {
        UUID patientId = UUID.randomUUID();
        String jsonPayload = "{\n" +
                "  \"patientId\": \"" + patientId + "\",\n" +
                "  \"name\": \"Amoxicillin 500mg\",\n" +
                "  \"dosageAmount\": 1.0,\n" +
                "  \"dosageUnit\": \"CAPSULE\",\n" +
                "  \"timesPerDay\": 3,\n" +
                "  \"reminderTimes\": [\"08:00:00\", \"14:00:00\", \"20:00:00\"],\n" +
                "  \"startDate\": \"2026-10-12\",\n" +
                "  \"foodInstruction\": \"AFTER_FOOD\"\n" +
                "}";

        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Amoxicillin 500mg")))
                .andExpect(jsonPath("$.timesPerDay", is(3)))
                .andExpect(jsonPath("$.reminderTimes", hasSize(3)))
                .andExpect(jsonPath("$.reminderTimes[0]", is("08:00:00")))
                .andExpect(jsonPath("$.reminderTimes[1]", is("14:00:00")))
                .andExpect(jsonPath("$.reminderTimes[2]", is("20:00:00")));
    }
}
