package com.Classroom_ai.Classroom.tools;

/** The text sent to the AI tools is longer than this caller may send. */
public class TextTooLongException extends RuntimeException {

    public TextTooLongException(String message) {
        super(message);
    }
}
