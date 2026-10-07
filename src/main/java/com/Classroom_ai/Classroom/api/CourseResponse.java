package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.membership.Role;

/** A course as one caller sees it. {@code accessCode} is set only when the caller is the Teacher. */
public record CourseResponse(
        Long id,
        String courseName,
        String section,
        String subject,
        Integer room,
        TeacherResponse teacher,
        Role role,
        String accessCode) {

    /** {@code picture} is the path to fetch the Teacher's picture from, or null when there is none. */
    public record TeacherResponse(Long id, String name, String picture) {
    }
}
