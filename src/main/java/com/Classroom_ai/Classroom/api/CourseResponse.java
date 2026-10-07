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

    public record TeacherResponse(String name, String profilePicture) {
    }
}
