package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The local adapter against a stub server that answers like a well-behaved model. */
class LocalTextGenerationContractTest extends TextGenerationContract {

    private static final Pattern ASKED = Pattern.compile("(\\d+) of type \"(\\w+)\"");

    private StubModelServer server;
    private final AtomicInteger written = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = new StubModelServer().replyToPrompt((system, user) -> {
            Matcher asked = ASKED.matcher(user);
            List<String> items = new ArrayList<>();
            while (asked.find()) {
                for (int i = 0; i < Integer.parseInt(asked.group(1)); i++) {
                    items.add(exercise(asked.group(2), written.incrementAndGet()));
                }
            }
            return items.isEmpty() ? summaryFor(system) : "[" + String.join(",", items) + "]";
        });
    }

    /** One valid exercise of the type asked for; the number keeps every question and option distinct. */
    private static String exercise(String type, int n) {
        return switch (type) {
            case "multiple_choice" -> "{\"type\":\"multiple_choice\",\"question\":\"Q" + n + "?\",\"options\":[\"Red " + n
                    + "\",\"Green " + n + "\",\"Blue " + n + "\",\"Black " + n + "\"],\"answer\":\"Blue " + n
                    + "\",\"explanation\":\"Because.\"}";
            case "true_false" -> "{\"type\":\"true_false\",\"question\":\"Q" + n + " is so.\",\"answer\":\""
                    + (n % 2 == 0) + "\",\"explanation\":\"Because.\"}";
            default -> "{\"type\":\"open\",\"question\":\"Q" + n + "?\",\"answer\":\"A" + n + "\"}";
        };
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
