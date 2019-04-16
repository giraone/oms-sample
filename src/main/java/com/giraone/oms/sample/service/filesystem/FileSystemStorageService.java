package com.giraone.oms.sample.service.filesystem;

import com.giraone.oms.sample.config.FileSystemConfiguration;
import com.giraone.oms.sample.service.PathNotFoundException;
import com.giraone.oms.sample.service.StorageException;
import com.giraone.oms.sample.service.StorageService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileSystemStorageService implements StorageService {

    @Autowired
    private FileSystemConfiguration fileSystemConfiguration;

    private Path fileStorageLocation;

    @PostConstruct
    private void initialize() {

        this.fileStorageLocation = Paths.get(fileSystemConfiguration.getStorageDirectory()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception e) {
            throw new StorageException("Could not create the directory, where the uploaded files will be stored.", e);
        }
    }

    @Override
    public void transferToStream(String fileName, OutputStream outputStream) {

        if (!this.isValidFileName(fileName)) {
            throw new StorageException("Sorry! Path contains invalid path sequence " + fileName);
        }

        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        try {
            try (final InputStream inputStream = new FileInputStream(targetLocation.toFile())) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (FileNotFoundException e) {
            throw new PathNotFoundException("Path \"" + fileName + "\" not found!", e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public long storeFromStream(InputStream inputStream, long contentLength, String fileName) {

        if (!this.isValidFileName(fileName)) {
            throw new StorageException("Sorry! Path contains invalid path sequence " + fileName);
        }

        // Copy filesystem to the target location (Replacing existing filesystem with the same name)
        Path targetLocation;
        try {
            targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new StorageException("Error storing filesystem \"" + fileName + "\".", e);
        }

        return targetLocation.toFile().length();
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
    public boolean delete(String fileName) {

        if (!this.isValidFileName(fileName)) {
            throw new StorageException("Sorry! Path contains invalid path sequence " + fileName);
        }

        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        return targetLocation.toFile().delete();
    }
}
