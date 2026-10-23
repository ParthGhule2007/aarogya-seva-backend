package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.exception.StorageException;
import com.ruralhealthcare.medicine.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Production implementation of StorageService using AWS S3.
 * Stores medicine photos in a private S3 bucket and generates temporary presigned URLs.
 */
public class S3StorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final int presignedExpiryMinutes;

    public S3StorageServiceImpl(MedicineProperties properties) {
        MedicineProperties.Storage.S3 s3Config = properties.getStorage().getS3();
        this.bucketName = s3Config.getBucket();
        this.presignedExpiryMinutes = s3Config.getPresignedExpiryMinutes();
        Region region = Region.of(s3Config.getRegion());

        if (StringUtils.hasText(s3Config.getAccessKey()) && StringUtils.hasText(s3Config.getSecretKey())) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey());
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

            this.s3Client = S3Client.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .build();
        } else {
            // Default credential provider chain
            this.s3Client = S3Client.builder().region(region).build();
            this.s3Presigner = S3Presigner.builder().region(region).build();
        }

        log.info("Initialized AWS S3 Storage Service targeting private bucket [{}] in region [{}]", bucketName, region);
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

        String key = String.format("patients/%s/medications/%s%s", patientId, UUID.randomUUID(), extension);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Successfully uploaded medicine photo to private S3 bucket. Key: {}", key);
            return key;
        } catch (IOException e) {
            log.error("Failed to read image stream for S3 upload: {}", e.getMessage(), e);
            throw new StorageException("Failed to read image content for upload", e);
        } catch (Exception e) {
            log.error("Failed to upload image to S3 bucket [{}]: {}", bucketName, e.getMessage(), e);
            throw new StorageException("Failed to store image in S3", e);
        }
    }

    @Override
    public String getPresignedImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedExpiryMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned S3 URL for key [{}]: {}", imageKey, e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteImage(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted S3 object [{}] from bucket [{}]", imageKey, bucketName);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object [{}]: {}", imageKey, e.getMessage());
        }
    }
}
