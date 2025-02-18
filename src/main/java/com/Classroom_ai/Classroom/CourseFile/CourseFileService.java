package com.Classroom_ai.Classroom.CourseFile;

import com.Classroom_ai.Classroom.Cours.Course;
import com.Classroom_ai.Classroom.Cours.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseFileService {
    private final CourseFileRepository courseFileRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public CourseFileService(CourseFileRepository courseFileRepository, CourseRepository courseRepository) {
        this.courseFileRepository = courseFileRepository;
        this.courseRepository = courseRepository;
    }

    public CourseFile uploadFile(Long courseId, MultipartFile file) throws IOException {
        Optional<Course> courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isEmpty()) {
            throw new IllegalArgumentException("Course not found");
        }
        Course course = courseOptional.get();

        // Générer un nom de fichier unique
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String uploadDir = "src/main/resources/uploads/";
        String filePath = uploadDir + fileName;

        // Sauvegarde physique du fichier
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + uploadDir);
            }
        }
        file.transferTo(new File(filePath));

        // Enregistrer en base de données
        CourseFile courseFile = new CourseFile();
        courseFile.setFileName(file.getOriginalFilename());
        courseFile.setFilePath(filePath);
        courseFile.setCourse(course);

        return courseFileRepository.save(courseFile);
    }

    public List<CourseFile> getFilesByCourseId(Long courseId) {
        return courseFileRepository.findByCourseId(courseId);
    }

    public byte[] getFileContent(Long fileId) throws IOException {
        Optional<CourseFile> courseFileOptional = courseFileRepository.findById(fileId);
        if (courseFileOptional.isEmpty()) {
            throw new IllegalArgumentException("File not found");
        }
        return Files.readAllBytes(Paths.get(courseFileOptional.get().getFilePath()));
    }
}
