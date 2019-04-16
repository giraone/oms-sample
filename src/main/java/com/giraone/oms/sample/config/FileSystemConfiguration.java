package com.giraone.oms.sample.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;

@Component
@ConfigurationProperties(prefix = "filesystem-properties", ignoreUnknownFields = false)
public class FileSystemConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemConfiguration.class);

    private boolean logConfigurationOnStartup = false;
    private String storageDirectory = null;

    @PostConstruct
    private void initialize() {

        if (this.storageDirectory == null) {
            try {
                this.storageDirectory = Files.createTempDirectory("").toFile().getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                this.storageDirectory = ".";
            }
        }
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

    public String getStorageDirectory() {
        return storageDirectory;
    }

    public void setStorageDirectory(String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    public String toString() {
        return "StorageConfiguration{" +
                "logConfigurationOnStartup=" + logConfigurationOnStartup +
                ", storageDirectory='" + storageDirectory + '\'' +
                '}';
    }
}
