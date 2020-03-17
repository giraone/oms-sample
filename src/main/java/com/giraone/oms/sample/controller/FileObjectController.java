package com.giraone.oms.sample.controller;

import com.amazonaws.HttpMethod;
import com.giraone.oms.sample.service.StorageService;
import com.giraone.oms.sample.service.s3.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
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
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple REST API for storing and retrieving BLOBs.
 */
@RestController
public class FileObjectController {

    private static final Logger logger = LoggerFactory.getLogger(FileObjectController.class);

    @Autowired
    private StorageService storageService;

    private FileNameMap fileNameMap = URLConnection.getFileNameMap();

    //------------------------------------------------------------------------------------------------------------------
    //- NON MULTIPART CODE ---------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @PutMapping(value = "/files/{fileName}")
    public ResponseEntity<Void> uploadFile(@PathVariable String fileName, HttpServletRequest request) {

        String contentType = request.getContentType() != null ? request.getContentType() : "application/octet-stream";
        long fileSize;
        try {
            fileSize = storageService.storeFromStream(
                    request.getInputStream(), contentType, request.getContentLengthLong(), fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("File \"%s\" saved with %d bytes", fileName, fileSize));
        }

        String location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/").path(fileName).toUriString();
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, location).build();
    }

    @GetMapping("/files/{fileName}")
    public void downloadFileAsAttachment(@PathVariable String fileName,
                                         @RequestParam(name = "as-attachment", defaultValue = "false") boolean asAttachment,
                                         HttpServletResponse response) {
        logger.debug("GET /files/{}", fileName);
        this.downloadFile(fileName, response, asAttachment);
    }

    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Void> downloadFileAsAttachment(@PathVariable String fileName) {

        boolean ret = storageService.delete(fileName);
        logger.debug("DELETE /files/{}: ret={}", fileName, ret);
        return ResponseEntity.status(ret ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/files-async/{fileName}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String fileName) {

        logger.debug("/download-filesystem-async/{}", fileName);
        String contentType = fileNameMap.getContentTypeFor(fileName);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Guessed content-type for \"%s\" is \"%s\".", fileName, contentType));
        }

        StreamingResponseBody stream = outputStream -> storageService.transferToStream(fileName, outputStream);
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.CONTENT_TYPE, contentType).body(stream);
    }

    //------------------------------------------------------------------------------------------------------------------
    //- MULTIPART CODE -------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @PostMapping(value = "/mp-file")
    public ResponseEntity<UploadFileResponse> uploadSingleFileAsMultipart(MultipartFile file) {

        logger.debug("/mp-file " + file.getName());
        String fileName = storageService.storeMultipartFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/").path(fileName).toUriString();
        UploadFileResponse result = new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
        return ResponseEntity.status(HttpStatus.OK).body(result);

    }

    @PostMapping(value = "/mp-files")
    public ResponseEntity<List<UploadFileResponse>> uploadMultipleFilesAsMultipart(MultipartFile[] files) {

        logger.debug("/mp-files #={}", files.length);
        List<UploadFileResponse> result = Arrays.stream(files)
                .map(this::uploadSingleFileAsMultipart)
                .map(HttpEntity::getBody)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    //------------------------------------------------------------------------------------------------------------------
    //- PRE-SIGNED CODE ------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @GetMapping("/pre-signed/{fileName}")
    public ResponseEntity<String> getPreSignedUrl(@PathVariable String fileName, @RequestParam String method) {

        logger.debug("/pre-signed/{}", fileName);

        S3StorageService s = (S3StorageService) this.storageService;
        URL url = s.createPreSignedUrl(fileName, "PUT".equalsIgnoreCase(method) ? HttpMethod.PUT : HttpMethod.POST, 1, 0);

        return ResponseEntity.ok(url.toExternalForm());
    }

    //------------------------------------------------------------------------------------------------------------------

    private void downloadFile(String fileName, HttpServletResponse response, boolean asAttachment) {

        String contentType = fileNameMap.getContentTypeFor(fileName);
        if (logger.isDebugEnabled()) {
            logger.debug("Guessed content-type for \"{}\" is \"{}\".", fileName, contentType);
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
}
