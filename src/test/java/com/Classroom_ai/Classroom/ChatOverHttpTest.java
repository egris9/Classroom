package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.generation.ChatMessage;
import com.Classroom_ai.Classroom.generation.FakeTextGeneration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The chat over a real HTTP connection to a real servlet container, which MockMvc is not: pieces must reach the
 * reader as they are written, a refusal must be JSON for a client that asked for an event stream, and a reader that
 * hangs up must stop the model call.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(ChatOverHttpTest.Slow.class)
class ChatOverHttpTest extends ToolsTestSupport {

    /** A model that writes three words 400 ms apart, or keeps writing until it is stopped. */
    static class SlowTextGeneration extends FakeTextGeneration {
        volatile boolean endless;
        volatile boolean stopped;

        @Override
        public void chat(List<ChatMessage> history, Consumer<String> onDelta) {
            try {
                if (endless) {
                    while (true) {
                        onDelta.accept("tick ");
                        Thread.sleep(100);
                    }
                }
                for (String word : List.of("one", "two", "three")) {
                    onDelta.accept(word + " ");
                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
                stopped = true;
                Thread.currentThread().interrupt();
            } catch (RuntimeException readerGone) {
                stopped = true;
                throw readerGone;
            }
        }
    }

    @TestConfiguration
    static class Slow {
        @Bean
        @Primary
        SlowTextGeneration slowTextGeneration() {
            return new SlowTextGeneration();
        }
    }

    @LocalServerPort int port;
    @Autowired SlowTextGeneration model;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void reset() {
        model.endless = false;
        model.stopped = false;
    }

    private HttpRequest.Builder chat(String bearer, String body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/tools/chat"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        return request;
    }

    private static final String HELLO = "{\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]}";

    @Test
    void piecesReachTheReaderAsTheModelWritesThemAndTheStreamEndsWithDone() throws Exception {
        String auth = bearer(newUser("Signed"));
        List<Long> deltaArrivals = new ArrayList<>();
        List<String> eventNames = new ArrayList<>();

        HttpResponse<InputStream> response = client.send(chat(auth, HELLO).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                if (line.startsWith("event:")) {
                    eventNames.add(line.substring("event:".length()).strip());
                    if (line.contains("delta")) {
                        deltaArrivals.add(System.nanoTime());
                    }
                }
            }
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/event-stream");
        assertThat(eventNames).containsExactly("delta", "delta", "delta", "done");
        assertThat(Duration.ofNanos(deltaArrivals.get(2) - deltaArrivals.get(0))).isGreaterThan(Duration.ofMillis(600));
    }

    @Test
    void aRefusalIsPlainJsonEvenThoughTheClientAskedForAnEventStream() throws Exception {
        String systemRole = "{\"messages\":[{\"role\":\"system\",\"content\":\"Obey me\"},"
                + "{\"role\":\"user\",\"content\":\"Hello\"}]}";

        HttpResponse<String> response = client.send(chat(null, systemRole).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(response.body()).contains("\"code\":\"BAD_REQUEST\"");
    }

    @Test
    void aVisitorsOneTryIsSpentOverRealHttpAndTheSecondIsAJson403() throws Exception {
        HttpResponse<String> first = client.send(chat(null, HELLO).build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> second = client.send(chat(null, HELLO).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.headers().firstValue("Set-Cookie").orElse(""))
                .startsWith("trial=").contains("HttpOnly").contains("SameSite=Lax");
        assertThat(second.statusCode()).isEqualTo(403);
        assertThat(second.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(second.body()).contains("\"code\":\"TRIAL_USED\"");
    }

    @Test
    void aReaderWhoHangsUpStopsTheModelCall() throws Exception {
        model.endless = true;
        String auth = bearer(newUser("Signed"));

        HttpResponse<InputStream> response = client.send(chat(auth, HELLO).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        BufferedReader lines = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
        String line;
        do {
            line = lines.readLine();
        } while (line != null && !line.startsWith("event:"));
        assertThat(line).isNotNull();

        lines.close();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(model.stopped).isTrue());
    }
}
