package com.giraone.oms.sample.service.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.giraone.oms.sample.service.ContentLengthCalculator;
import com.giraone.oms.sample.service.StorageException;
import com.giraone.oms.sample.service.StorageService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    @Autowired
    private AmazonClient amazonClient;

    @PostConstruct
    private void initialize() {

        AmazonS3 s3Client = this.amazonClient.getS3Client();
        String bucketName = this.amazonClient.getBucketName();
        if (!s3Client.doesBucketExistV2(bucketName)) {

            s3Client.createBucket(new CreateBucketRequest(bucketName));

            // Verify that the bucket was created by retrieving it and checking its location.
            String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
            System.out.println("New bucket in location: " + bucketLocation);
        }
    }

    @Override
    public void transferToStream(String path, OutputStream outputStream) {
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60);
        URL url = this.amazonClient.getS3Client().generatePresignedUrl(this.amazonClient.getBucketName(), path, expiration);
        try {
            URLConnection uc = url.openConnection();
            try (InputStream in = uc.getInputStream()) {
                IOUtils.copy(in, outputStream);
            }
        } catch (IOException e) {
            throw new StorageException("Cannot stream from S3 path " + path, e);
        }
    }

    /**
     * Store the BLOB defined by reading an input stream in S3 using "path" as the object key.
     * @param inputStream   The input stream to read the content from
     * @param contentType   The content type of the BLOB
     * @param contentLength The content length or null, if the content length is unknown by the caller.
     *                      In this case the content length will be calculated by a reading!
     * @param path          The object key to be used as the path to the object.
     * @return the byte length of the content that was stored
     */
    @Override
    public long storeFromStream(InputStream inputStream, String contentType, Long contentLength, String path) {

        ContentLengthCalculator cc = null;
        if (contentLength == null || contentLength < 0) {
            logger.warn("Content-length not given! Must calculate content-length of " + path);
            cc = new ContentLengthCalculator(inputStream);
            inputStream = cc.getInputStream();
            contentLength = cc.getContentLength();
        }

        try {
            final ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(contentLength);
            objectMetadata.setContentType(contentType);
            PutObjectRequest request = new PutObjectRequest(this.amazonClient.getBucketName(), path, inputStream, objectMetadata);
            // request.withCannedAcl(CannedAccessControlList.PublicRead);
            this.amazonClient.getS3Client().putObject(request);
            return objectMetadata.getContentLength();
        } finally {
            if (cc != null) {
                cc.clean();
            }
        }
    }

    @Override
    public String storeMultipartFile(MultipartFile file) {

        // Normalize filesystem name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!this.isValidPathName(fileName)) {
            throw new StorageException("Sorry! Path contains invalid path sequence " + fileName);
        }
        try (InputStream inputStream = file.getInputStream()) {
            this.storeFromStream(inputStream, file.getContentType(), (long) file.getBytes().length, fileName);
            return fileName;
        } catch (IOException e) {
            throw new StorageException("Error reading multipart filesystem \"" + file.getName() + "\".", e);
        }
    }

    @Override
    public boolean exists(String path) {

        return this.amazonClient.getS3Client().doesObjectExist(this.amazonClient.getBucketName(), path);
    }

    @Override
    public boolean delete(String path) {

        if (this.amazonClient.getS3Client().doesObjectExist(this.amazonClient.getBucketName(), path)) {
            DeleteObjectRequest request = new DeleteObjectRequest(this.amazonClient.getBucketName(), path);
            this.amazonClient.getS3Client().deleteObject(request);
            return true;
        } else {
            return false;
        }
    }

    public URL createPreSignedUrl(String bucketName, String objectKey, HttpMethod httpMethod,
                                  int expireHour, int cacheControlSeconds) {

        // Set the pre-signed URL to expire after n hours.
        final Date expiration = new Date();
        final long expTimeMillis = expiration.getTime() + 1000 * 60 * 60 * expireHour;
        expiration.setTime(expTimeMillis);

        final ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        if (cacheControlSeconds > 0) {
            responseHeaders.setCacheControl("max-age=" + cacheControlSeconds);
        }

        // Generate the pre-signed URL.
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(httpMethod)
                        .withResponseHeaders(responseHeaders)
                        .withExpiration(expiration);
        URL url = this.amazonClient.getS3Client().generatePresignedUrl(generatePresignedUrlRequest);
        logger.debug("S3StorageService.createPreSignedUrl: {} {} {} -> {}", bucketName, objectKey, httpMethod.name(), url.toString());
        return url;
    }
}
