package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.exception.StorageException;
import com.ruralhealthcare.medicine.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Strict development fallback implementation of StorageService.
 * Used only when AWS S3 credentials are not configured in local environment.
 */
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    private final Path baseStoragePath;

    public LocalStorageServiceImpl(MedicineProperties properties) {
        String dir = properties.getStorage().getLocal().getDirectory();
        this.baseStoragePath = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseStoragePath);
            log.info("Initialized Local Dev Storage fallback at directory: {}", baseStoragePath);
        } catch (IOException e) {
            log.error("Failed to create local storage directory [{}]: {}", baseStoragePath, e.getMessage());
            throw new StorageException("Could not initialize local storage directory", e);
        }
    }

    @Override
    public String storeImage(MultipartFile file, UUID patientId) {
        if (file.isEmpty()) {
            throw new StorageException("Cannot upload empty image file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String filename = String.format("%s_%s%s", patientId, UUID.randomUUID(), extension);
        Path targetPath = baseStoragePath.resolve(filename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[LOCAL-STORAGE-DEV] Saved medicine image to: {}", targetPath);
            return filename;
        } catch (IOException e) {
            log.error("Failed to store image locally: {}", e.getMessage(), e);
            throw new StorageException("Failed to save image file locally", e);
        }
    }

    @Override
    public String getPresignedImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }
        // In local development, serve via static resource handler
        return "/storage/medicine-images/" + imageKey;
    }

    @Override
    public void deleteImage(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return;
        }
        try {
            Path targetPath = baseStoragePath.resolve(imageKey);
            Files.deleteIfExists(targetPath);
            log.info("[LOCAL-STORAGE-DEV] Deleted file: {}", targetPath);
        } catch (IOException e) {
            log.warn("Failed to delete local file [{}]: {}", imageKey, e.getMessage());
        }
    }
}
