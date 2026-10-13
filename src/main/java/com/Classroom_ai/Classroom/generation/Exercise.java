package com.Classroom_ai.Classroom.generation;

import java.util.List;

/**
 * One question with its model answer. A multiple choice exercise has four options and an answer that is one of them;
 * a true or false exercise has no options and the answer {@code "true"} or {@code "false"}. A null type reads as an
 * open question and null options as none, so an exercise stored as just a question and an answer still reads.
 */
public record Exercise(ExerciseType type, String question, List<String> options, String answer, String explanation) {

    public Exercise {
        type = type == null ? ExerciseType.OPEN : type;
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** An open question. */
    public Exercise(String question, String answer) {
        this(ExerciseType.OPEN, question, List.of(), answer, null);
    }
}
