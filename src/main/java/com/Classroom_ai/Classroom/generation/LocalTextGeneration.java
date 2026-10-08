package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Talks to a local model server (llama.cpp, for one) that speaks the OpenAI chat-completions API. Selected by
 * {@code generation.adapter=local}; the server is {@code generation.base-url}.
 */
@Component
@ConditionalOnProperty(name = "generation.adapter", havingValue = "local")
public class LocalTextGeneration implements TextGeneration {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");
    private static final int MAX_ROUNDS = 5;

    private static final String SUMMARISE =
            "You summarise course material for students. Answer in the language of the text.";
    private static final String SUMMARISE_PART =
            "You summarise one part of a longer course document. Keep the key points. Answer in the language of the text.";
    private static final String EXERCISES =
            "You write exercises for students from course material. An exercise is an open question with a model "
                    + "answer. Write in the language of the text. Reply with only a JSON array of objects with the "
                    + "keys \"question\" and \"answer\".";
    private static final String COMBINE =
            "You are given summaries of consecutive parts of one course document. Write one summary of the whole "
                    + "document. Answer in the language of the summaries.";

    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final int chunkChars;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public LocalTextGeneration(@Value("${generation.base-url:}") String baseUrl,
                               @Value("${generation.model:}") String model,
                               @Value("${generation.timeout:120s}") Duration timeout,
                               @Value("${generation.chunk-chars:10000}") int chunkChars,
                               ObjectMapper mapper) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("generation.base-url must be set when generation.adapter=local");
        }
        this.endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/v1/chat/completions");
        this.model = model;
        this.timeout = timeout;
        this.chunkChars = chunkChars;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Text that fits in one chunk is summarised in one request. Longer text is cut into chunks, each chunk is
     * summarised, and the summaries are joined and summarised in turn, again in chunks if they are still too long.
     */
    @Override
    public String summarize(String text) {
        List<String> chunks = split(text.strip());
        if (chunks.size() == 1) {
            return chat(SUMMARISE, chunks.get(0));
        }
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<String> summaries = new ArrayList<>();
            for (String chunk : chunks) {
                summaries.add(chat(SUMMARISE_PART, chunk));
            }
            String joined = String.join("\n\n", summaries);
            if (joined.length() <= chunkChars) {
                return chat(COMBINE, joined);
            }
            chunks = split(joined);
        }
        throw new GenerationFailure("BAD_OUTPUT", "The model's summaries did not get shorter.");
    }

    /** Cuts at whitespace so that no chunk is longer than the limit and no word is split. */
    private List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkChars, text.length());
            if (end < text.length()) {
                int space = end;
                while (space > start && !Character.isWhitespace(text.charAt(space))) {
                    space--;
                }
                end = space > start ? space : end;
            }
            chunks.add(text.substring(start, end).strip());
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        return chunks.isEmpty() ? List.of("") : chunks;
    }

    /**
     * The exercises are shared out over the chunks of the text, spread from the start to the end, so a long
     * document is asked about throughout and not only at the beginning.
     */
    @Override
    public List<Exercise> generateExercises(String text, int count) {
        List<String> chunks = split(text.strip());
        int[] perChunk = new int[chunks.size()];
        for (int i = 0; i < count; i++) {
            perChunk[(int) ((i + 0.5) * chunks.size() / count)]++;
        }
        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (perChunk[i] > 0) {
                exercises.addAll(exercisesFrom(chunks.get(i), perChunk[i]));
            }
        }
        return exercises;
    }

    /** Asks for JSON; if the reply is not the right shape, says so and asks once more. */
    private List<Exercise> exercisesFrom(String chunk, int count) {
        List<Map<String, String>> messages = new ArrayList<>(List.of(
                message("system", EXERCISES),
                message("user", "Write exactly " + count + " exercises from this text.\n\nText:\n" + chunk)));
        String reply = chat(messages);
        List<Exercise> exercises = parseExercises(reply, count);
        if (exercises == null) {
            messages.add(message("assistant", reply));
            messages.add(message("user", "That was not valid. Reply with only a JSON array of exactly " + count
                    + " objects, each with the keys \"question\" and \"answer\"."));
            exercises = parseExercises(chat(messages), count);
        }
        if (exercises == null) {
            throw new GenerationFailure("BAD_OUTPUT", "The model did not return exercises in the expected form.");
        }
        return exercises;
    }

    /** The first {@code count} exercises of the JSON array in the reply, or null if the reply is not one. */
    private List<Exercise> parseExercises(String reply, int count) {
        int open = reply.indexOf('[');
        int close = reply.lastIndexOf(']');
        if (open < 0 || close < open) {
            return null;
        }
        try {
            JsonNode array = mapper.readTree(reply.substring(open, close + 1));
            if (!array.isArray() || array.size() < count) {
                return null;
            }
            List<Exercise> exercises = new ArrayList<>();
            for (JsonNode item : array) {
                String question = item.path("question").asText("").strip();
                String answer = item.path("answer").asText("").strip();
                if (!item.path("question").isTextual() || !item.path("answer").isTextual()
                        || question.isEmpty() || answer.isEmpty()) {
                    return null;
                }
                exercises.add(new Exercise(question, answer));
            }
            return exercises.subList(0, count);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public String modelName() {
        return model;
    }

    /** One round trip: a system and a user message in, the assistant's text out. */
    String chat(String system, String user) {
        return chat(List.of(message("system", system), message("user", user)));
    }

    private static Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private String chat(List<Map<String, String>> messages) {
        String body;
        try {
            body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", 0.2,
                    "chat_template_kwargs", Map.of("enable_thinking", false)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new GenerationFailure("MODEL_TIMEOUT", "The model did not answer in time.", e);
        } catch (IOException e) {
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The model server could not be reached.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The request was interrupted.", e);
        }
        if (response.statusCode() != 200) {
            throw new GenerationFailure("MODEL_ERROR", "The model server answered with status " + response.statusCode() + ".");
        }
        try {
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply with no text.");
            }
            return THINK_BLOCK.matcher(content.asText()).replaceAll("").strip();
        } catch (JsonProcessingException e) {
            throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply that is not JSON.", e);
        }
    }
}
