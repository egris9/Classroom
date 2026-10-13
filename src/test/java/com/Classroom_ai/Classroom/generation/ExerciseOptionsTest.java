package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseOptionsTest {

    @Test
    void defaultsAreFiveOpenMediumQuestions() {
        assertThat(ExerciseOptions.DEFAULT.types()).containsExactly(ExerciseType.OPEN);
        assertThat(ExerciseOptions.DEFAULT.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(ExerciseOptions.DEFAULT.count()).isEqualTo(5);
    }

    @Test
    void nullsTakeTheDefaults() {
        assertThat(ExerciseOptions.of(null, null, null)).isEqualTo(ExerciseOptions.DEFAULT);
    }

    @Test
    void typesAreDistinctAndInOrder() {
        ExerciseOptions options = ExerciseOptions.of(
                List.of(ExerciseType.TRUE_FALSE, ExerciseType.OPEN, ExerciseType.TRUE_FALSE), null, null);

        assertThat(options.types()).containsExactly(ExerciseType.OPEN, ExerciseType.TRUE_FALSE);
    }

    @Test
    void countOfTwoOrSixteenIsRefused() {
        assertThatThrownBy(() -> ExerciseOptions.of(null, null, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExerciseOptions.of(null, null, 16)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ExerciseOptions.of(null, null, 3).count()).isEqualTo(3);
        assertThat(ExerciseOptions.of(null, null, 15).count()).isEqualTo(15);
    }

    @Test
    void emptyTypesAreRefused() {
        assertThatThrownBy(() -> ExerciseOptions.of(List.of(), null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planTakesTheTypesInTurn() {
        ExerciseOptions options = ExerciseOptions.of(
                List.of(ExerciseType.OPEN, ExerciseType.MULTIPLE_CHOICE, ExerciseType.TRUE_FALSE), null, 5);

        assertThat(options.plan()).containsExactly(ExerciseType.OPEN, ExerciseType.MULTIPLE_CHOICE,
                ExerciseType.TRUE_FALSE, ExerciseType.OPEN, ExerciseType.MULTIPLE_CHOICE);
    }

    @Test
    void anExerciseStoredAsQuestionAndAnswerOnlyReadsAsOpen() throws Exception {
        Exercise exercise = new ObjectMapper().readValue("{\"question\":\"Q?\",\"answer\":\"A\"}", Exercise.class);

        assertThat(exercise.type()).isEqualTo(ExerciseType.OPEN);
        assertThat(exercise.options()).isEmpty();
        assertThat(exercise.explanation()).isNull();
        assertThat(exercise.question()).isEqualTo("Q?");
        assertThat(exercise.answer()).isEqualTo("A");
    }
}
