package com.giraone.oms.sample.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface StorageService {

    public static final String FILENAME_REGEX = "^[a-zA-Z0-9_. -]+$";

    public default boolean isValidFileName(String fileName) {
        return fileName.matches(FILENAME_REGEX);
    }

    public default File copyToTempFile(InputStream inputStream) throws IOException {

        Path tempFile = Files.createTempFile("stosvc", "");
        Files.copy(inputStream, tempFile);
        return tempFile.toFile();
    }

    public void transferToStream(String path, OutputStream outputStream);

    public long storeFromStream(InputStream inputStream, long contentLength, String path);

    public String storeMultipartFile(MultipartFile file);

    public boolean delete(String path);
}
