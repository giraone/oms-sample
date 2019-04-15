package com.giraone.oms.sample.controller;

class UploadFileResponse {
    private String fileName;
    private String fileDownloadUri;
    private String fileType;
    private long size;

    UploadFileResponse(String fileName, String fileDownloadUri, String fileType, long size) {
        this.fileName = fileName;
        this.fileDownloadUri = fileDownloadUri;
        this.fileType = fileType;
        this.size = size;
    }

    String getFileName() {
        return fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String getFileDownloadUri() {
        return fileDownloadUri;
    }

    void setFileDownloadUri(String fileDownloadUri) {
        this.fileDownloadUri = fileDownloadUri;
    }

    String getFileType() {
        return fileType;
    }

    void setFileType(String fileType) {
        this.fileType = fileType;
    }

    long getSize() {
        return size;
    }

    void setSize(long size) {
        this.size = size;
    }
}
