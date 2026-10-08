package com.Classroom_ai.Classroom.generation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What every {@link TextGeneration} must do, whatever stands behind it. The fake, the local adapter against a stub
 * server, and the local adapter against a real model all run these same tests.
 */
abstract class TextGenerationContract {

    private static final String ENGLISH = "Photosynthesis is the process by which plants turn light into chemical energy. "
            + "It takes place in the chloroplasts. Chlorophyll absorbs the light. Water and carbon dioxide are combined "
            + "into glucose, and oxygen is released. ";
    private static final String FRENCH = "La photosynthese est le processus par lequel les plantes transforment la lumiere "
            + "en energie chimique. Elle a lieu dans les chloroplastes. La chlorophylle absorbe la lumiere. ";

    abstract TextGeneration adapter();

    /** Text of about {@code chars} characters made of whole sentences. */
    static String lesson(int chars) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; text.length() < chars; i++) {
            text.append(i % 2 == 0 ? ENGLISH : FRENCH);
        }
        return text.toString().strip();
    }

    @Test
    void it_names_its_model() {
        assertThat(adapter().modelName()).isNotBlank();
    }

    @Test
    void a_summary_is_not_blank() {
        assertThat(adapter().summarize(lesson(600))).isNotBlank();
    }

    @Test
    void a_summary_is_shorter_than_a_long_text() {
        String text = lesson(3000);

        assertThat(adapter().summarize(text).length()).isLessThan(text.length());
    }

    /** The demo adapter quotes its source on purpose and says so; every real adapter must not. */
    boolean quotesItsSource() {
        return false;
    }

    @Test
    void summaryIsNotAQuoteOfTheStart() {
        String text = lesson(3000);

        String summary = adapter().summarize(text);

        if (!quotesItsSource()) {
            assertThat(summary).doesNotContain(text.substring(0, 80));
        }
        assertThat(summary.stripTrailing()).doesNotEndWith("...").doesNotEndWith("…");
    }

    @Test
    void a_text_longer_than_one_chunk_still_gets_a_summary() {
        String text = lesson(25_000);

        String summary = adapter().summarize(text);

        assertThat(summary).isNotBlank();
        assertThat(summary.length()).isLessThan(text.length());
    }

    @Test
    void exercises_come_in_the_number_asked_for_with_a_question_and_an_answer_each() {
        List<Exercise> exercises = adapter().generateExercises(lesson(1200), 5);

        assertThat(exercises).hasSize(5);
        assertThat(exercises).allSatisfy(exercise -> {
            assertThat(exercise.question()).isNotBlank();
            assertThat(exercise.answer()).isNotBlank();
        });
    }

    @Test
    void exercises_from_a_text_longer_than_one_chunk_come_in_the_number_asked_for() {
        List<Exercise> exercises = adapter().generateExercises(lesson(25_000), 5);

        assertThat(exercises).hasSize(5);
    }

    @Test
    void a_smaller_count_gives_a_smaller_set() {
        assertThat(adapter().generateExercises(lesson(1200), 2)).hasSize(2);
    }
}
