package com.Classroom_ai.Classroom.api;

import java.time.Instant;
import java.util.List;

/** {@code types}, {@code difficulty} and {@code count} are what the set was asked for; an older set reads as open, medium. */
public record ExerciseSetResponse(Long id, String status, List<ExerciseResponse> exercises, List<String> types,
                                  String difficulty, int count, boolean published, Long authorId, String model,
                                  String failureCode, String failureMessage, Instant createdAt) {
}
