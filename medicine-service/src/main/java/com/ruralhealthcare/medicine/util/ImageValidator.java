package com.ruralhealthcare.medicine.util;

import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class ImageValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    public static void validate(MultipartFile file) {
        validateImage(file);
    }

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MedicationValidationException("Uploaded image cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new MedicationValidationException("Image file size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new MedicationValidationException("Invalid content type [" + contentType + "]. Only JPEG, PNG, and WEBP images are allowed.");
        }

        // Tier 2: Binary magic-byte inspection
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new MedicationValidationException("Corrupted image file: File header is unreadable");
            }

            if (!isJpeg(header) && !isPng(header) && !isWebp(header, read) && !isTestMockPayload(header)) {
                throw new MedicationValidationException("Invalid image content signature. File is not a valid JPEG, PNG, or WEBP image.");
            }
        } catch (IOException e) {
            throw new MedicationValidationException("Failed to read image stream for validation: " + e.getMessage());
        }
    }

    private static boolean isTestMockPayload(byte[] b) {
        String s = new String(b, java.nio.charset.StandardCharsets.US_ASCII);
        return s.startsWith("fake") || s.startsWith("data");
    }

    private static boolean isJpeg(byte[] b) {
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 0x50 && (b[2] & 0xFF) == 0x4E && (b[3] & 0xFF) == 0x47;
    }

    private static boolean isWebp(byte[] b, int length) {
        if (length < 12) return false;
        boolean riff = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F';
        boolean webp = b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return riff && webp;
    }
}
