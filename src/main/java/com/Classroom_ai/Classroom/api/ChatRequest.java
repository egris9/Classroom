package com.Classroom_ai.Classroom.api;

import java.util.List;

/** What a visitor says to the chat: the whole conversation so far, ending with their own message. */
public record ChatRequest(List<Turn> messages) {

    /** One turn as the client sends it. It is checked by the controller, not trusted: any role may arrive. */
    public record Turn(String role, String content) {
    }
}
