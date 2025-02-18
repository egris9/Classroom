package com.Classroom_ai.Classroom.CourseFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/upload/{courseId}")
    public ResponseEntity<CourseFile> uploadFile(@PathVariable Long courseId, @RequestParam("file") MultipartFile file) {
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

    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        try {
            byte[] fileContent = courseFileService.getFileContent(fileId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
