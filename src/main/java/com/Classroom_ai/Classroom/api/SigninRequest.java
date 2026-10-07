package com.Classroom_ai.Classroom.api;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(@NotBlank String email, @NotBlank String password) {
}
