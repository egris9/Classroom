package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.generation.Exercise;

import java.util.List;

public record ExerciseResponse(String type, String question, List<String> options, String answer, String explanation) {

    public static ExerciseResponse of(Exercise exercise) {
        return new ExerciseResponse(exercise.type().name(), exercise.question(), exercise.options(), exercise.answer(),
                exercise.explanation());
    }
}
