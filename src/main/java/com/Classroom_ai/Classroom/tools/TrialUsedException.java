package com.Classroom_ai.Classroom.tools;

/** An anonymous visitor has had their one free use of the AI tools. */
public class TrialUsedException extends RuntimeException {

    public TrialUsedException() {
        super("Sign up to keep using the AI tools.");
    }
}
