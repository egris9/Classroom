package com.Classroom_ai.Classroom.generation;

/**
 * One turn of a chat. Only a person ({@code "user"}) and the assistant speak in a history: the system prompt belongs
 * to the server, so nobody can slip an instruction in under another role.
 */
public record ChatMessage(String role, String content) {

    public ChatMessage {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException("A chat message is from the user or the assistant.");
        }
        if (content == null) {
            throw new IllegalArgumentException("A chat message needs content.");
        }
    }
}
