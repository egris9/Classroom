package com.Classroom_ai.Classroom.api;

import java.time.Instant;

public record SummaryResponse(Long id, String status, String text, boolean published, Long authorId, String model,
                              String failureCode, String failureMessage, Instant createdAt) {
}
