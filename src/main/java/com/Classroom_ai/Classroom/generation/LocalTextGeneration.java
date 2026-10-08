package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
    static final int DEFAULT_MAX_CHUNKS = 40;

    private static final String SUMMARY_FORMAT =
            "Write the summary in your own words. Do not copy sentences from the text, and use no ellipses. "
                    + "Start with an overview of one or two sentences, then give 4 to 8 bullet points with the key "
                    + "points, and put the key terms in bold. Answer in the language of the text.";
    private static final String SUMMARISE =
            "You summarise course material for students. " + SUMMARY_FORMAT;
    private static final String SUMMARISE_PART =
            "You summarise one part of a longer course document. Keep the key points. " + SUMMARY_FORMAT;
    private static final String EXERCISES =
            "You write exercises for students from course material. An exercise is an open question with a model "
                    + "answer. Write in the language of the text. Reply with only a JSON array of objects with the "
                    + "keys \"question\" and \"answer\".";
    private static final String COMBINE =
            "You are given summaries of consecutive parts of one course document. Write one summary of the whole "
                    + "document. " + SUMMARY_FORMAT;
    private static final String CHAT =
            "You are a study assistant for course material. Answer briefly, in the language the user writes in. "
                    + "If you do not know the answer, say that you do not know instead of inventing one.";
    private static final String THINK_OPEN = "<think>";
    private static final String THINK_CLOSE = "</think>";

    /** Ends a streamed reply whose model has gone quiet: a request's timeout covers only the wait for its headers. */
    private static final ScheduledThreadPoolExecutor WATCHDOG = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = new Thread(runnable, "generation-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    static {
        WATCHDOG.setRemoveOnCancelPolicy(true);
    }

    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final int chunkChars;
    private final int maxChunks;
    private final ObjectMapper mapper;
    private final HttpClient client;

    LocalTextGeneration(String baseUrl, String model, Duration timeout, int chunkChars, ObjectMapper mapper) {
        this(baseUrl, model, timeout, chunkChars, DEFAULT_MAX_CHUNKS, mapper);
    }

    @Autowired
    public LocalTextGeneration(@Value("${generation.base-url:}") String baseUrl,
                               @Value("${generation.model:}") String model,
                               @Value("${generation.timeout:120s}") Duration timeout,
                               @Value("${generation.chunk-chars:10000}") int chunkChars,
                               @Value("${generation.max-chunks:40}") int maxChunks,
                               ObjectMapper mapper) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("generation.base-url must be set when generation.adapter=local");
        }
        if (chunkChars < 1) {
            throw new IllegalStateException("generation.chunk-chars must be at least 1");
        }
        if (maxChunks < 1) {
            throw new IllegalStateException("generation.max-chunks must be at least 1");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("generation.timeout must be a positive duration such as 120s");
        }
        this.maxChunks = maxChunks;
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
        List<String> chunks = chunksOf(text);
        if (chunks.size() == 1) {
            return summary(SUMMARISE, chunks.get(0));
        }
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<String> summaries = new ArrayList<>();
            for (String chunk : chunks) {
                summaries.add(summary(SUMMARISE_PART, chunk));
            }
            String joined = String.join("\n\n", summaries);
            if (joined.length() <= chunkChars) {
                return summary(COMBINE, joined);
            }
            chunks = split(joined);
        }
        throw new GenerationFailure("BAD_OUTPUT", "The model's summaries did not get shorter.");
    }

    /** The chunks of the text, or {@code TOO_LONG} if there are more than {@code generation.max-chunks}. */
    private List<String> chunksOf(String text) {
        List<String> chunks = split(text.strip());
        if (chunks.size() > maxChunks) {
            throw new GenerationFailure("TOO_LONG", "This document is too long for the model to work on in one go.");
        }
        return chunks;
    }

    /** A reply to a summarising request; one with no text left in it is a failure, not a blank summary. */
    private String summary(String system, String text) {
        String reply = chat(system, text);
        if (reply.isBlank()) {
            throw new GenerationFailure("BAD_OUTPUT", "The model returned an empty summary.");
        }
        return reply;
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
        List<String> chunks = chunksOf(text);
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

    /**
     * The first {@code count} exercises of the first JSON array in the reply that has the right shape, or null.
     * Every '[' is tried in turn, so prose such as "5 [easy] exercises:" before the array does not hide it.
     */
    private List<Exercise> parseExercises(String reply, int count) {
        for (int open = reply.indexOf('['); open >= 0; open = reply.indexOf('[', open + 1)) {
            List<Exercise> exercises = exercisesAt(reply.substring(open), count);
            if (exercises != null) {
                return exercises;
            }
        }
        return null;
    }

    private List<Exercise> exercisesAt(String json, int count) {
        try {
            JsonNode array = mapper.readTree(json);
            if (array == null || !array.isArray() || array.size() < count) {
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

    private HttpRequest request(Map<String, Object> body) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return client.send(request, handler);
        } catch (HttpTimeoutException e) {
            throw new GenerationFailure("MODEL_TIMEOUT", "The model did not answer in time.", e);
        } catch (IOException e) {
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The model server could not be reached.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The request was interrupted.", e);
        }
    }

    @Override
    public void chat(List<ChatMessage> history, Consumer<String> onDelta) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", CHAT));
        for (ChatMessage turn : history) {
            messages.add(message(turn.role(), turn.content()));
        }
        HttpRequest request = request(Map.of(
                "model", model,
                "messages", messages,
                "stream", true,
                "temperature", 0.3,
                "chat_template_kwargs", Map.of("enable_thinking", false)));
        HttpResponse<InputStream> response = send(request, HttpResponse.BodyHandlers.ofInputStream());
        InputStream body = response.body();
        try {
            if (response.statusCode() != 200) {
                throw new GenerationFailure("MODEL_ERROR", "The model server answered with status " + response.statusCode() + ".");
            }
            streamReply(body, onDelta);
        } finally {
            closeQuietly(body);
        }
    }

    /**
     * Reads the server-sent events of a streamed reply and hands the text to {@code onDelta}. The reply is complete
     * at {@code [DONE]}, or at the end of a stream that has reported a finish reason; a stream that stops short of
     * that, or goes quiet for longer than the timeout, is a failure.
     */
    private void streamReply(InputStream body, Consumer<String> onDelta) {
        AtomicBoolean quiet = new AtomicBoolean();
        Runnable giveUp = () -> {
            quiet.set(true);
            closeQuietly(body);
        };
        ScheduledFuture<?> watch = WATCHDOG.schedule(giveUp, timeout.toMillis(), TimeUnit.MILLISECONDS);
        ReplyFilter reply = new ReplyFilter(onDelta);
        boolean done = false;
        boolean finishSeen = false;
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            for (String line = lines.readLine(); line != null && !done; line = lines.readLine()) {
                watch.cancel(false);
                watch = WATCHDOG.schedule(giveUp, timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).strip();
                if (data.equals("[DONE]")) {
                    done = true;
                    continue;
                }
                JsonNode chunk = readChunk(data);
                if (chunk.has("error")) {
                    throw new GenerationFailure("MODEL_ERROR", "The model server reported an error while answering.");
                }
                JsonNode choice = chunk.path("choices").path(0);
                finishSeen |= choice.path("finish_reason").isTextual();
                JsonNode content = choice.path("delta").path("content");
                if (content.isTextual()) {
                    reply.accept(content.asText());
                }
            }
        } catch (IOException e) {
            if (quiet.get()) {
                throw new GenerationFailure("MODEL_TIMEOUT", "The model stopped answering in time.", e);
            }
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The connection to the model server was lost.", e);
        } finally {
            watch.cancel(false);
        }
        if (!done && !finishSeen) {
            if (quiet.get()) {
                throw new GenerationFailure("MODEL_TIMEOUT", "The model stopped answering in time.");
            }
            throw new GenerationFailure("MODEL_ERROR", "The model stopped before it finished its reply.");
        }
        reply.finish();
    }

    private JsonNode readChunk(String data) {
        try {
            return mapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply that is not JSON.", e);
        }
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            // nothing left to read; the connection is gone either way
        }
    }

    /**
     * Turns the pieces a model streams into the pieces the caller gets: no {@code <think>} block (a tag may arrive
     * split across pieces), no blank piece, and no whitespace before the first text or after the last. Whitespace
     * between two pieces of text travels with the next piece, so the pieces joined are the reply, stripped.
     */
    private static final class ReplyFilter {

        private final Consumer<String> out;
        private final StringBuilder pending = new StringBuilder();
        private final StringBuilder held = new StringBuilder();
        private boolean thinking;
        private boolean sent;

        ReplyFilter(Consumer<String> out) {
            this.out = out;
        }

        void accept(String piece) {
            pending.append(piece);
            while (true) {
                String tag = thinking ? THINK_CLOSE : THINK_OPEN;
                int at = pending.indexOf(tag);
                if (at >= 0) {
                    emitUnlessThinking(pending.substring(0, at));
                    pending.delete(0, at + tag.length());
                    thinking = !thinking;
                    continue;
                }
                int settled = pending.length() - partialTagAtEnd(pending, tag);
                emitUnlessThinking(pending.substring(0, settled));
                pending.delete(0, settled);
                return;
            }
        }

        /** The stream is over: what was held back as a possible tag was text after all. A think block left open is cut off. */
        void finish() {
            emitUnlessThinking(pending.toString());
            pending.setLength(0);
            if (!sent) {
                throw new GenerationFailure("BAD_OUTPUT", "The model returned an empty reply.");
            }
        }

        private void emitUnlessThinking(String text) {
            if (thinking) {
                return;
            }
            String all = held + text;
            String body = all.stripTrailing();
            held.setLength(0);
            held.append(all, body.length(), all.length());
            if (!sent) {
                body = body.stripLeading();
            }
            if (!body.isEmpty()) {
                sent = true;
                out.accept(body);
            }
        }

        /** How many characters at the end of the text could be the start of the tag. */
        private static int partialTagAtEnd(CharSequence text, String tag) {
            for (int length = Math.min(tag.length() - 1, text.length()); length > 0; length--) {
                if (tag.startsWith(text.subSequence(text.length() - length, text.length()).toString())) {
                    return length;
                }
            }
            return 0;
        }
    }

    private String chat(List<Map<String, String>> messages) {
        HttpResponse<String> response = send(request(Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.2,
                "chat_template_kwargs", Map.of("enable_thinking", false))), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new GenerationFailure("MODEL_ERROR", "The model server answered with status " + response.statusCode() + ".");
        }
        try {
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply with no text.");
            }
            String reply = THINK_BLOCK.matcher(content.asText()).replaceAll("");
            int unfinished = reply.indexOf("<think>");
            return (unfinished >= 0 ? reply.substring(0, unfinished) : reply).strip();
        } catch (JsonProcessingException e) {
            throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply that is not JSON.", e);
        }
    }
}
