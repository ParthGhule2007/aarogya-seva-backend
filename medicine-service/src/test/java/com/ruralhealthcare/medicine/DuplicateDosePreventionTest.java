package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.domain.DosageUnit;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.DoseEventResponse;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class DuplicateDosePreventionTest {

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private DoseEventService doseEventService;

    @Test
    @DisplayName("Prevent duplicate dose events for same medication, date, and time")
    void testDuplicateDosePrevention() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("image", "med.jpg", "image/jpeg", "data".getBytes());

        CreateMedicationRequest request = new CreateMedicationRequest(
                patientId,
                BigDecimal.valueOf(1.0),
                DosageUnit.TABLET,
                1,
                List.of(LocalTime.of(14, 0)),
                LocalDate.now(),
                null,
                null
        );

        MedicationResponse med = medicationService.createMedication(request, image);

        // Call generateTodayDoseEvents multiple times
        doseEventService.generateTodayDoseEvents();
        doseEventService.generateTodayDoseEvents();
        doseEventService.generateTodayDoseEvents();

        List<DoseEventResponse> todayDoses = doseEventService.getTodayDosesForPatient(patientId);
        assertEquals(1, todayDoses.size(), "Must strictly have exactly 1 dose event generated, duplicate creation prevented");
    }
}
