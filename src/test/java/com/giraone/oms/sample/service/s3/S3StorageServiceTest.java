package com.giraone.oms.sample.service.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.giraone.oms.sample.config.S3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class S3StorageServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceTest.class);

    @Autowired
    private S3Configuration s3Configuration;

    @Autowired
    private AmazonClient amazonClient;

    @Autowired
    private S3StorageService s3StorageService;

    @Before
    public void initS3() {

        logger.info("initS3 with {}", s3Configuration);
        final String bucketName = s3Configuration.getBucketName();
        amazonClient.getS3Client().createBucket(bucketName);
    }

    @After
    public void purgeS3() {

        logger.info("purgeS3 with {}", s3Configuration);
        final String bucketName = s3Configuration.getBucketName();
        AmazonS3 s3 = amazonClient.getS3Client();
        if (!s3.doesBucketExistV2(bucketName)) {
            return;
        }
        s3.listObjects(bucketName).getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .forEach(objectKey -> s3.deleteObject(bucketName, objectKey));
        s3.deleteBucket(bucketName);
    }

    @Test
    public void testThat_storeFromStream_and_transferToStream_works() {

        // arrange
        byte[] bytesToStore = "abcdefghijklmnopqrstuvwxyzäöü".getBytes(Charset.forName("UTF-8"));
        InputStream inputStream = new ByteArrayInputStream(bytesToStore);
        long contentLength = bytesToStore.length;
        String path = "path1/path2/" + UUID.randomUUID();

        // act
        long storedContentLength = s3StorageService.storeFromStream(
                inputStream, "application/octet-stream", contentLength, path);

        // assert
        assertThat(storedContentLength).isEqualTo(contentLength);

        // act
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        s3StorageService.transferToStream(path, outputStream);

        // assert
        assertThat(outputStream.toByteArray()).isEqualTo(bytesToStore);
    }

    @Test
    public void testThat_delete_works() {

        // arrange
        byte[] bytesToStore = "abcdefghijklmnopqrstuvwxyzäöü".getBytes(Charset.forName("UTF-8"));
        InputStream inputStream = new ByteArrayInputStream(bytesToStore);
        long contentLength = bytesToStore.length;
        String path = "path1/path2/" + UUID.randomUUID();
        s3StorageService.storeFromStream(inputStream, "application/octet-stream", contentLength, path);
        assertThat(s3StorageService.exists(path)).isTrue();

        // act
        boolean done = s3StorageService.delete(path);

        // assert
        assertThat(done).isTrue();
        assertThat(s3StorageService.exists(path)).isFalse();
    }

    @Test
    public void testThat_createPreSignedUrl_works() {

        // arrange
        String bucketName = "bucket-01";
        String objectKey = "path1/path2/" + UUID.randomUUID();
        HttpMethod httpMethod = HttpMethod.GET;
        int expireHour = 1;
        int cacheControlSeconds = 60 * 5;

        // act
        URL url = s3StorageService.createPreSignedUrl(bucketName, objectKey, httpMethod, expireHour, cacheControlSeconds);
        System.out.println(bucketName + "/" + objectKey + " -> \"" + url.toExternalForm() + "\"");

        // assert
        assertThat(url).isNotNull();
        assertThat(url.toString()).startsWith("http");
        assertThat(url.toString()).contains(objectKey);
    }

    @Test
    public void testThat_createPreSignedUrl_is_fast() {

        // arrange
        int repetitions = 1000;
        String bucketName = "bucket-01";
        HttpMethod httpMethod = HttpMethod.GET;
        int expireHour = 1;
        int cacheControlSeconds = 60 * 5;
        List<String> objectKeys = IntStream.rangeClosed(1, repetitions)
                .mapToObj(i -> String.format("path1/path-%010d/%s", i, UUID.randomUUID()))
                .collect(Collectors.toList());

        // act
        long start = System.currentTimeMillis();
        for (String objectKey : objectKeys) {
            URL url = s3StorageService.createPreSignedUrl(bucketName, objectKey, httpMethod, expireHour, cacheControlSeconds);
        }
        long end = System.currentTimeMillis();
        long duration = end - start;
        logger.info("Signing {} URLs took {} ms", repetitions, duration);

        // assert
        assertThat(duration).isLessThan(repetitions); // less than one ms per signing
    }
}
