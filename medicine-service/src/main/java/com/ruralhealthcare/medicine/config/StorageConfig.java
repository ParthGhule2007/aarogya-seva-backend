package com.ruralhealthcare.medicine.config;

import com.ruralhealthcare.medicine.service.StorageService;
import com.ruralhealthcare.medicine.service.impl.LocalStorageServiceImpl;
import com.ruralhealthcare.medicine.service.impl.S3StorageServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    private final MedicineProperties properties;

    public StorageConfig(MedicineProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public StorageService storageService() {
        String provider = properties.getStorage().getProvider();
        MedicineProperties.Storage.S3 s3 = properties.getStorage().getS3();

        boolean hasAwsConfig = StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey());

        if ("s3".equalsIgnoreCase(provider) || ("auto".equalsIgnoreCase(provider) && hasAwsConfig)) {
            log.info("Using AWS S3 Storage Provider for private medicine photographs with presigned URLs");
            return new S3StorageServiceImpl(properties);
        } else {
            log.info("Using Local Disk Storage strictly as development fallback. (Production requires S3 bucket configuration)");
            return new LocalStorageServiceImpl(properties);
        }
    }
}
