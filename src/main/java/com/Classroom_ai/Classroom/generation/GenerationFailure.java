package com.Classroom_ai.Classroom.generation;

/** A generation that cannot succeed. The code is stored on the result so the user sees why. */
public class GenerationFailure extends RuntimeException {

    private final String code;

    public GenerationFailure(String code, String message) {
        super(message);
        this.code = code;
    }

    public GenerationFailure(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
