package com.Classroom_ai.Classroom.CourseFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/course-files")
public class CourseFileController {
    private final CourseFileService courseFileService;

    @Autowired
    public CourseFileController(CourseFileService courseFileService) {
        this.courseFileService = courseFileService;
    }
    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/upload/{courseId}")
    public ResponseEntity<CourseFile> uploadFile(@PathVariable String courseId, @RequestParam("file") MultipartFile file) {

        try {
            CourseFile uploadedFile = courseFileService.uploadFile(courseId, file);
            return ResponseEntity.ok(uploadedFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseFile>> getFilesByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseFileService.getFilesByCourseId(courseId));
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewPdfFile(@PathVariable Long fileId) {
        try {
            // Get the CourseFile entity from database
            CourseFile courseFile = courseFileService.getFile(fileId);

            // Create a Path object from the filePath
            Path path = Paths.get(courseFile.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            System.out.println("Created Path object: " + path);
            System.out.println("Absolute Path: " + path.toAbsolutePath());
            System.out.println("Path exists: " + Files.exists(path));
            // Check if file exists and is readable
            if (resource.exists() && resource.isReadable()) {
                HttpHeaders headers = new HttpHeaders();
                // Set these specific headers to prevent color inversion
                headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
                headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + courseFile.getFileName() + "\"");

                // Add Cross-Origin headers to prevent browser issues
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS");
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(resource.contentLength())
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
