package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredFile;
import com.Classroom_ai.Classroom.membership.Membership;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

    public CourseFileService(CourseFileRepository courseFileRepository, CourseService courseService,
                             Membership membership, Materials materials, ApplicationEventPublisher events) {
        this.courseFileRepository = courseFileRepository;
        this.courseService = courseService;
        this.membership = membership;
        this.materials = materials;
        this.events = events;
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

    /** Removes the file, its Summaries and Exercise sets, and its stored PDF. Only the course's Teacher may do this. */
    public void delete(User user, Long fileId) {
        CourseFile file = find(fileId);
        Course course = file.getCourse();
        membership.requireTeacher(user, course);

        events.publishEvent(new FileRemoved(file.getId()));
        course.getFiles().remove(file);
        AfterCommit.run(() -> materials.delete(file));
    }

    /** The file's record, if the user is a member of its course. */
    public CourseFile get(User user, Long fileId) {
        CourseFile file = find(fileId);
        membership.requireMember(user, file.getCourse());
        return file;
    }

    private CourseFile find(Long fileId) {
        return courseFileRepository.findById(fileId)
                .orElseThrow(() -> new CourseFileNotFoundException("File not found."));
    }
}
