package com.Classroom_ai.Classroom.auth;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException() {
        super("Email is already taken.");
    }
}
