package com.Classroom_ai.Classroom.generation;

/** One PDF's row in the eval table. A status is {@code DONE} or the code of the failure. */
record EvalResult(String pdf, int textChars, String summaryStatus, long summaryMillis,
                  String exercisesStatus, long exercisesMillis) {
}
