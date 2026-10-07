package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredFile;
import com.Classroom_ai.Classroom.membership.Membership;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class CourseFileService {
    private final CourseFileRepository courseFileRepository;
    private final CourseService courseService;
    private final Membership membership;
    private final Materials materials;

    public CourseFileService(CourseFileRepository courseFileRepository, CourseService courseService,
                             Membership membership, Materials materials) {
        this.courseFileRepository = courseFileRepository;
        this.courseService = courseService;
        this.membership = membership;
        this.materials = materials;
    }

    /** Stores the upload as a file of the course. Only the course's Teacher may do this. */
    public CourseFile add(User user, Long courseId, MultipartFile file) throws IOException {
        Course course = courseService.find(courseId);
        membership.requireTeacher(user, course);

        StoredFile stored = materials.put(course, file);

        CourseFile courseFile = new CourseFile();
        courseFile.setFileName(stored.displayName());
        courseFile.setFilePath(stored.key());
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
