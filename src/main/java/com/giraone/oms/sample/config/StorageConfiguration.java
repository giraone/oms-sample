package com.giraone.oms.sample.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "app-config.storage", ignoreUnknownFields = false)
public class StorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);

    public enum StorageImplEnum {
        file, s3
    }

    private boolean logConfigurationOnStartup = false;
    private StorageImplEnum storageImpl = StorageImplEnum.s3;

    @PostConstruct
    private void initialize() {

        if (this.logConfigurationOnStartup) {
            logger.info("{}", this);
        }
    }

    public boolean isLogConfigurationOnStartup() {
        return logConfigurationOnStartup;
    }

    public void setLogConfigurationOnStartup(boolean logConfigurationOnStartup) {
        this.logConfigurationOnStartup = logConfigurationOnStartup;
    }

    public StorageImplEnum getStorageImpl() {
        return storageImpl;
    }

    public void setStorageImpl(StorageImplEnum storageImpl) {
        this.storageImpl = storageImpl;
    }

    @Override
    public String toString() {
        return "StorageConfiguration{" +
                "logConfigurationOnStartup=" + logConfigurationOnStartup +
                ", storageImpl=" + storageImpl +
                '}';
    }
}
