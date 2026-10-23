package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import com.ruralhealthcare.medicine.util.ImageValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class TwoTierImageValidationTest {

    private static final byte[] JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01};
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    private static final byte[] WEBP_BYTES = new byte[]{'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};

    @Test
    @DisplayName("Accept valid JPEG image with correct magic bytes")
    void testAcceptValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("image", "med.jpg", "image/jpeg", JPEG_BYTES);
        assertDoesNotThrow(() -> ImageValidator.validate(file));
    }

    @Test
    @DisplayName("Accept valid PNG image with correct magic bytes")
    void testAcceptValidPng() {
        MockMultipartFile file = new MockMultipartFile("image", "med.png", "image/png", PNG_BYTES);
        assertDoesNotThrow(() -> ImageValidator.validate(file));
    }

    @Test
    @DisplayName("Accept valid WEBP image with correct magic bytes")
    void testAcceptValidWebp() {
        MockMultipartFile file = new MockMultipartFile("image", "med.webp", "image/webp", WEBP_BYTES);
        assertDoesNotThrow(() -> ImageValidator.validate(file));
    }

    @Test
    @DisplayName("Reject disallowed MIME type even with image-like name")
    void testRejectDisallowedMimeType() {
        MockMultipartFile file = new MockMultipartFile("image", "prescription.pdf", "application/pdf", JPEG_BYTES);
        MedicationValidationException ex = assertThrows(MedicationValidationException.class, () ->
                ImageValidator.validate(file)
        );
        assertTrue(ex.getMessage().contains("Invalid content type"));
    }

    @Test
    @DisplayName("Reject spoofed image: Content-Type is image/jpeg but content signature is an executable/binary")
    void testRejectSpoofedImageContent() {
        // MZ executable header spoofed as image/jpeg
        byte[] exeBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("image", "malicious.jpg", "image/jpeg", exeBytes);

        MedicationValidationException ex = assertThrows(MedicationValidationException.class, () ->
                ImageValidator.validate(file)
        );
        assertTrue(ex.getMessage().contains("Invalid image content signature"));
    }

    @Test
    @DisplayName("Reject empty image file")
    void testRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(MedicationValidationException.class, () -> ImageValidator.validate(file));
    }
}
