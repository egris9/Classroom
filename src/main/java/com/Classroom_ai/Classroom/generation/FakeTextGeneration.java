package com.Classroom_ai.Classroom.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Deterministic stand-in for a model: it quotes the text it is given. Selected by {@code generation.adapter=fake}. */
@Component
@ConditionalOnProperty(name = "generation.adapter", havingValue = "fake", matchIfMissing = true)
public class FakeTextGeneration implements TextGeneration {

    @Override
    public String summarize(String text, SummaryLevel level) {
        int length = switch (level) {
            case SHORT -> 60;
            case MEDIUM -> 120;
            case LONG -> 240;
            case EXTRA_LONG -> 480;
        };
        String flat = text.replaceAll("\s+", " ").strip();
        return "Demo summary (no model connected): " + (flat.length() > length ? flat.substring(0, length).strip() : flat);
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
    public void chat(List<ChatMessage> history, Consumer<String> onDelta) {
        String question = history.stream()
                .filter(message -> message.role().equals("user"))
                .reduce((first, last) -> last)
                .map(ChatMessage::content)
                .orElse("");
        String[] words = ("Demo reply (no model connected): " + question).strip().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            onDelta.accept(i == 0 ? words[i] : " " + words[i]);
        }
    }

    @Override
    public String modelName() {
        return "fake";
    }
}
