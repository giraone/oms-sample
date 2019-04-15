package com.giraone.oms.sample.controller;

import com.giraone.oms.sample.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FileObjectController {

    private static final Logger logger = LoggerFactory.getLogger(FileObjectController.class);

    @Autowired
    private StorageService storageService;

    private FileNameMap fileNameMap = URLConnection.getFileNameMap();

    @PostMapping("/mp-upload-filesystem")
    public UploadFileResponse uploadFileAsMultipart(@RequestParam("filesystem") MultipartFile file) {

        String fileName = storageService.storeMultipartFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/download-filesystem/").path(fileName).toUriString();
        return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
    }

    @PostMapping("/mp-upload-multiple-files")
    public List<UploadFileResponse> uploadMultipleFilesAsMultipart(@RequestParam("files") MultipartFile[] files) {

        return Arrays.asList(files).stream().map(file -> uploadFileAsMultipart(file)).collect(Collectors.toList());
    }

    @PostMapping(value = "/files")
    public ResponseEntity<Void> uploadNewFile(HttpServletRequest request) {

        String fileName = request.getHeader("X-File-Name");
        long fileSize;
        try {
            fileSize = storageService.storeFromStream(request.getInputStream(), request.getContentLengthLong(), fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("File \"%s\" saved with %ld bytes", fileName, fileSize));
        }

        String location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/").path(fileName).toUriString();
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, location).build();
    }

    @PutMapping(value = "/files/{fileName}")
    public ResponseEntity<Void> uploadFile(@PathVariable String fileName, HttpServletRequest request) {

        long fileSize;
        try {
            fileSize = storageService.storeFromStream(request.getInputStream(), request.getContentLengthLong(), fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("File \"%s\" saved with %ld bytes", fileName, fileSize));
        }

        String location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/").path(fileName).toUriString();
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, location).build();
    }

    @GetMapping("/files/{fileName}")
    public void downloadFileAsAttachment(@PathVariable String fileName,
                                         @RequestParam(name = "as-attachment", defaultValue = "false") boolean asAttachment, HttpServletResponse response) {
        logger.debug("GET /files/" + fileName);
        this.downloadFile(fileName, response, asAttachment);
    }

    protected void downloadFile(String fileName, HttpServletResponse response, boolean asAttachment) {

        String contentType = fileNameMap.getContentTypeFor(fileName);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Guessed content-type for \"%s\" is \"%s\".", fileName, contentType));
        }
        response.setContentType(contentType);
        if (asAttachment) {
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        }
        try {
            try (OutputStream outputStream = response.getOutputStream()) {
                storageService.transferToStream(fileName, outputStream);
            }
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Void> downloadFileAsAttachment(@PathVariable String fileName) {
        logger.debug("DELETE /files/" + fileName);
        boolean ret = storageService.delete(fileName);
        return ResponseEntity.status(ret ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/files-async/{fileName}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String fileName) {

        logger.debug("/download-filesystem-async/" + fileName);
        String contentType = fileNameMap.getContentTypeFor(fileName);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Guessed content-type for \"%s\" is \"%s\".", fileName, contentType));
        }

        StreamingResponseBody stream = outputStream -> {
            storageService.transferToStream(fileName, outputStream);
        };
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.CONTENT_TYPE, contentType).body(stream);
    }
}
