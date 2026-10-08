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

    @Test
    void a_blank_base_url_is_refused_at_construction() {
        assertThatThrownBy(() -> adapter(" ", Duration.ofSeconds(5), 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("generation.base-url");
    }
}
