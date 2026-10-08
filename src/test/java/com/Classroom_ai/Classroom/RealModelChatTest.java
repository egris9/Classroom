package com.Classroom_ai.Classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A chat reply from the real model, streamed through the whole stack over real HTTP. It runs only when
 * GENERATION_BASE_URL is set in the environment, for example {@code GENERATION_BASE_URL=http://127.0.0.1:8081};
 * GENERATION_MODEL and GENERATION_TIMEOUT_SECONDS are optional.
 */
@EnabledIfEnvironmentVariable(named = "GENERATION_BASE_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "generation.adapter=local",
        "generation.base-url=${GENERATION_BASE_URL}",
        "generation.model=${GENERATION_MODEL:}",
        "generation.timeout=${GENERATION_TIMEOUT_SECONDS:180}s"
})
@AutoConfigureMockMvc
class RealModelChatTest extends ToolsTestSupport {

    @LocalServerPort int port;

    @Test
    void aRealReplyStreamsInSeveralPiecesAndEndsWithDone() throws Exception {
        String auth = bearer(newUser("Signed"));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/tools/chat"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", auth)
                .POST(HttpRequest.BodyPublishers.ofString("{\"messages\":[{\"role\":\"user\","
                        + "\"content\":\"In two sentences, what is photosynthesis?\"}]}"))
                .build();
        List<String> events = new ArrayList<>();
        StringBuilder reply = new StringBuilder();

        HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                if (line.startsWith("event:")) {
                    events.add(line.substring("event:".length()).strip());
                } else if (line.startsWith("data:") && events.get(events.size() - 1).equals("delta")) {
                    reply.append(line);
                }
            }
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(events).doesNotContain("error");
        assertThat(events.get(events.size() - 1)).isEqualTo("done");
        assertThat(events.stream().filter("delta"::equals).count()).isGreaterThan(3);
        assertThat(reply.toString()).isNotBlank().doesNotContain("<think>");
    }
}
