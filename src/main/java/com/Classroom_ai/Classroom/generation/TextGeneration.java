package com.Classroom_ai.Classroom.generation;

import java.util.List;

/** The seam through which the app asks a model for summaries and exercises. */
public interface TextGeneration {

    String summarize(String text);

    List<Exercise> generateExercises(String text, int count);

    /** The name stored beside every result, so a result can be traced to the model that wrote it. */
    String modelName();
}
