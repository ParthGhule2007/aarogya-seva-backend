package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.domain.DoseStatus;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.DoseEventResponse;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.exception.InvalidStateTransitionException;
import com.ruralhealthcare.medicine.service.DoseEventService;
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
class DoseEventLifecycleTest {

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private DoseEventService doseEventService;

    @Test
    @DisplayName("Dose event lifecycle: PENDING -> TAKEN")
    void testMarkDoseTaken() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "med.jpg", "image/jpeg", "data".getBytes());

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(10, 0)),
                LocalDate.now(),
                null,
                null
        );

        MedicationResponse med = medicationService.createMedication(request, image);
        List<DoseEventResponse> todayDoses = doseEventService.getTodayDosesForPatient(patientId);

        assertFalse(todayDoses.isEmpty(), "Today doses should be generated");
        DoseEventResponse dose = todayDoses.get(0);
        assertEquals(DoseStatus.PENDING, dose.getStatus());

        DoseEventResponse taken = doseEventService.markDoseTaken(dose.getId());
        assertEquals(DoseStatus.TAKEN, taken.getStatus());
        assertNotNull(taken.getTakenAt());

        // Cannot mark TAKEN dose as MISSED
        assertThrows(InvalidStateTransitionException.class, () ->
                doseEventService.markDoseMissed(dose.getId())
        );
    }

    @Test
    @DisplayName("Consecutive missed doses tracking")
    void testConsecutiveMissedDoses() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "med.jpg", "image/jpeg", "data".getBytes());

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(12, 0)),
                LocalDate.now(),
                null,
                null
        );

        MedicationResponse med = medicationService.createMedication(request, image);
        List<DoseEventResponse> todayDoses = doseEventService.getTodayDosesForPatient(patientId);

        DoseEventResponse dose = todayDoses.get(0);
        doseEventService.markDoseMissed(dose.getId());

        int missedCount = doseEventService.countConsecutiveMissedDoses(med.getId());
        assertEquals(1, missedCount);
    }
}
