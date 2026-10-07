package com.Classroom_ai.Classroom.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseRequest(
        @NotBlank(message = "Course Name is required") String courseName,
        @NotBlank(message = "Section is required") String section,
        @NotBlank(message = "Subject is required") String subject,
        @NotNull(message = "Room is required") @Min(value = 1, message = "Room must be greater than 0") Integer room) {
}
