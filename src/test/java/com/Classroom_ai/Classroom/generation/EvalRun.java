package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The eval command: every PDF in {@code eval/pdfs/} goes through both operations of the local model, and the
 * outputs and timings land in {@code eval/out/}. See {@code eval/README.md}. It runs only with {@code -Deval=true}.
 */
@EnabledIfSystemProperty(named = "eval", matches = "true")
class EvalRun {

    @Test
    void run_every_pdf_in_eval_pdfs_through_the_local_model() throws Exception {
        String baseUrl = System.getenv("GENERATION_BASE_URL");
        assertThat(baseUrl).as("GENERATION_BASE_URL must name the model server").isNotBlank();
        Duration timeout = Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("GENERATION_TIMEOUT_SECONDS", "300")));
        int chunkChars = Integer.parseInt(System.getenv().getOrDefault("GENERATION_CHUNK_CHARS", "10000"));
        TextGeneration model = new LocalTextGeneration(baseUrl, System.getenv().getOrDefault("GENERATION_MODEL", ""),
                timeout, chunkChars, new ObjectMapper());

        List<EvalResult> results = new EvalRunner(model, new PdfText(), GenerationService.EXERCISES_PER_SET)
                .run(Path.of("eval", "pdfs"), Path.of("eval", "out"));

        results.forEach(result -> System.out.println("eval: " + result));
        System.out.println("eval: results written to eval/out/results.md");
    }
}
