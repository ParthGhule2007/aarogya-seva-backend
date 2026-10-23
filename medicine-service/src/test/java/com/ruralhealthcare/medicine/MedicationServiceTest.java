package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.DosageUnit;
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
class MedicationServiceTest {

    @Autowired
    private MedicationService medicationService;

    @Test
    @DisplayName("Create Medication successfully with photo and schedule")
    void testCreateMedicationSuccess() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image", "paracetamol.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                2,
                List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                "Take after food"
        );

        MedicationResponse response = medicationService.createMedication(request, image);

        assertNotNull(response.getId());
        assertEquals(patientId, response.getPatientId());
        assertEquals(BigDecimal.valueOf(1.0), response.getDosageAmount());
        assertEquals(DosageUnit.TABLET, response.getDosageUnit());
        assertEquals(2, response.getTimesPerDay());
        assertEquals(2, response.getReminderTimes().size());
        assertEquals(MedicationStatus.ACTIVE, response.getStatus());
        assertNotNull(response.getImageUrl(), "Image URL must be populated");
    }

    @Test
    @DisplayName("Reject Medication creation if photograph is missing")
    void testRejectMissingImage() {
        UUID patientId = UUID.randomUUID();
        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(9, 0)),
                LocalDate.now(),
                null,
                null
        );

        assertThrows(MedicationValidationException.class, () ->
                medicationService.createMedication(request, null)
        );
    }

    @Test
    @DisplayName("Reject Medication creation if dosage is zero or negative")
    void testRejectInvalidDosage() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "med.jpg", "image/jpeg", "data".getBytes());

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.ZERO,
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(9, 0)),
                LocalDate.now(),
                null,
                null
        );

        assertThrows(MedicationValidationException.class, () ->
                medicationService.createMedication(request, image)
        );
    }
}
