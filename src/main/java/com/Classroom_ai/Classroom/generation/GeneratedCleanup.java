package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.course.FileRemoved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Deletes the Summaries and Exercise sets of a CourseFile that is being removed, so no row points at it. */
@Component
class GeneratedCleanup {

    private final SummaryRepository summaries;
    private final ExerciseSetRepository exerciseSets;

    GeneratedCleanup(SummaryRepository summaries, ExerciseSetRepository exerciseSets) {
        this.summaries = summaries;
        this.exerciseSets = exerciseSets;
    }

    @EventListener
    void on(FileRemoved removed) {
        summaries.deleteAllOf(removed.fileId());
        exerciseSets.deleteAllOf(removed.fileId());
    }
}
