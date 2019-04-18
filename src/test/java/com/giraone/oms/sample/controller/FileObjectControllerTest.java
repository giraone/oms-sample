package com.giraone.oms.sample.controller;

import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileObjectControllerTest {

    private static final File TEST_FILES_DIR = new File("src/test/resources/testfiles");

    private static final String TEST_FILE_JPEG_01 = "image-01.jpg";
    private static final String TEST_FILE_JPEG_02 = "image-02.jpg";

    private static final String TEST_FILE_JPEG_01_MD5 = "db22fe1cb26762b78ff730744df2cc8c";
    private static final String TEST_FILE_JPEG_02_MD5 = "84156ecc6016f3f945b9e0d09dfd534a";

    private UsernamePasswordCredentials userCredentials =
            new UsernamePasswordCredentials("user", "user");

    @LocalServerPort
    private int randomServerPort;

    @BeforeClass
    public static void init() {

    }

    @Test
    public void testThat_file_put_isWorking() throws Exception {

        // arrange
        String uuid = UUID.randomUUID().toString();
        File testFile = new File(TEST_FILES_DIR, TEST_FILE_JPEG_01);
        String url = this.buildUrl(uuid);

        // act
        int httpCodePut = this.putFromFile(url, testFile);

        // assert
        assertThat(httpCodePut).isBetween(200, 201);
    }

    @Test
    public void testThat_file_get_isWorking() throws Exception {

        // arrange - upload a file
        String uuid = UUID.randomUUID().toString();
        File testFile = new File(TEST_FILES_DIR, TEST_FILE_JPEG_01);
        String url = this.buildUrl(uuid);
        this.putFromFile(url, testFile);
        int httpCode = this.putFromFile(url, testFile);
        assertThat(httpCode).isBetween(200, 201);

        File tempFile = File.createTempFile("test", ".jpg");
        tempFile.deleteOnExit();

        // act
        long fileSize = this.getToFile(url, tempFile);

        // assert
        assertThat(fileSize).isEqualTo(testFile.length());
        assertThat(tempFile).hasDigest("MD5", TEST_FILE_JPEG_01_MD5);
    }

    @Test
    public void testThat_file_delete_isWorking() throws Exception {

        // arrange - upload a file
        String uuid = UUID.randomUUID().toString();
        File testFile = new File(TEST_FILES_DIR, TEST_FILE_JPEG_01);
        String url = this.buildUrl(uuid);
        this.putFromFile(url, testFile);
        int httpCodePut = this.putFromFile(url, testFile);
        assertThat(httpCodePut).isBetween(200, 201);

        // act
        int httpCodeDelete = this.deleteFile(url);

        // assert
        assertThat(httpCodeDelete).isEqualTo(204);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void testThat_multipart_post_single_isWorking() throws Exception {

        // arrange
        File testFile = new File(TEST_FILES_DIR, TEST_FILE_JPEG_01);
        String url = "http://localhost:" + randomServerPort + "/mp-file";

        // act
        int httpCodePut = this.multipartPostFromFile(url, testFile);

        // assert
        assertThat(httpCodePut).isBetween(200, 201);

        String downloadUrl = this.buildUrl(TEST_FILE_JPEG_01);
        File tempFile = File.createTempFile("test", ".jpg");
        tempFile.deleteOnExit();
        long fileSize = this.getToFile(downloadUrl, tempFile);
        assertThat(fileSize).isEqualTo(testFile.length());
        assertThat(tempFile).hasDigest("MD5", TEST_FILE_JPEG_01_MD5);
    }

    @Ignore
    @Test
    public void testThat_multipart_post_multiple_isWorking() throws Exception {

        // arrange
        File testFile1 = new File(TEST_FILES_DIR, TEST_FILE_JPEG_01);
        File testFile2 = new File(TEST_FILES_DIR, TEST_FILE_JPEG_02);
        String url = "http://localhost:" + randomServerPort + "/mp-files";

        // act
        int httpCodePut = this.multipartPostFromFiles(url, new File[] { testFile1, testFile2 } );

        // assert
        assertThat(httpCodePut).isBetween(200, 201);

        {
            String downloadUrl = this.buildUrl(TEST_FILE_JPEG_01);
            File tempFile = File.createTempFile("test", ".jpg");
            tempFile.deleteOnExit();
            long fileSize = this.getToFile(downloadUrl, tempFile);
            assertThat(fileSize).isEqualTo(testFile1.length());
            assertThat(tempFile).hasDigest("MD5", TEST_FILE_JPEG_01_MD5);
        }
        {
            String downloadUrl = this.buildUrl(TEST_FILE_JPEG_02);
            File tempFile = File.createTempFile("test", ".jpg");
            tempFile.deleteOnExit();
            long fileSize = this.getToFile(downloadUrl, tempFile);
            assertThat(fileSize).isEqualTo(testFile2.length());
            assertThat(tempFile).hasDigest("MD5", TEST_FILE_JPEG_02_MD5);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private int putFromFile(String url, File file) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPut httpRequest = new HttpPut(url);
            httpRequest.addHeader(new BasicScheme().authenticate(userCredentials, httpRequest, null));
            FileEntity entity = new FileEntity(file);
            httpRequest.setEntity(entity);
            httpRequest.setHeader("Content-Type", "image/jpeg");
            CloseableHttpResponse response = client.execute(httpRequest);
            return response.getStatusLine().getStatusCode();
        }
    }

    private long getToFile(String url, File file) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet httpRequest = new HttpGet(url);
            httpRequest.addHeader(new BasicScheme().authenticate(userCredentials, httpRequest, null));
            CloseableHttpResponse response = client.execute(httpRequest);
            return copyFileUsingBufferedStreams(response.getEntity().getContent(), file, 4096);
        }
    }

    private int deleteFile(String url) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpDelete httpRequest = new HttpDelete(url);
            httpRequest.addHeader(new BasicScheme().authenticate(userCredentials, httpRequest, null));
            CloseableHttpResponse response = client.execute(httpRequest);
            return response.getStatusLine().getStatusCode();
        }
    }

    private int multipartPostFromFile(String url, File file) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost httpRequest = new HttpPost(url);
            httpRequest.addHeader(new BasicScheme().authenticate(userCredentials, httpRequest, null));

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.IMAGE_JPEG, file.getName())
                    .build();
            httpRequest.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpRequest);
            return response.getStatusLine().getStatusCode();
        }
    }

    private int multipartPostFromFiles(String url, File[] files) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost httpRequest = new HttpPost(url);
            httpRequest.addHeader(new BasicScheme().authenticate(userCredentials, httpRequest, null));

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            for (File file : files) {
                builder.addBinaryBody("file", file, ContentType.IMAGE_JPEG, file.getName());
            }
            HttpEntity entity = builder.build();
            httpRequest.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpRequest);
            return response.getStatusLine().getStatusCode();
        }
    }

    private String buildUrl(String path) {

        return "http://localhost:" + randomServerPort + "/files/" + path;
    }

    private static long copyFileUsingBufferedStreams(InputStream inputStream, File targetFile, int bufferSize) throws IOException {

        long bytesCopied = 0L;
        byte[] buffer = new byte[bufferSize];

        try (BufferedInputStream in = new BufferedInputStream(inputStream)) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;
                }
            }
        }

        return bytesCopied;
    }
}
