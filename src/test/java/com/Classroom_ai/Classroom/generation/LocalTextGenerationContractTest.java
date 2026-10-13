package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The local adapter against a stub server that answers like a well-behaved model. */
class LocalTextGenerationContractTest extends TextGenerationContract {

    private static final Pattern ASKED = Pattern.compile("exactly (\\d+)");

    private StubModelServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new StubModelServer().replyToPrompt((system, user) -> {
            Matcher asked = ASKED.matcher(user);
            if (asked.find()) {
                StringBuilder json = new StringBuilder("[");
                for (int i = 1; i <= Integer.parseInt(asked.group(1)); i++) {
                    json.append(i > 1 ? "," : "").append("{\"question\":\"Q").append(i).append("?\",\"answer\":\"A").append(i).append("\"}");
                }
                return json.append("]").toString();
            }
            return summaryFor(system);
        });
    }

    /** A well-behaved model writes more when the format it is given asks for more. */
    private static String summaryFor(String system) {
        String sentence = "A short summary. ";
        if (system.contains("5 to 8 sections")) {
            return sentence.repeat(8).strip();
        }
        if (system.contains("3 to 5 sections")) {
            return sentence.repeat(4).strip();
        }
        return system.contains("exactly 3 bullet points") ? "A summary." : sentence.repeat(2).strip();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Override
    TextGeneration adapter() {
        return new LocalTextGeneration(server.baseUrl(), "stub-model", Duration.ofSeconds(10), 10_000, new ObjectMapper());
    }
}
