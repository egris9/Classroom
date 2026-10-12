package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.course.FileRemoved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Deletes the Summaries, Exercise sets and stored page texts of a CourseFile that is being removed, so no row points
 * at it.
 */
@Component
class GeneratedCleanup {

    private final SummaryRepository summaries;
    private final ExerciseSetRepository exerciseSets;
    private final FilePageTextRepository pageTexts;

    GeneratedCleanup(SummaryRepository summaries, ExerciseSetRepository exerciseSets,
                     FilePageTextRepository pageTexts) {
        this.summaries = summaries;
        this.exerciseSets = exerciseSets;
        this.pageTexts = pageTexts;
    }

    @EventListener
    void on(FileRemoved removed) {
        summaries.deleteAllOf(removed.fileId());
        exerciseSets.deleteAllOf(removed.fileId());
        pageTexts.deleteAllOf(removed.fileId());
    }
}
