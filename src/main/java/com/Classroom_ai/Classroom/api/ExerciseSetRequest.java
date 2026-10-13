package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.generation.Difficulty;
import com.Classroom_ai.Classroom.generation.ExerciseType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** What an Exercise set is asked for. Every field is optional: types default to open, difficulty to medium, count to 5. */
public record ExerciseSetRequest(@Size(min = 1) List<@NotNull ExerciseType> types, Difficulty difficulty,
                                 @Min(3) @Max(15) Integer count) {
}
