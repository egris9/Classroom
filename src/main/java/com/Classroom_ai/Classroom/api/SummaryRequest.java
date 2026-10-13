package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.generation.SummaryLevel;

/** The optional body of a request for a Summary. No level means a medium one. */
public record SummaryRequest(SummaryLevel level) {
}
