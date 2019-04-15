## Spring Boot File Upload / Download Rest API Example

This is a Spring Boot sample application to show upload and download of files.
For uploading there are two techniques:

- classic HTTP multipart uploads
- modern REST based PUT uploads

## CURL tests

```
curl -v -s http://localhost:8080/actuator/health
curl -v -s -u user:user http://localhost:8080/actuator/health
curl -v -s -u admin:admin http://localhost:8080/actuator/health

curl -v -s -u user:user -o ferrari.jpg http://localhost:8080/files/ferrari.jpg
curl -v -s -u user:user -o ferrari.jpg http://localhost:8080/files/ferrari.jpg?as-attachment=true
curl -v -s -u user:user -o ferrari.jpg http://localhost:8080/files-async/ferrari.jpg

curl -v -s -X POST -u user:user -H "Content-Type: application/xml" -H "X-File-Name: pom.xml" -d @pom.xml http://localhost:8080/files
curl -v -s -X POST -u user:user -H "Content-Type: application/xml" -d @pom.xml http://localhost:8080/files/pom.xml
curl -v -s -X DELETE -u user:user http://localhost:8080/files/pom.xml

curl -v -s -X POST -u user:user -F "file=@readme.md" http://localhost:8080/mp-upload-file
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