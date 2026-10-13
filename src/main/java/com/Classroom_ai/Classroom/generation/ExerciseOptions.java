package com.Classroom_ai.Classroom.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** What an Exercise set is asked for: the types of its Exercises, how hard they are and how many there are. */
public record ExerciseOptions(List<ExerciseType> types, Difficulty difficulty, int count) {

    public static final int MIN_COUNT = 3;
    public static final int MAX_COUNT = 15;
    public static final ExerciseOptions DEFAULT = new ExerciseOptions(List.of(ExerciseType.OPEN), Difficulty.MEDIUM, 5);

    /**
     * Options from what a request gave, with nulls taking the defaults. The types are made distinct and put in enum
     * order. An empty list of types, or a count outside {@value #MIN_COUNT} to {@value #MAX_COUNT}, is refused.
     */
    public static ExerciseOptions of(List<ExerciseType> typesOrNull, Difficulty difficultyOrNull, Integer countOrNull) {
        List<ExerciseType> types = DEFAULT.types();
        if (typesOrNull != null) {
            if (typesOrNull.isEmpty()) {
                throw new IllegalArgumentException("An exercise set needs at least one type.");
            }
            types = Arrays.stream(ExerciseType.values()).filter(typesOrNull::contains).toList();
        }
        int count = countOrNull == null ? DEFAULT.count() : countOrNull;
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new IllegalArgumentException("The count must be from " + MIN_COUNT + " to " + MAX_COUNT + ".");
        }
        return new ExerciseOptions(types, difficultyOrNull == null ? DEFAULT.difficulty() : difficultyOrNull, count);
    }

    /** The type of each of the {@code count} exercises, taking the types in turn. */
    public List<ExerciseType> plan() {
        List<ExerciseType> plan = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            plan.add(types.get(i % types.size()));
        }
        return plan;
    }
}
