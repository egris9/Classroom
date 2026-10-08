package com.Classroom_ai.Classroom.api;

import java.util.List;

/** An Exercise set made on the AI tools page; it belongs to no course and is not stored. */
public record ToolsExerciseSetResponse(List<ExerciseResponse> exercises, String model) {
}
