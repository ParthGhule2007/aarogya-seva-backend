package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.dto.OcrExtractionResponse;
import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import com.ruralhealthcare.medicine.service.OcrService;
import com.ruralhealthcare.medicine.service.impl.MockOcrServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class TransparentMockOcrTest {

    private final OcrService ocrService = new MockOcrServiceImpl();
    private static final byte[] VALID_PNG_HEADER = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Test
    @DisplayName("Mock OCR transparently flags isMock=true and avoids fabricated confidence scores")
    void testMockOcrTransparency() {
        MockMultipartFile image = new MockMultipartFile("image", "strip.png", "image/png", VALID_PNG_HEADER);

        OcrExtractionResponse response = ocrService.extractText(image);

        assertTrue(response.isMock(), "isMock flag must be true");
        assertNull(response.getConfidence(), "Mock OCR must not return fabricated confidence score");
        assertEquals("MOCK_SIMULATED", response.getProvider());
        assertTrue(response.getRawExtractedText().contains("[SIMULATED OCR]"),
                "Raw extracted text must state simulated status");
        assertNotNull(response.getDisclaimer(), "Disclaimer explaining manual verification requirement must be present");
    }

    @Test
    @DisplayName("Mock OCR validates image format before simulating text extraction")
    void testMockOcrEnforcesImageValidation() {
        // Invalid file signature
        byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        MockMultipartFile invalidFile = new MockMultipartFile("image", "corrupt.jpg", "image/jpeg", invalidBytes);

        assertThrows(MedicationValidationException.class, () ->
                ocrService.extractText(invalidFile)
        );
    }
}
