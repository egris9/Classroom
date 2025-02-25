package com.Classroom_ai.Classroom.CourseFile;

import com.Classroom_ai.Classroom.Cours.Course;
import com.Classroom_ai.Classroom.Cours.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseFileService {
    private final CourseFileRepository courseFileRepository;
    private final CourseRepository courseRepository;
    @Value("${upload.dir}")
    private String uploadDir;

    @Autowired
    public CourseFileService(CourseFileRepository courseFileRepository, CourseRepository courseRepository) {
        this.courseFileRepository = courseFileRepository;
        this.courseRepository = courseRepository;
        // Ensure upload directory path ends with a separator

    }
    private String getUploadDir() {
        if (uploadDir == null) {
            throw new IllegalStateException("Upload directory not configured. Please set 'upload.dir' in application.properties");
        }
        // Ensure upload directory path ends with a separator
        return uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }
    public CourseFile uploadFile(String courseId, MultipartFile file) throws IOException {
        Optional<Course> courseOptional = courseRepository.findByAccessCode(courseId);
        if (courseOptional.isEmpty()) {
            throw new IllegalArgumentException("Course not found");
        }
        Course course = courseOptional.get();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(getUploadDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        // Create proper file path using Path API
        Path targetLocation = uploadPath.resolve(uniqueFileName);

        // Save file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);





        // Enregistrer en base de données
        CourseFile courseFile = new CourseFile();
        courseFile.setFileName(file.getOriginalFilename());
        courseFile.setFilePath(targetLocation.toString());  // Store the complete path
        courseFile.setCourse(course);

        return courseFileRepository.save(courseFile);
    }

    public CourseFile getFile(Long fileId) {
        return courseFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));
    }


    public List<CourseFile> getFilesByCourseId(Long courseId) {
        return courseFileRepository.findByCourseId(courseId);
    }


}
