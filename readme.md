## Spring Boot File Upload / Download Rest API Example

This is a Spring Boot sample application to show upload (with and without multipart) and download of files including
tests with [Apache HTTP Components](https://hc.apache.org/).

The code also shows to storage technologies: one with S3 and one with file system storage.

## S3 Setup

The system was tested with *minio*. To run the code without changes, the following two buckets must exist:
- `bucket-01` for the "production" code - see `src/main/resources/application.xml`
- `bucket-unit-test` for the unit test code - `src/test/resources/application.xml`

The default setup uses a locally install minio without SSL.

## CURL tests

```
curl -s http://localhost:8080/actuator/health
curl -s -u user:user http://localhost:8080/actuator/health
curl -s -u admin:admin http://localhost:8080/actuator/health

# Upload PUT
curl -s -X PUT -u user:user -H "Content-Type: image/jpeg" -d @src/test/resources/testfiles/image-01.jpg http://localhost:8080/files/image-01.jpg

# Multipart Upload
curl -s -X POST -u user:user -F "file=@src/test/resources/testfiles/image-02.jpg" http://localhost:8080/mp-file
curl -s -X POST -u user:user -F "files[]=@readme.md" -F "files[]=@pom.xml" http://localhost:8080/mp-files

# Download
curl -s -u user:user -o image-01-1.jpg http://localhost:8080/files/image-01.jpg
curl -s -u user:user -o image-01-2.jpg http://localhost:8080/files/image-01.jpg?as-attachment=true
curl -s -u user:user -o image-01-3.jpg http://localhost:8080/files-async/image-01.jpg

# Delete
curl -s -X DELETE -u user:user http://localhost:8080/files/image-01.jpg
curl -s -X DELETE -u user:user http://localhost:8080/files/image-02.jpg
```

## Steps to Setup

** Run the app using maven**

```bash
mvn spring-boot:run
```

That's it! The application can be accessed at `http://localhost:8080`.

You may also package the application in the form of a jar and then run the jar file like so -

```bash
mvn clean package
java -jar target/upload-sample-1.0.0.jar
```

## Open Issues

- Pre-Signed-URL test from browser
- Cache-Control and Pre-Signed URLs