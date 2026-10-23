package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.FoodInstruction;
import com.ruralhealthcare.medicine.domain.MedicationStatus;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import com.ruralhealthcare.medicine.service.MedicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LegacyAndNewMedicationCreationTest {

    @Autowired
    private MedicationService medicationService;

    private static final byte[] VALID_JPEG_HEADER = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01};

    @Test
    @DisplayName("Legacy format: creation without name field defaults safely to 'Prescribed Medicine'")
    void testLegacyMedicationCreationWithoutName() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "legacy-med.jpg", "image/jpeg", VALID_JPEG_HEADER);

        // Legacy request using the 8-parameter constructor without name or foodInstruction
        CreateMedicationRequest legacyRequest = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(2.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(9, 0)),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Take with water"
        );

        MedicationResponse response = medicationService.createMedication(legacyRequest, image);

        assertNotNull(response.getId());
        assertEquals("Prescribed Medicine", response.getName(), "Legacy entries must safely fallback to 'Prescribed Medicine'");
        assertEquals(FoodInstruction.NONE, response.getFoodInstruction());
        assertEquals(MedicationStatus.ACTIVE, response.getStatus());
        assertNotNull(response.getImageUrl());
    }

    @Test
    @DisplayName("New format Mode A: creation with custom name, food instruction, and image")
    void testNewFormatModeAWithImageAndCustomName() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "amoxicillin.jpg", "image/jpeg", VALID_JPEG_HEADER);

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                "Amoxicillin 500mg",
                "Amoxicillin",
                BigDecimal.valueOf(1.0),
                DosageUnit.CAPSULE,
                3,
                List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                FoodInstruction.AFTER_FOOD,
                "Complete full antibiotics course"
        );

        MedicationResponse response = medicationService.createMedication(request, image);

        assertNotNull(response.getId());
        assertEquals("Amoxicillin 500mg", response.getName());
        assertEquals("Amoxicillin", response.getExtractedName());
        assertEquals(FoodInstruction.AFTER_FOOD, response.getFoodInstruction());
        assertNotNull(response.getImageUrl());
        assertEquals(MedicationStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("New format Mode C: creation with custom name and NO photograph")
    void testNewFormatModeCWithoutPhoto() {
        UUID patientId = UUID.randomUUID();

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                "Metformin 850mg",
                null,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                2,
                List.of(LocalTime.of(8, 30), LocalTime.of(20, 30)),
                LocalDate.now(),
                null,
                FoodInstruction.WITH_FOOD,
                "Take during meal"
        );

        MedicationResponse response = medicationService.createMedication(request, null);

        assertNotNull(response.getId());
        assertEquals("Metformin 850mg", response.getName());
        assertEquals(FoodInstruction.WITH_FOOD, response.getFoodInstruction());
        assertNull(response.getImageKey(), "Image key must be null for Mode C");
        assertNull(response.getImageUrl(), "Image URL must be null for Mode C");
    }

    @Test
    @DisplayName("Reject medication creation if both name and photograph are missing")
    void testRejectWhenBothNameAndImageMissing() {
        UUID patientId = UUID.randomUUID();

        CreateMedicationRequest request = new CreateMedicationRequest();
        request.setPatientId(patientId);
        request.setDosageAmount(BigDecimal.valueOf(1.0));
        request.setDosageUnit(DosageUnit.TABLET);
        request.setTimesPerDay(1);
        request.setReminderTimes(List.of(LocalTime.of(10, 0)));
        request.setStartDate(LocalDate.now());
        // Name is null, image is null

        assertThrows(MedicationValidationException.class, () ->
                medicationService.createMedication(request, null)
        );
    }

    @Test
    @DisplayName("Support course lifecycle: complete and discontinue medication")
    void testCompleteAndDiscontinueMedication() {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                "Azithromycin",
                null,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(9, 0)),
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                FoodInstruction.EMPTY_STOMACH,
                "1 hour before meal"
        );

        MedicationResponse created = medicationService.createMedication(request, null);
        assertEquals(MedicationStatus.ACTIVE, created.getStatus());

        MedicationResponse completed = medicationService.completeMedication(created.getId());
        assertEquals(MedicationStatus.COMPLETED, completed.getStatus());

        MedicationResponse discontinued = medicationService.discontinueMedication(created.getId());
        assertEquals(MedicationStatus.DISCONTINUED, discontinued.getStatus());
    }
}
