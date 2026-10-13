package com.Classroom_ai.Classroom.generation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "exercise_sets")
public class ExerciseSet extends Generated {
    /** The exercises as a JSON array of {type, question, options, answer, explanation}. */
    @Lob
    private String exercisesJson;

    /** The types asked for, as comma-separated names; null for a set made before sets had options. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String types;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Integer exerciseCount;

    /**
     * What this set was asked for. A set made before sets had options was five open questions of medium difficulty;
     * its count is read from the exercises themselves, so here it is only the default.
     */
    public ExerciseOptions getOptions() {
        List<ExerciseType> asked = types == null || types.isBlank() ? null
                : Arrays.stream(types.split(",")).map(ExerciseType::valueOf).toList();
        return ExerciseOptions.of(asked, difficulty, exerciseCount);
    }

    public void setOptions(ExerciseOptions options) {
        this.types = String.join(",", options.types().stream().map(ExerciseType::name).toList());
        this.difficulty = options.difficulty();
        this.exerciseCount = options.count();
    }

    /** How many exercises were asked for, or null for a set made before sets had options. */
    public Integer getAskedCount() {
        return exerciseCount;
    }
}
