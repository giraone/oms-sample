'use strict';

var singleUploadForm = document.querySelector('#singleUploadForm');
var singleFileUploadInput = document.querySelector('#singleFileUploadInput');
var singleFileUploadError = document.querySelector('#singleFileUploadError');
var singleFileUploadSuccess = document.querySelector('#singleFileUploadSuccess');

var multipleUploadForm = document.querySelector('#multipleUploadForm');
var multipleFileUploadInput = document.querySelector('#multipleFileUploadInput');
var multipleFileUploadError = document.querySelector('#multipleFileUploadError');
var multipleFileUploadSuccess = document.querySelector('#multipleFileUploadSuccess');

function uploadSingleFileWithTarget(file, method, url, withCredentials) {

  var fileReader = new FileReader();
  var contentType;
  if (file.type == '') {
    contentType = 'application/octet-stream';
  } else {
    contentType = file.type;
  }
  fileReader.readAsArrayBuffer(file);

  fileReader.onload = function(e) {
    var xhr = new XMLHttpRequest();
    xhr.withCredentials = withCredentials;
    xhr.open(method, url);
    xhr.setRequestHeader("Content-Type", contentType);

    xhr.onload = function() {
      if (xhr.status == 201) {
        var location = xhr.getResponseHeader("Location");
        singleFilePutUploadError.style.display = "none";
        singleFilePutUploadSuccess.innerHTML = "<p>File Uploaded Successfully.</p><p>DownloadUrl : <a href='" + location
            + "' target='_blank'>" + location + "</a></p>";
        singleFileUploadSuccess.style.display = "block";
      } else {
        singleFilePutUploadError.style.display = "none";
        singleFilePutUploadError.innerHTML = "Error " + xhr.status + " " + xhr.responseText;
      }
    }
    xhr.send(fileReader.result);
  }
}

function uploadSingleFileWithPut() {

  var fileInput = document.getElementById('singleFileUploadWithReader');
  var file = fileInput.files[0];
  uploadSingleFileWithTarget(file, "PUT", "/files/" + file.name, true);
}

function uploadSingleFileWithPreSignedPut(file) {

  var fileInput = document.getElementById('singleFileUploadWithReader');
  var file = fileInput.files[0];

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.open("GET", "/pre-signed/" + file.name + "?method=PUT");
  xhr.onload = function() {
      uploadSingleFileWithTarget(file, "PUT", xhr.responseText, false);
  }
  xhr.send();
}

function uploadSingleFileWithPreSignedPost(file) {

  var fileInput = document.getElementById('singleFileUploadWithReader');
  var file = fileInput.files[0];
  var formData = new FormData();
  formData.append("file", file);

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.open("GET", "/pre-signed/" + file.name + "?method=POST");
  xhr.onload = function() {
      uploadSingleFileWithTarget(file, "POST", xhr.responseText, false);
  }
  xhr.send();
}

function uploadSingleFileWithPreSignedMultipart(file) {

  var fileInput = document.getElementById('singleFileUploadWithReader');
  var file = fileInput.files[0];
  var formData = new FormData();
  formData.append("file", file);

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.open("GET", "/pre-signed/" + file.name + "?method=POST");
  xhr.onload = function() {
      console.log(xhr.responseText);
      uploadSingleFileAsFormData(file, xhr.responseText);
  }
  xhr.send();
}

function uploadSingleFileAsFormData(file, url) {
  var formData = new FormData();
  formData.append("file", file);

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.open("POST", url);

  xhr.onload = function() {
    console.log(xhr.responseText);
    var response = JSON.parse(xhr.responseText);
    if (xhr.status == 200) {
      singleFileUploadError.style.display = "none";
      singleFileUploadSuccess.innerHTML = "<p>File Uploaded Successfully.</p><p>DownloadUrl : <a href='" + response.fileDownloadUri
          + "' target='_blank'>" + response.fileDownloadUri + "</a></p>";
      singleFileUploadSuccess.style.display = "block";
    } else {
      singleFileUploadSuccess.style.display = "none";
      singleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
    }
  }

  xhr.send(formData);
}

function uploadMultipleFilesAsFormData(files, url) {
  var formData = new FormData();
  for (var index = 0; index < files.length; index++) {
    formData.append("files", files[index]);
  }

  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.open("POST", url);

  xhr.onload = function() {
    console.log(xhr.responseText);
    var response = JSON.parse(xhr.responseText);
    if (xhr.status == 200) {
      multipleFileUploadError.style.display = "none";
      var content = "<p>All Files Uploaded Successfully</p>";
      for (var i = 0; i < response.length; i++) {
        content += "<p>DownloadUrl : <a href='" + response[i].fileDownloadUri + "' target='_blank'>" + response[i].fileDownloadUri
            + "</a></p>";
      }
      multipleFileUploadSuccess.innerHTML = content;
      multipleFileUploadSuccess.style.display = "block";
    } else {
      multipleFileUploadSuccess.style.display = "none";
      multipleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
    }
  }

  xhr.send(formData);
}

singleUploadForm.addEventListener('submit', function(event) {
  var files = singleFileUploadInput.files;
  if (files.length === 0) {
    singleFileUploadError.innerHTML = "Please select a file";
    singleFileUploadError.style.display = "block";
  }
  uploadSingleFileAsFormData(files[0], "/mp-file");
  event.preventDefault();
}, true);

multipleUploadForm.addEventListener('submit', function(event) {
  var files = multipleFileUploadInput.files;
  if (files.length === 0) {
    multipleFileUploadError.innerHTML = "Please select at least one file";
    multipleFileUploadError.style.display = "block";
  }
  uploadMultipleFilesAsFormData(files, "/mp-files");
  event.preventDefault();
}, true);
