package com.Classroom_ai.Classroom.tools;

/** The chat history cannot be answered: no messages, too many, too long, or from a role other than the two allowed. */
public class BadChatRequestException extends RuntimeException {

    public BadChatRequestException(String message) {
        super(message);
    }
}
