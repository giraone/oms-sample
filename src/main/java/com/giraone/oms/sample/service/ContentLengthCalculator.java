package com.giraone.oms.sample.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ContentLengthCalculator {

    Path tempFile;
    long contentLength;

    public ContentLengthCalculator(InputStream inputStream) {
        super();
        try {
            this.tempFile = Files.createTempFile("stosvc", "");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.contentLength = this.tempFile.toFile().length();
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(this.tempFile.toFile());
        } catch (FileNotFoundException e) {
            throw new StorageException("Cannot open temp filesystem " + this.tempFile, e);
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    public void clean() {
        this.tempFile.toFile().delete();
    }
}
