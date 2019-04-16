package com.giraone.oms.sample.service.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.giraone.oms.sample.config.StorageConfiguration;
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
    private StorageConfiguration storageConfiguration;

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

    @Override
    public long storeFromStream(InputStream inputStream, long contentLength, String path) {

        ContentLengthCalculator cc = null;
        if (contentLength < 0) {
            logger.warn("Must calculate content-length of " + path);
            cc = new ContentLengthCalculator(inputStream);
            inputStream = cc.getInputStream();
            contentLength = cc.getContentLength();
        }

        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(contentLength);
            PutObjectRequest request = new PutObjectRequest(this.amazonClient.getBucketName(), path, inputStream, objectMetadata);
            // request.withCannedAcl(CannedAccessControlList.PublicRead);
            PutObjectResult result = this.amazonClient.getS3Client().putObject(request);
            return result.getMetadata().getContentLength();
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
        if (!this.isValidFileName(fileName)) {
            throw new StorageException("Sorry! Path contains invalid path sequence " + fileName);
        }
        try (InputStream inputStream = file.getInputStream()) {
            this.storeFromStream(inputStream, -1, fileName);
            return fileName;
        } catch (IOException e) {
            throw new StorageException("Error reading multipart filesystem \"" + file.getName() + "\".", e);
        }
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

    public URL createPreSignedUr(String bucketName, String objectKey, HttpMethod httpMethod, int expireHour) {

        // Set the pre-signed URL to expire after one hour.
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * expireHour;
        expiration.setTime(expTimeMillis);

        // Generate the pre-signed URL.
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(httpMethod)
                        .withExpiration(expiration);
        URL url = this.amazonClient.getS3Client().generatePresignedUrl(generatePresignedUrlRequest);

        System.out.println("Pre-Signed URL: " + url.toString());
        return url;
    }
}
