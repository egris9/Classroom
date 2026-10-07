package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.membership.Membership;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CourseFileService {
    private final CourseFileRepository courseFileRepository;
    private final CourseService courseService;
    private final Membership membership;
    @Value("${upload.dir}")
    private String uploadDir;

    public CourseFileService(CourseFileRepository courseFileRepository, CourseService courseService, Membership membership) {
        this.courseFileRepository = courseFileRepository;
        this.courseService = courseService;
        this.membership = membership;
    }

    /** Stores the upload as a file of the course. Only the course's Teacher may do this. */
    public CourseFile add(User user, Long courseId, MultipartFile file) throws IOException {
        Course course = courseService.find(courseId);
        membership.requireTeacher(user, course);

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path target = uploadPath.resolve(UUID.randomUUID() + "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        CourseFile courseFile = new CourseFile();
        courseFile.setFileName(file.getOriginalFilename());
        courseFile.setFilePath(target.toString());
        courseFile.setCourse(course);
        return courseFileRepository.save(courseFile);
    }

    public List<CourseFile> list(User user, Long courseId) {
        Course course = courseService.find(courseId);
        membership.requireMember(user, course);
        return courseFileRepository.findByCourseId(courseId);
    }

    /** The file's record, if the user is a member of its course. */
    public CourseFile get(User user, Long fileId) {
        CourseFile file = courseFileRepository.findById(fileId)
                .orElseThrow(() -> new CourseFileNotFoundException("File not found."));
        membership.requireMember(user, file.getCourse());
        return file;
    }
}
