package com.Classroom_ai.Classroom.course;

/**
 * Published inside the transaction that removes a CourseFile, before the row goes. Modules that keep rows
 * pointing at a file (the Summaries and Exercise sets of {@code generation}) delete theirs when they hear it.
 */
public record FileRemoved(Long fileId) {
}
