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

    @Override
    public String summarize(String text) {
        return chat("You summarise course material for students. Answer in the language of the text.", text);
    }

    @Override
    public List<Exercise> generateExercises(String text, int count) {
        throw new UnsupportedOperationException("not yet");
    }

    @Override
    public String modelName() {
        return model;
    }

    /** One round trip: a system and a user message in, the assistant's text out. */
    String chat(String system, String user) {
        String body;
        try {
            body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)),
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
