package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalTextGenerationTest {

    private static final String MODEL = "qwen-test";

    private StubModelServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new StubModelServer();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private LocalTextGeneration adapter(String baseUrl, Duration timeout, int chunkChars) {
        return new LocalTextGeneration(baseUrl, MODEL, timeout, chunkChars, new ObjectMapper());
    }

    private LocalTextGeneration adapter() {
        return adapter(server.baseUrl(), Duration.ofSeconds(5), 10_000);
    }

    @Test
    void summarize_sends_the_text_to_chat_completions_and_returns_the_reply() {
        server.replyWith(user -> "A short summary.");

        String summary = adapter().summarize("Photosynthesis turns light into sugar.");

        assertThat(summary).isEqualTo("A short summary.");
        JsonNode request = server.requests().get(0);
        assertThat(request.path("model").asText()).isEqualTo(MODEL);
        assertThat(server.userMessage(0)).contains("Photosynthesis turns light into sugar.");
    }

    @Test
    void summaryRequestAsksForOwnWordsAndNoEllipsis() {
        adapter().summarize("Photosynthesis turns light into sugar.");

        String system = server.requests().get(0).path("messages").path(0).path("content").asText();
        assertThat(system).contains("in your own words").contains("Do not copy sentences").contains("no ellipses");
    }

    @Test
    void requests_switch_the_model_thinking_off() {
        adapter().summarize("Some text.");

        assertThat(server.requests().get(0).path("chat_template_kwargs").path("enable_thinking").asBoolean(true))
                .isFalse();
    }

    @Test
    void the_model_name_is_the_configured_one() {
        assertThat(adapter().modelName()).isEqualTo(MODEL);
    }

    @Test
    void a_reply_has_its_think_block_removed() {
        server.replyWith(user -> "<think>let me think</think>\n\nThe summary.");

        assertThat(adapter().summarize("Some text.")).isEqualTo("The summary.");
    }

    @Test
    void a_server_that_is_down_fails_with_MODEL_UNAVAILABLE() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        LocalTextGeneration down = adapter("http://127.0.0.1:" + closedPort, Duration.ofSeconds(5), 10_000);

        assertThatThrownBy(() -> down.summarize("Some text."))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_UNAVAILABLE"));
    }

    @Test
    void a_server_slower_than_the_timeout_fails_with_MODEL_TIMEOUT() {
        server.delayBy(1500);
        LocalTextGeneration slow = adapter(server.baseUrl(), Duration.ofMillis(300), 10_000);

        assertThatThrownBy(() -> slow.summarize("Some text."))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_TIMEOUT"));
    }

    @Test
    void an_error_status_fails_with_MODEL_ERROR() {
        server.failWith(500);

        assertThatThrownBy(() -> adapter().summarize("Some text."))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_ERROR"));
    }

    private static String words(int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append("word").append(i).append(' ');
        }
        return text.toString().strip();
    }

    @Test
    void text_that_fits_in_one_chunk_costs_one_request() {
        adapter(server.baseUrl(), Duration.ofSeconds(5), 1000).summarize(words(20));

        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void long_text_is_summarised_chunk_by_chunk_and_then_the_summaries_are_summarised() {
        server.replyWith(user -> "S");
        String text = words(60);

        String summary = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text);

        int chunks = server.requests().size() - 1;
        assertThat(chunks).isGreaterThan(1);
        assertThat(summary).isEqualTo("S");
        assertThat(server.userMessage(chunks)).isEqualTo(String.join("\n\n", java.util.Collections.nCopies(chunks, "S")));
    }

    @Test
    void no_chunk_is_longer_than_the_limit_and_no_word_is_cut_in_two() {
        server.replyWith(user -> "S");
        String text = words(60);

        adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text);

        StringBuilder sent = new StringBuilder();
        for (int i = 0; i < server.requests().size() - 1; i++) {
            String chunk = server.userMessage(i);
            assertThat(chunk.length()).isLessThanOrEqualTo(150);
            sent.append(chunk).append(' ');
        }
        assertThat(sent.toString().strip()).isEqualTo(text);
    }

    @Test
    void summaries_that_are_still_too_long_are_summarised_again() {
        server.replyWith(user -> user.startsWith("word") ? "x".repeat(100) : "short");
        String text = words(120);

        String summary = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text);

        assertThat(summary).isEqualTo("short");
        for (JsonNode request : server.requests()) {
            String user = request.path("messages").path(1).path("content").asText();
            assertThat(user.length()).isLessThanOrEqualTo(150);
        }
    }

    @Test
    void summaries_that_never_get_shorter_fail_with_BAD_OUTPUT() {
        server.replyWith(user -> "x".repeat(140));

        assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(120)))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    private static String exercisesJson(int n) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 1; i <= n; i++) {
            json.append(i > 1 ? "," : "").append("{\"question\":\"Q").append(i).append("?\",\"answer\":\"A").append(i).append("\"}");
        }
        return json.append("]").toString();
    }

    /** Answers any exercise request with as many exercises as it asks for ("exactly N"). */
    private static String asManyAsAsked(String user) {
        java.util.regex.Matcher asked = java.util.regex.Pattern.compile("exactly (\\d+)").matcher(user);
        return asked.find() ? exercisesJson(Integer.parseInt(asked.group(1))) : "no number asked";
    }

    @Test
    void exercises_are_read_from_a_json_array_of_question_and_answer() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter().generateExercises("Photosynthesis turns light into sugar.", 5);

        assertThat(exercises).hasSize(5);
        assertThat(exercises.get(0)).isEqualTo(new Exercise("Q1?", "A1"));
        assertThat(server.requests()).hasSize(1);
        assertThat(server.userMessage(0)).contains("Photosynthesis turns light into sugar.").contains("exactly 5");
    }

    @Test
    void a_json_array_inside_a_code_fence_is_accepted() {
        server.replyWith(user -> "Here you go:\n```json\n" + exercisesJson(5) + "\n```");

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
    }

    @Test
    void invalid_output_is_retried_once_and_the_retry_can_succeed() {
        server.thenReply("Sorry, I cannot do that.").replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter().generateExercises("Some text.", 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests()).hasSize(2);
        assertThat(server.userMessage(1)).contains("JSON");
    }

    @Test
    void output_that_is_invalid_twice_fails_with_BAD_OUTPUT_after_two_requests() {
        server.replyWith(user -> "not json at all");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
        assertThat(server.requests()).hasSize(2);
    }

    @Test
    void an_exercise_without_an_answer_is_invalid() {
        server.replyWith(user -> "[{\"question\":\"Q1?\"},{\"question\":\"Q2?\",\"answer\":\"A2\"}]");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 2))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void a_blank_question_is_invalid() {
        server.replyWith(user -> "[{\"question\":\" \",\"answer\":\"A1\"}]");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 1))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void fewer_exercises_than_asked_for_is_invalid() {
        server.replyWith(user -> exercisesJson(3));

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void more_exercises_than_asked_for_are_cut_to_the_count() {
        server.replyWith(user -> exercisesJson(8));

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
    }

    @Test
    void long_text_gets_its_exercises_from_chunks_spread_across_the_text() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);
        String text = words(400);

        var exercises = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).generateExercises(text, 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests()).hasSize(5);
        assertThat(firstWordNumber(server.userMessage(0))).isLessThan(100);
        assertThat(firstWordNumber(server.userMessage(4))).isGreaterThan(300);
        for (int i = 1; i < 5; i++) {
            assertThat(firstWordNumber(server.userMessage(i))).isGreaterThan(firstWordNumber(server.userMessage(i - 1)));
        }
    }

    private static int firstWordNumber(String message) {
        java.util.regex.Matcher word = java.util.regex.Pattern.compile("word(\\d+)").matcher(message);
        assertThat(word.find()).isTrue();
        return Integer.parseInt(word.group(1));
    }

    @Test
    void short_text_in_few_chunks_shares_the_count_between_them() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).generateExercises(words(30), 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests().size()).isGreaterThan(1).isLessThanOrEqualTo(5);
    }

    // --- review fixes ---

    @Test
    void a_chunk_size_below_one_is_refused_at_construction() {
        for (int bad : new int[]{0, -5}) {
            assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), bad))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.chunk-chars");
        }
    }

    @Test
    void a_missing_or_non_positive_timeout_is_refused_at_construction() {
        for (Duration bad : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            assertThatThrownBy(() -> adapter(server.baseUrl(), bad, 10_000))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.timeout");
        }
    }

    @Test
    void text_needing_more_chunks_than_the_limit_fails_with_TOO_LONG_before_any_request() {
        LocalTextGeneration limited = new LocalTextGeneration(server.baseUrl(), MODEL, Duration.ofSeconds(5), 100, 3,
                new ObjectMapper());
        String text = words(200);

        assertThatThrownBy(() -> limited.summarize(text))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("TOO_LONG"));
        assertThatThrownBy(() -> limited.generateExercises(text, 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("TOO_LONG"));
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_reply_that_is_only_a_think_block_is_BAD_OUTPUT_not_a_blank_summary() {
        server.replyWith(user -> "<think>nothing else</think>");

        assertThatThrownBy(() -> adapter().summarize("Some text."))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void a_blank_chunk_summary_does_not_reach_the_combining_request() {
        server.replyWith(user -> "  ");

        assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60)))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void an_unterminated_think_block_is_cut_off_and_never_stored() {
        server.replyWith(user -> "The summary.\n<think>reasoning that never ends");

        assertThat(adapter().summarize("Some text.")).isEqualTo("The summary.");
    }

    @Test
    void a_reply_that_is_only_an_unterminated_think_block_is_BAD_OUTPUT() {
        server.replyWith(user -> "<think>reasoning that never ends");

        assertThatThrownBy(() -> adapter().summarize("Some text."))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void prose_with_brackets_before_the_array_does_not_hide_it() {
        server.replyWith(user -> "Here are 5 [easy] exercises: " + exercisesJson(5));

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void a_reference_after_the_array_does_not_hide_it() {
        server.replyWith(user -> exercisesJson(5) + "\n\nSee [1] for details.");

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void a_blank_base_url_is_refused_at_construction() {
        assertThatThrownBy(() -> adapter(" ", Duration.ofSeconds(5), 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("generation.base-url");
    }
}
