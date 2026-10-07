package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import com.Classroom_ai.Classroom.api.CourseRequest;
import com.Classroom_ai.Classroom.membership.Membership;
import com.Classroom_ai.Classroom.membership.Role;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final Membership membership;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository, Membership membership) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.membership = membership;
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

    public CourseAccess getFor(User user, Long courseId) {
        Course course = find(courseId);
        return new CourseAccess(course, membership.requireMember(user, course));
    }

    Course find(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found."));
    }
}
