package com.Classroom_ai.Classroom.api;

import jakarta.validation.constraints.NotBlank;

public record JoinRequest(@NotBlank(message = "Access code is required") String accessCode) {
}
