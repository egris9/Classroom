package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Character.UnicodeScript;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    static final Duration DEFAULT_LONG_TIMEOUT = Duration.ofSeconds(240);
    /**
     * The most tokens a summary may take, for short, medium, long and extra long: about one and a half times the
     * most Qwen3.5 4B wrote for each level over two runs of three course PDFs (253, 577, 1008 and 1611 tokens).
     */
    static final List<Integer> DEFAULT_SUMMARY_MAX_TOKENS = List.of(400, 900, 1550, 2450);

    private static final String OWN_WORDS =
            "Write the summary in your own words. Do not copy sentences from the text, and use no ellipses. ";
    private static final String SAME_LANGUAGE = " Answer in the language of the text.";
    private static final String ONLY_THE_TEXT = " Use only what the text says: if the text is short, write less.";
    private static final String SUMMARISE = "You summarise course material for students. ";
    private static final String SUMMARISE_PART = "You summarise one part of a longer course document. ";
    private static final String PART_DETAILED = OWN_WORDS
            + "Give 6 to 12 bullet points with the key points and the details that support them, and put the key "
            + "terms in bold." + SAME_LANGUAGE;
    private static final String CUT_SHORT = "length";
    /** A model call asks for at most this many exercises; a chunk that owes more is asked again. */
    private static final int MAX_EXERCISES_PER_CALL = 5;
    private static final Pattern ANSWER_LETTER = Pattern.compile("(?i)^\\(?([A-D1-4])[.)]?$");
    private static final String EXERCISES =
            "You write exercises for students from course material. Each exercise has a type: \"open\" (an open "
                    + "question with a model answer), \"multiple_choice\" (a question with four different options, "
                    + "exactly one of them correct) or \"true_false\" (a statement that the student marks true or "
                    + "false). ";
    private static final String EXERCISES_FORMAT =
            " Write in the language of the text. Reply with only a JSON array of objects with the keys \"type\" "
                    + "(\"open\", \"multiple_choice\" or \"true_false\"), \"question\", \"options\" (an array of four "
                    + "strings, for multiple_choice only), \"answer\" (for multiple_choice the full text of the correct "
                    + "option, for true_false \"true\" or \"false\") and \"explanation\" (one sentence saying why, for "
                    + "multiple_choice and true_false).";
    private static final String COMBINE =
            "You are given summaries of consecutive parts of one course document. Write one summary of the whole "
                    + "document. ";
    private static final String CHAT =
            "You are a study assistant for course material. Answer briefly, in the language the user writes in. "
                    + "If you do not know the answer, say that you do not know instead of inventing one.";
    /** Left to itself the model answers Arabic text in English or French, so the language is named for it. */
    private static final String ARABIC_TEXT = " The text is in Arabic, so answer in Arabic.";
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

    private static final Logger log = LoggerFactory.getLogger(LocalTextGeneration.class);

    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final Duration longTimeout;
    private final Map<SummaryLevel, Integer> summaryMaxTokens = new EnumMap<>(SummaryLevel.class);
    private final int chunkChars;
    private final int maxChunks;
    private final ObjectMapper mapper;
    private final HttpClient client;

    LocalTextGeneration(String baseUrl, String model, Duration timeout, int chunkChars, ObjectMapper mapper) {
        this(baseUrl, model, timeout, chunkChars, DEFAULT_MAX_CHUNKS, mapper);
    }

    LocalTextGeneration(String baseUrl, String model, Duration timeout, int chunkChars, int maxChunks,
                        ObjectMapper mapper) {
        this(baseUrl, model, timeout, DEFAULT_LONG_TIMEOUT, chunkChars, maxChunks, DEFAULT_SUMMARY_MAX_TOKENS, mapper);
    }

    @Autowired
    public LocalTextGeneration(@Value("${generation.base-url:}") String baseUrl,
                               @Value("${generation.model:}") String model,
                               @Value("${generation.timeout:120s}") Duration timeout,
                               @Value("${generation.long-timeout:240s}") Duration longTimeout,
                               @Value("${generation.chunk-chars:10000}") int chunkChars,
                               @Value("${generation.max-chunks:40}") int maxChunks,
                               @Value("${generation.summary.max-tokens:400,900,1550,2450}") List<Integer> summaryMaxTokens,
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
        if (longTimeout == null || longTimeout.isZero() || longTimeout.isNegative()) {
            throw new IllegalStateException("generation.long-timeout must be a positive duration such as 240s");
        }
        SummaryLevel[] levels = SummaryLevel.values();
        if (summaryMaxTokens == null || summaryMaxTokens.size() != levels.length
                || summaryMaxTokens.stream().anyMatch(cap -> cap == null || cap < 1)) {
            throw new IllegalStateException("generation.summary.max-tokens must be four positive numbers, for a "
                    + "short, a medium, a long and an extra long summary, such as 400,900,1550,2450");
        }
        for (SummaryLevel level : levels) {
            this.summaryMaxTokens.put(level, summaryMaxTokens.get(level.ordinal()));
        }
        this.longTimeout = longTimeout;
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
     * The level sets the format of the summary; the parts of a long or extra long one are asked for in more detail,
     * so the whole has enough to draw on.
     */
    @Override
    public String summarize(String text, SummaryLevel level) {
        List<String> chunks = chunksOf(text);
        String language = languageOf(text);
        if (chunks.size() == 1) {
            return summary("whole", SUMMARISE + formatOf(level) + language, chunks.get(0), level, level);
        }
        boolean detailed = isLong(level);
        String partFormat = detailed ? PART_DETAILED : "Keep the key points. " + formatOf(SummaryLevel.MEDIUM);
        SummaryLevel partCap = detailed ? SummaryLevel.LONG : SummaryLevel.MEDIUM;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<String> summaries = new ArrayList<>();
            for (String chunk : chunks) {
                summaries.add(summary("part", SUMMARISE_PART + partFormat + language, chunk, level, partCap));
            }
            String joined = String.join("\n\n", summaries);
            if (joined.length() <= chunkChars) {
                return summary("combined", COMBINE + formatOf(level) + language, joined, level, level);
            }
            chunks = split(joined);
        }
        throw new GenerationFailure("BAD_OUTPUT", "The model's summaries did not get shorter.");
    }

    /** What a summary of this level looks like. Every level is written in the model's own words and the text's language. */
    private static String formatOf(SummaryLevel level) {
        String shape = switch (level) {
            case SHORT -> "Start with an overview of one sentence, then give exactly 3 bullet points with the key "
                    + "points, and put the key terms in bold.";
            case MEDIUM -> "Start with an overview of one or two sentences, then give 4 to 8 bullet points with the "
                    + "key points, and put the key terms in bold.";
            case LONG -> "Start with an overview of two or three sentences. Then write 3 to 5 sections. Each section "
                    + "has a heading on its own line that starts with '## ' and 3 to 5 bullet points. Put the key "
                    + "terms in bold." + ONLY_THE_TEXT;
            case EXTRA_LONG -> "Start with an overview of three or four sentences. Then write 5 to 8 sections. Each "
                    + "section has a heading on its own line that starts with '## ', one sentence that introduces "
                    + "it, and 4 to 6 bullet points. End with a section headed '## Key terms', its heading written "
                    + "in the language of the text, that lists each key term in bold with a one-line definition."
                    + ONLY_THE_TEXT;
        };
        return OWN_WORDS + shape + SAME_LANGUAGE;
    }

    private static boolean isLong(SummaryLevel level) {
        return level == SummaryLevel.LONG || level == SummaryLevel.EXTRA_LONG;
    }

    /**
     * What to add to a system prompt about the language of a document: its name when most of its letters are Arabic
     * script, nothing otherwise. It is decided once for the whole document, so every request about it agrees.
     */
    private static String languageOf(String text) {
        long letters = text.codePoints().filter(Character::isLetter).count();
        long arabic = text.codePoints()
                .filter(c -> Character.isLetter(c) && UnicodeScript.of(c) == UnicodeScript.ARABIC)
                .count();
        return arabic * 2 > letters ? ARABIC_TEXT : "";
    }

    /** The chunks of the text, or {@code TOO_LONG} if there are more than {@code generation.max-chunks}. */
    private List<String> chunksOf(String text) {
        List<String> chunks = split(text.strip());
        if (chunks.size() > maxChunks) {
            throw new GenerationFailure("TOO_LONG", "This document is too long for the model to work on in one go.");
        }
        return chunks;
    }

    /**
     * A reply to a summarising request. It may take as many tokens as {@code cap}'s level allows and as long as the
     * summary's own level allows. How much the model writes varies from one try to the next, so a reply that the cap
     * cut off is asked for once more; one cut off again, or with no text left in it, is a failure, not a summary.
     */
    private String summary(String kind, String system, String text, SummaryLevel level, SummaryLevel cap) {
        List<Map<String, String>> messages = List.of(message("system", system), message("user", text));
        Reply reply = summaryReply(kind, messages, level, cap);
        if (CUT_SHORT.equals(reply.finishReason())) {
            reply = summaryReply(kind, messages, level, cap);
        }
        if (CUT_SHORT.equals(reply.finishReason())) {
            throw new GenerationFailure("BAD_OUTPUT", "The summary was cut short. Try a shorter level.");
        }
        if (reply.text().isBlank()) {
            throw new GenerationFailure("BAD_OUTPUT", "The model returned an empty summary.");
        }
        return reply.text();
    }

    private Reply summaryReply(String kind, List<Map<String, String>> messages, SummaryLevel level, SummaryLevel cap) {
        long start = System.nanoTime();
        Reply reply = complete(messages, summaryMaxTokens.get(cap), isLong(level) ? longTimeout : timeout);
        log.debug("Summary call ({}, level {}): {} completion tokens in {} ms, finished by {}", kind, level,
                reply.completionTokens(), (System.nanoTime() - start) / 1_000_000, reply.finishReason());
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
     * document is asked about throughout and not only at the beginning. The types of the plan go to the chunks in
     * turn. A call asks for at most {@value #MAX_EXERCISES_PER_CALL} exercises, so a chunk that owes more is asked
     * again, and every later call lists the questions already written so the model does not repeat them.
     */
    @Override
    public List<Exercise> generateExercises(String text, ExerciseOptions options) {
        int count = options.count();
        List<String> chunks = chunksOf(text);
        String system = EXERCISES + difficultyText(options.difficulty()) + EXERCISES_FORMAT + languageOf(text);
        int[] perChunk = new int[chunks.size()];
        for (int i = 0; i < count; i++) {
            perChunk[(int) ((i + 0.5) * chunks.size() / count)]++;
        }
        List<ExerciseType> plan = options.plan();
        List<Exercise> exercises = new ArrayList<>();
        int planned = 0;
        for (int i = 0; i < chunks.size(); i++) {
            List<ExerciseType> owed = plan.subList(planned, planned + perChunk[i]);
            planned += perChunk[i];
            for (int from = 0; from < owed.size(); from += MAX_EXERCISES_PER_CALL) {
                List<ExerciseType> batch = owed.subList(from, Math.min(from + MAX_EXERCISES_PER_CALL, owed.size()));
                exercises.addAll(exercisesFrom(system, chunks.get(i), batch, exercises));
            }
        }
        return exercises;
    }

    private static String difficultyText(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "Each question asks for one fact from the text, in at most 20 words. Answers are one sentence.";
            case MEDIUM -> "Each question asks the student to explain or apply one idea, in at most 40 words. Answers "
                    + "are 1 to 3 sentences.";
            case HARD -> "Each question asks the student to analyse, compare or combine ideas from different places in "
                    + "the text, and may open with a short scenario, in at most 80 words. Answers are 3 to 5 sentences.";
        };
    }

    /** The name of a type in the JSON the model is asked for. */
    private static String typeName(ExerciseType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    /** "2 of type "open", 1 of type "true_false"", for the types of one call, in enum order. */
    private static String breakdown(Map<ExerciseType, Integer> counts) {
        return counts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getValue() + " of type \"" + typeName(entry.getKey()) + "\"")
                .collect(Collectors.joining(", "));
    }

    /**
     * Asks for JSON with the count of each type; if the reply is not the right shape, says what was wrong and asks
     * once more. A set is never kept half valid.
     */
    private List<Exercise> exercisesFrom(String system, String chunk, List<ExerciseType> types, List<Exercise> written) {
        Map<ExerciseType, Integer> asked = new EnumMap<>(ExerciseType.class);
        types.forEach(type -> asked.merge(type, 1, Integer::sum));
        StringBuilder request = new StringBuilder("Write exactly " + types.size() + " exercises from this text: "
                + breakdown(asked) + ".\n\nText:\n" + chunk);
        if (!written.isEmpty()) {
            request.append("\n\nDo not repeat these questions:");
            written.forEach(exercise -> request.append("\n- ").append(exercise.question()));
        }
        List<Map<String, String>> messages = new ArrayList<>(List.of(
                message("system", system), message("user", request.toString())));
        String reply = chat(messages);
        Parsed parsed = parseExercises(reply, asked);
        if (parsed.exercises() == null) {
            log.debug("The exercises ({}) were not valid, {}, asking once more", breakdown(asked), parsed.problem());
            messages.add(message("assistant", reply));
            messages.add(message("user", "That was not valid: " + parsed.problem() + " Reply with only a JSON array of "
                    + "exactly " + types.size() + " objects (" + breakdown(asked) + "), each with the keys described "
                    + "before."));
            parsed = parseExercises(chat(messages), asked);
        }
        if (parsed.exercises() == null) {
            throw new GenerationFailure("BAD_OUTPUT", "The model did not return exercises in the expected form.");
        }
        return parsed.exercises();
    }

    /** The exercises kept from a reply, or what was wrong with it. */
    private record Parsed(List<Exercise> exercises, String problem) {
    }

    private static final Parsed NO_ARRAY = new Parsed(null, null);

    /** One item of a reply: the exercise it holds, or what is wrong with it. */
    private record Item(Exercise exercise, String problem) {
    }

    /**
     * The exercises of the first JSON array in the reply that holds them, or what was wrong with it. Every '[' is
     * tried in turn, so prose such as "5 [easy] exercises:" before the array does not hide it. An array whose first
     * item is not an object (the options of one exercise, a reference such as "[1]") is not a candidate.
     */
    private Parsed parseExercises(String reply, Map<ExerciseType, Integer> asked) {
        String problem = "there was no JSON array of exercises in the reply.";
        boolean problemKept = false;
        for (int open = reply.indexOf('['); open >= 0; open = reply.indexOf('[', open + 1)) {
            Parsed parsed = exercisesAt(reply.substring(open), asked);
            if (parsed.exercises() != null) {
                return parsed;
            }
            if (parsed.problem() != null && !problemKept) {
                problem = parsed.problem();
                problemKept = true;
            }
        }
        return new Parsed(null, problem);
    }

    /**
     * Every item must be valid. The items are then taken in order for as long as their type still has some of its
     * count left, so an extra item is dropped, and a type that ends with some of its count left is a failure.
     */
    private Parsed exercisesAt(String json, Map<ExerciseType, Integer> asked) {
        JsonNode array;
        try {
            array = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            return NO_ARRAY;
        }
        if (array == null || !array.isArray() || array.isEmpty() || !array.get(0).isObject()) {
            return NO_ARRAY;
        }
        List<Exercise> items = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Item item = exerciseFrom(array.get(i), i + 1);
            if (item.exercise() == null) {
                return new Parsed(null, item.problem());
            }
            items.add(item.exercise());
        }
        Map<ExerciseType, Integer> left = new EnumMap<>(asked);
        List<Exercise> kept = new ArrayList<>();
        for (Exercise exercise : items) {
            if (left.getOrDefault(exercise.type(), 0) > 0) {
                kept.add(exercise);
                left.merge(exercise.type(), -1, Integer::sum);
            }
        }
        String missing = breakdown(left);
        return missing.isEmpty() ? new Parsed(kept, null) : new Parsed(null, "it is missing " + missing + ".");
    }

    private static Item flawed(String problem) {
        return new Item(null, problem);
    }

    private static Item exerciseFrom(JsonNode item, int number) {
        String where = "item " + number;
        if (!item.isObject()) {
            return flawed(where + " is not an object.");
        }
        ExerciseType type = typeOf(item.path("type"));
        if (type == null) {
            return flawed(where + " has an unknown type.");
        }
        String question = text(item.path("question"));
        if (question == null) {
            return flawed(where + " has no question.");
        }
        return switch (type) {
            case OPEN -> {
                String answer = text(item.path("answer"));
                yield answer == null ? flawed(where + " has no answer.") : new Item(new Exercise(question, answer), null);
            }
            case MULTIPLE_CHOICE -> {
                List<String> options = optionsOf(item.path("options"));
                if (options == null) {
                    yield flawed(where + " needs 4 options that are different from each other.");
                }
                String answer = optionFor(item.path("answer"), options);
                yield answer == null ? flawed(where + " needs an answer that is one of its options.")
                        : new Item(new Exercise(type, question, options, answer, text(item.path("explanation"))), null);
            }
            case TRUE_FALSE -> {
                String answer = truthOf(item.path("answer"));
                yield answer == null ? flawed(where + " needs the answer \"true\" or \"false\".")
                        : new Item(new Exercise(type, question, List.of(), answer, text(item.path("explanation"))), null);
            }
        };
    }

    /** A missing type is an open question, so a reply in the old shape still reads. */
    private static ExerciseType typeOf(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return ExerciseType.OPEN;
        }
        if (!node.isTextual()) {
            return null;
        }
        String name = node.asText().strip().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return Arrays.stream(ExerciseType.values()).filter(type -> typeName(type).equals(name)).findFirst().orElse(null);
    }

    /** The text of a string node, stripped, or null if it is not a string or is blank. */
    private static String text(JsonNode node) {
        if (!node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }

    /** Exactly four strings, none blank, no two the same once outer spaces and case are ignored. */
    private static List<String> optionsOf(JsonNode node) {
        if (!node.isArray() || node.size() != 4) {
            return null;
        }
        List<String> options = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode option : node) {
            String text = text(option);
            if (text == null || !seen.add(text.toLowerCase(Locale.ROOT))) {
                return null;
            }
            options.add(text);
        }
        return options;
    }

    /** The option an answer names: its own text, or its letter A to D or number 1 to 4. */
    private static String optionFor(JsonNode node, List<String> options) {
        String raw = node.isNumber() ? String.valueOf(node.asInt()) : text(node);
        if (raw == null) {
            return null;
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(raw)) {
                return option;
            }
        }
        Matcher letter = ANSWER_LETTER.matcher(raw);
        if (!letter.matches()) {
            return null;
        }
        char mark = Character.toUpperCase(letter.group(1).charAt(0));
        return options.get(mark >= 'A' ? mark - 'A' : mark - '1');
    }

    /** {@code "true"} or {@code "false"} from a boolean or a string, or null. */
    private static String truthOf(JsonNode node) {
        if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        }
        String text = text(node);
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.equals("true") || lower.equals("false") ? lower : null;
    }

    @Override
    public String modelName() {
        return model;
    }

    private static Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private HttpRequest request(Map<String, Object> body, Duration timeout) {
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
                "chat_template_kwargs", Map.of("enable_thinking", false)), timeout);
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

    /** What came back from one request that is not streamed: its text, why it ended, and how many tokens it took. */
    private record Reply(String text, String finishReason, int completionTokens) {
    }

    private String chat(List<Map<String, String>> messages) {
        return complete(messages, null, timeout).text();
    }

    /** One round trip. {@code maxTokens} caps the reply, or is null for no cap; the model has {@code timeout} to answer. */
    private Reply complete(List<Map<String, String>> messages, Integer maxTokens, Duration timeout) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("chat_template_kwargs", Map.of("enable_thinking", false));
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        HttpResponse<String> response = send(request(body, timeout), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new GenerationFailure("MODEL_ERROR", "The model server answered with status " + response.statusCode() + ".");
        }
        try {
            JsonNode json = mapper.readTree(response.body());
            JsonNode choice = json.path("choices").path(0);
            JsonNode content = choice.path("message").path("content");
            if (!content.isTextual()) {
                throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply with no text.");
            }
            String reply = THINK_BLOCK.matcher(content.asText()).replaceAll("");
            int unfinished = reply.indexOf("<think>");
            return new Reply((unfinished >= 0 ? reply.substring(0, unfinished) : reply).strip(),
                    choice.path("finish_reason").asText(""), json.path("usage").path("completion_tokens").asInt(-1));
        } catch (JsonProcessingException e) {
            throw new GenerationFailure("MODEL_ERROR", "The model server sent a reply that is not JSON.", e);
        }
    }
}
