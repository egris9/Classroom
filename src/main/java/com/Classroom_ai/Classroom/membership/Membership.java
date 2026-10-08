package com.Classroom_ai.Classroom.membership;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.course.Course;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** The one place that decides a user's role in a course. */
@Component
public class Membership {

    public Role roleOf(User user, Course course) {
        if (Objects.equals(course.getTeacher().getId(), user.getId())) {
            return Role.TEACHER;
        }
        boolean enrolled = course.getStudents().stream()
                .anyMatch(student -> Objects.equals(student.getId(), user.getId()));
        return enrolled ? Role.STUDENT : Role.NONE;
    }

    /** Returns the caller's role, or throws {@link ForbiddenException} if they are not in the course. */
    public Role requireMember(User user, Course course) {
        Role role = roleOf(user, course);
        if (role == Role.NONE) {
            throw new ForbiddenException("You are not a member of this course.");
        }
        return role;
    }

    public void requireTeacher(User user, Course course) {
        if (roleOf(user, course) != Role.TEACHER) {
            throw new ForbiddenException("Only the teacher of this course can do this.");
        }
    }
}
