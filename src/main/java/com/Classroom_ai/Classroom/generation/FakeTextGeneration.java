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
    public List<Exercise> generateExercises(String text, ExerciseOptions options) {
        String[] sentences = text.replaceAll("\s+", " ").strip().split("(?<=[.!?]) ");
        List<ExerciseType> plan = options.plan();
        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            String sentence = sentences[i % sentences.length];
            String number = "Question " + (i + 1) + ": ";
            exercises.add(switch (plan.get(i)) {
                case OPEN -> new Exercise(number + "what does the course say about \"" + sentence + "\"?", sentence);
                case MULTIPLE_CHOICE -> {
                    List<String> choices = new ArrayList<>();
                    for (int k = 0; k < 4; k++) {
                        choices.add(k == i % 4 ? sentence : "Demo option " + (k + 1) + ": not what the course says");
                    }
                    yield new Exercise(ExerciseType.MULTIPLE_CHOICE, number + "which of these does the course say?",
                            choices, sentence, "The course says: " + sentence);
                }
                case TRUE_FALSE -> {
                    boolean isTrue = i % 2 == 0;
                    String statement = isTrue ? sentence : "It is not true that " + sentence;
                    yield new Exercise(ExerciseType.TRUE_FALSE, number + "true or false: " + statement, List.of(),
                            String.valueOf(isTrue), "The course says: " + sentence);
                }
            });
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
