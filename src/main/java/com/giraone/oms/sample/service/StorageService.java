package com.giraone.oms.sample.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface StorageService {

    String PATH_REGEX = "^[a-zA-Z0-9_.-][a-zA-Z0-9_. -/]*[a-zA-Z0-9]+$";

    default boolean isValidPathName(String path) {
        return path.matches(PATH_REGEX);
    }

    default File copyToTempFile(InputStream inputStream) throws IOException {

        Path tempFile = Files.createTempFile("sto-svc", "");
        Files.copy(inputStream, tempFile);
        return tempFile.toFile();
    }

    void transferToStream(String path, OutputStream outputStream);

    /**
     * Store the BLOB defined by reading an input stream in S3 using "path" as the object key.
     * @param inputStream   The input stream to read the content from
     * @param contentType   The content type of the BLOB
     * @param contentLength The content length or null, if the content length is unknown by the caller.
     *                      In this case the content length will be calculated by a reading!
     * @param path          The object key to be used as the path to the object.
     * @return the byte length of the content that was stored
     */
    long storeFromStream(InputStream inputStream, String contentType, Long contentLength, String path);

    String storeMultipartFile(MultipartFile file);

    boolean exists(String path);

    boolean delete(String path);
}
