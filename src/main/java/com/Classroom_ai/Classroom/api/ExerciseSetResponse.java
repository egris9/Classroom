package com.Classroom_ai.Classroom.api;

import java.time.Instant;
import java.util.List;

public record ExerciseSetResponse(Long id, String status, List<ExerciseResponse> exercises, boolean published,
                                  Long authorId, String model, String failureCode, String failureMessage,
                                  Instant createdAt) {
}
