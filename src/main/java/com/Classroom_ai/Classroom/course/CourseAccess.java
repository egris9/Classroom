package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.membership.Role;

/** A course together with the caller's role in it. */
public record CourseAccess(Course course, Role role) {
}
