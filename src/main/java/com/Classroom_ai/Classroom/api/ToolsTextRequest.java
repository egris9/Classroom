package com.Classroom_ai.Classroom.api;

import jakarta.validation.constraints.NotBlank;

public record ToolsTextRequest(@NotBlank(message = "Text is required") String text) {
}
