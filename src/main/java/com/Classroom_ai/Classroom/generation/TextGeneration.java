package com.Classroom_ai.Classroom.generation;

import java.util.List;
import java.util.function.Consumer;

/** The seam through which the app asks a model for summaries, exercises and chat replies. */
public interface TextGeneration {

    String summarize(String text);

    List<Exercise> generateExercises(String text, int count);

    /**
     * Answers the last message of {@code history} as the study assistant. {@code onDelta} gets each piece of the
     * reply as the model writes it: never blank, never a {@code <think>} block, and the pieces joined are the whole
     * reply. Returns when the reply is complete. A reply that cannot be completed throws {@link GenerationFailure}
     * (after the pieces already sent), and so does {@code onDelta}: whatever it throws stops the model call and is
     * rethrown, which is how a caller that has gone away cancels the reply.
     */
    void chat(List<ChatMessage> history, Consumer<String> onDelta);

    /** The name stored beside every result, so a result can be traced to the model that wrote it. */
    String modelName();
}
