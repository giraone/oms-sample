package com.giraone.oms.sample.service.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.giraone.oms.sample.config.S3Configuration;
import io.findify.s3mock.S3Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.net.URL;

@Service
class AmazonClient {

    private static final Logger logger = LoggerFactory.getLogger(AmazonClient.class);

    private static S3Mock S3MockApi;

    private AmazonS3 s3Client;

    @Autowired
    private S3Configuration s3Configuration;

    @PostConstruct
    private void initialize() {

        logger.info("AmazonClient.initialize isMock={}", s3Configuration.isMock());
        if (s3Configuration.isMock()) {
            initializeS3Mock();
            initializeS3();
        } else {
            initializeS3();
        }
    }

    private void initializeS3() {

        AWSCredentials credentials = new BasicAWSCredentials(s3Configuration.getAccessKey(), s3Configuration.getSecretKey());
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                s3Configuration.getEndpointUrl(), s3Configuration.getClientRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(s3Configuration.isPathStyleAccessEnabled())
                .build();
    }

    private void initializeS3Mock() {

        if (S3MockApi != null) {
            return;
        }

        URL url;
        try {
            url = new URL(s3Configuration.getEndpointUrl());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        S3MockApi = new S3Mock.Builder()
                .withPort(url.getPort())
                .withInMemoryBackend()
                .build();
        S3MockApi.start();
    }

    String getBucketName() {
        return s3Configuration.getBucketName();
    }

    AmazonS3 getS3Client() {
        return s3Client;
    }
}
