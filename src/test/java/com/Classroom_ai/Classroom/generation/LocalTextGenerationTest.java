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

    @Test
    void a_blank_base_url_is_refused_at_construction() {
        assertThatThrownBy(() -> adapter(" ", Duration.ofSeconds(5), 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("generation.base-url");
    }
}
