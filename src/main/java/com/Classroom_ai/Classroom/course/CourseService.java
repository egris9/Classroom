package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.api.CourseRequest;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.membership.ForbiddenException;
import com.Classroom_ai.Classroom.membership.Membership;
import com.Classroom_ai.Classroom.membership.Role;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final Membership membership;
    private final Materials materials;
    private final ApplicationEventPublisher events;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository, Membership membership,
                         Materials materials, ApplicationEventPublisher events) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.membership = membership;
        this.materials = materials;
        this.events = events;
    }

    public CourseAccess create(User teacher, CourseRequest request) {
        if (courseRepository.existsByTeacherIdAndCourseName(teacher.getId(), request.courseName())) {
            throw new CourseNameTakenException("You already teach a course with this name.");
        }
        Course course = new Course();
        course.setCourseName(request.courseName());
        course.setSection(request.section());
        course.setSubject(request.subject());
        course.setRoom(request.room());
        course.setTeacher(teacher);
        course.setAccessCode(Course.generateAccessCode());
        return new CourseAccess(courseRepository.save(course), Role.TEACHER);
    }

    /** Enrols the user as a Student. Joining a course you are already in changes nothing. */
    public CourseAccess join(User user, String accessCode) {
        Course course = courseRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new CourseNotFoundException("No course has this access code."));
        if (Objects.equals(course.getTeacher().getId(), user.getId())) {
            throw new AlreadyTeacherException("You teach this course, so you cannot join it.");
        }
        if (membership.roleOf(user, course) == Role.NONE) {
            User managed = userRepository.findById(user.getId()).orElseThrow();
            if (managed.getCourses() == null) {
                managed.setCourses(new ArrayList<>());
            }
            managed.getCourses().add(course);
            course.getStudents().add(managed);
            userRepository.save(managed);
        }
        return new CourseAccess(course, Role.STUDENT);
    }

    public List<CourseAccess> listFor(User user) {
        List<CourseAccess> result = new ArrayList<>();
        courseRepository.findByTeacherId(user.getId())
                .forEach(course -> result.add(new CourseAccess(course, Role.TEACHER)));
        courseRepository.findByStudentsContaining(user)
                .forEach(course -> result.add(new CourseAccess(course, Role.STUDENT)));
        return result;
    }

    /** Removes the course with its files, their generated items, their stored PDFs and its enrolments. Teacher only. */
    public void delete(User user, Long courseId) {
        Course course = find(courseId);
        membership.requireTeacher(user, course);

        String pictureKey = course.getPicturePath();
        List<CourseFile> removed = List.copyOf(course.getFiles());
        removed.forEach(file -> events.publishEvent(new FileRemoved(file.getId())));
        for (User student : course.getStudents()) {
            student.getCourses().remove(course);
        }
        courseRepository.delete(course);
        AfterCommit.run(() -> removed.forEach(materials::delete));
        deleteAfterCommit(pictureKey);    }

    /** Sets or replaces the cover picture. Teacher only. The old file goes once the change is committed. */
    public CourseAccess setPicture(User user, Long courseId, MultipartFile upload) throws IOException {
        Course course = find(courseId);
        membership.requireTeacher(user, course);
        String oldKey = course.getPicturePath();
        String newKey = materials.putPicture(upload);
        AfterCommit.onRollback(() -> materials.deletePicture(newKey));
        course.setPicturePath(newKey);
        deleteAfterCommit(oldKey);
        return new CourseAccess(courseRepository.save(course), Role.TEACHER);
    }

    /** Removes the cover picture. Teacher only. Removing a cover that is not there changes nothing. */
    public void removePicture(User user, Long courseId) {
        Course course = find(courseId);
        membership.requireTeacher(user, course);
        String oldKey = course.getPicturePath();
        course.setPicturePath(null);
        deleteAfterCommit(oldKey);
    }

    private void deleteAfterCommit(String pictureKey) {
        if (pictureKey != null) {
            AfterCommit.run(() -> materials.deletePicture(pictureKey));
        }
    }

    /** A Student leaves the course. The Teacher cannot leave their own course, and a non-member has nothing to leave. */
    public void leave(User user, Long courseId) {
        Course course = find(courseId);
        if (membership.requireMember(user, course) == Role.TEACHER) {
            throw new ForbiddenException("The teacher cannot leave their own course. Delete it instead.");
        }
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.getCourses().remove(course);
        course.getStudents().remove(managed);
        userRepository.save(managed);
    }

    /** The students enrolled in the course. Teacher only. */
    public List<User> studentsOf(User user, Long courseId) {
        Course course = find(courseId);
        membership.requireTeacher(user, course);
        return List.copyOf(course.getStudents());
    }

    public CourseAccess getFor(User user, Long courseId) {
        Course course = find(courseId);
        return new CourseAccess(course, membership.requireMember(user, course));
    }

    Course find(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found."));
    }
}
