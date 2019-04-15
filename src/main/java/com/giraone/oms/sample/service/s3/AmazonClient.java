package com.giraone.oms.sample.service.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.giraone.oms.sample.config.S3Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
class AmazonClient {

    private AmazonS3 s3Client;

    @Autowired
    private S3Configuration s3Configuration;

    @PostConstruct
    private void initialize() {
        AWSCredentials credentials = new BasicAWSCredentials(s3Configuration.getAccessKey(), s3Configuration.getSecretKey());
        this.s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                            s3Configuration.getEndpointUrl(), s3Configuration.getClientRegion()))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withPathStyleAccessEnabled(s3Configuration.isPathStyleAccessEnabled())
            .build();
    }

    String getBucketName() {
        return s3Configuration.getBucketName();
    }

    AmazonS3 getS3Client() {
        return s3Client;
    }
}
