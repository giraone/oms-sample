package com.giraone.oms.sample.config;

import com.giraone.oms.sample.service.StorageService;
import com.giraone.oms.sample.service.filesystem.FileSystemStorageService;
import com.giraone.oms.sample.service.s3.S3StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class AppConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app-config.storage.storage-impl", name = "filesystem")
    public StorageService getFileStorageService() {

        return new FileSystemStorageService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app-config.storage.storage-impl", name = "s3", matchIfMissing = true)
    public StorageService getS3StorageService() {

        try {
            return new S3StorageService();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
