package com.Classroom_ai.Classroom.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Deterministic stand-in for a model: it quotes the text it is given. Selected by {@code generation.adapter=fake}. */
@Component
@ConditionalOnProperty(name = "generation.adapter", havingValue = "fake", matchIfMissing = true)
public class FakeTextGeneration implements TextGeneration {

    private static final int SUMMARY_LENGTH = 120;

    @Override
    public String summarize(String text) {
        String flat = text.replaceAll("\s+", " ").strip();
        return "Demo summary (no model connected): " + (flat.length() > SUMMARY_LENGTH ? flat.substring(0, SUMMARY_LENGTH).strip() : flat);
    }

    @Override
    public List<Exercise> generateExercises(String text, int count) {
        String[] sentences = text.replaceAll("\s+", " ").strip().split("(?<=[.!?]) ");
        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String sentence = sentences[i % sentences.length];
            exercises.add(new Exercise("Question " + (i + 1) + ": what does the course say about \"" + sentence + "\"?", sentence));
        }
        return exercises;
    }

    @Override
    public String modelName() {
        return "fake";
    }
}
