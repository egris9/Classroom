package com.Classroom_ai.Classroom.membership;

/** The caller's role in the course does not allow the action. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
