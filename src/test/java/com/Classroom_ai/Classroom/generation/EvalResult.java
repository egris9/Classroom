package com.Classroom_ai.Classroom.generation;

import java.util.List;

/**
 * One PDF's rows in the eval tables: a summary at each level, in level order, an exercise set, and a hard mixed set
 * of fifteen. A status is {@code DONE} or the code of the failure.
 */
record EvalResult(String pdf, int textChars, List<LevelResult> summaries, String exercisesStatus, long exercisesMillis,
                  String hardMixedStatus, long hardMixedMillis) {

    /** The summary at one level: how it went, how many words it has (0 if it failed) and how long it took. */
    record LevelResult(SummaryLevel level, String status, int words, long millis) {
    }
}
