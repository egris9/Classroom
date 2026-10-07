package com.Classroom_ai.Classroom.generation;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "exercise_sets")
public class ExerciseSet extends Generated {
    /** The exercises as a JSON array of {question, answer}. */
    @Lob
    private String exercisesJson;
}
