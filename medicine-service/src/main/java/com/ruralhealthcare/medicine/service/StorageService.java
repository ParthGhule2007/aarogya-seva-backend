package com.ruralhealthcare.medicine.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface StorageService {

    /**
     * Stores the raw uploaded medicine image and returns the unique storage object key.
     */
    String storeImage(MultipartFile file, UUID patientId);

    /**
     * Generates a temporary presigned URL for displaying the private medicine photograph.
     */
    String getPresignedImageUrl(String imageKey);

    /**
     * Deletes the stored image object when medication is deleted.
     */
    void deleteImage(String imageKey);
}
