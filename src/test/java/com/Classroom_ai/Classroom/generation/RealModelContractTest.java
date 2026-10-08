package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

/**
 * The local adapter against the real model server. It runs only when GENERATION_BASE_URL is set in the environment,
 * for example {@code GENERATION_BASE_URL=http://127.0.0.1:8081}. GENERATION_MODEL and GENERATION_TIMEOUT are optional.
 */
@EnabledIfEnvironmentVariable(named = "GENERATION_BASE_URL", matches = ".+")
class RealModelContractTest extends TextGenerationContract {

    @Override
    TextGeneration adapter() {
        String model = System.getenv().getOrDefault("GENERATION_MODEL", "");
        Duration timeout = Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("GENERATION_TIMEOUT_SECONDS", "180")));
        return new LocalTextGeneration(System.getenv("GENERATION_BASE_URL"), model, timeout, 10_000, new ObjectMapper());
    }
}
