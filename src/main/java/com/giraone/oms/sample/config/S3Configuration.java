package com.giraone.oms.sample.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "s3-properties", ignoreUnknownFields = false)
public class S3Configuration {

    private static final Logger logger = LoggerFactory.getLogger(S3Configuration.class);

    private boolean logConfigurationOnStartup = false;
    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String clientRegion;
    private boolean pathStyleAccessEnabled;

    @PostConstruct
    private void initialize() {

        if (this.logConfigurationOnStartup) {
            logger.info("{}", this);
        }
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public boolean isLogConfigurationOnStartup() {
        return logConfigurationOnStartup;
    }

    public void setLogConfigurationOnStartup(boolean logConfigurationOnStartup) {
        this.logConfigurationOnStartup = logConfigurationOnStartup;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getClientRegion() {
        return clientRegion;
    }

    public void setClientRegion(String clientRegion) {
        this.clientRegion = clientRegion;
    }

    public boolean isPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
        this.pathStyleAccessEnabled = pathStyleAccessEnabled;
    }

    @Override
    public String toString() {
        return "S3Configuration{" +
                "endpointUrl='" + endpointUrl + '\'' +
                ", accessKey.length=" + accessKey.length() +
                ", secretKey.length=" + secretKey.length() +
                ", bucketName='" + bucketName + '\'' +
                ", clientRegion='" + clientRegion + '\'' +
                ", pathStyleAccessEnabled=" + pathStyleAccessEnabled +
                '}';
    }
}
