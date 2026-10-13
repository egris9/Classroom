package com.Classroom_ai.Classroom.generation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The summary settings nobody sets: the adapter Spring builds, the settings file and the code must all name the
 * same token caps and the same long time limit.
 */
@SpringBootTest(properties = {"generation.adapter=local", "generation.model=wired-model"})
class LocalAdapterDefaultsTest {

    private static final StubModelServer SERVER = start();

    private static StubModelServer start() {
        try {
            return new StubModelServer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void modelServer(DynamicPropertyRegistry registry) {
        registry.add("generation.base-url", SERVER::baseUrl);
    }

    @AfterAll
    static void stopServer() {
        SERVER.close();
    }

    @Autowired TextGeneration textGeneration;

    @Test
    void theAdapterSpringBuildsSendsTheDefaultTokenCapOfEachLevel() {
        int before = SERVER.requests().size();

        for (SummaryLevel level : SummaryLevel.values()) {
            textGeneration.summarize("Photosynthesis turns light into sugar.", level);
        }

        List<Integer> sent = new ArrayList<>();
        for (int i = before; i < SERVER.requests().size(); i++) {
            sent.add(SERVER.requests().get(i).path("max_tokens").asInt(-1));
        }
        assertThat(sent).containsExactly(400, 900, 1550, 2450)
                .containsExactlyElementsOf(LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS);
    }

    @Test
    void theSettingsFileNamesTheSameDefaultsAsTheCode() throws IOException {
        Properties settings = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src", "main", "resources", "application.properties"))) {
            settings.load(in);
        }
        String caps = LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS.stream().map(String::valueOf)
                .collect(Collectors.joining(","));

        assertThat(settings.getProperty("generation.summary.max-tokens"))
                .isEqualTo("${GENERATION_SUMMARY_MAX_TOKENS:" + caps + "}");
        assertThat(settings.getProperty("generation.long-timeout"))
                .isEqualTo("${GENERATION_LONG_TIMEOUT:" + LocalTextGeneration.DEFAULT_LONG_TIMEOUT.toSeconds() + "s}");
    }
}
