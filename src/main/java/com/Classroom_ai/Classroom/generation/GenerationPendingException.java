package com.Classroom_ai.Classroom.generation;

/** A member asked for a Summary or an Exercise set of a file while an earlier request of the same kind is still running. */
public class GenerationPendingException extends RuntimeException {

    public GenerationPendingException() {
        super("You already have a request running for this PDF. Wait for it to finish.");
    }
}
