package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The eval command: every PDF in {@code eval/pdfs/} goes through both operations of the local model, and the
 * outputs and timings land in {@code eval/out/}. See {@code eval/README.md}. It runs only with {@code -Deval=true}.
 * OCR is off unless OCR_COMMAND names the Tesseract program.
 */
@EnabledIfSystemProperty(named = "eval", matches = "true")
class EvalRun {

    @Test
    void run_every_pdf_in_eval_pdfs_through_the_local_model() throws Exception {
        String baseUrl = System.getenv("GENERATION_BASE_URL");
        assertThat(baseUrl).as("GENERATION_BASE_URL must name the model server").isNotBlank();
        Duration timeout = Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("GENERATION_TIMEOUT_SECONDS", "300")));
        Duration longTimeout = Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("GENERATION_LONG_TIMEOUT_SECONDS", "600")));
        int chunkChars = Integer.parseInt(System.getenv().getOrDefault("GENERATION_CHUNK_CHARS", "10000"));
        String caps = System.getenv("GENERATION_SUMMARY_MAX_TOKENS");
        List<Integer> summaryMaxTokens = caps == null || caps.isBlank() ? LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS
                : Arrays.stream(caps.split(",")).map(cap -> Integer.valueOf(cap.strip())).toList();
        TextGeneration model = new LocalTextGeneration(baseUrl, System.getenv().getOrDefault("GENERATION_MODEL", ""),
                timeout, longTimeout, chunkChars, LocalTextGeneration.DEFAULT_MAX_CHUNKS, summaryMaxTokens, new ObjectMapper());

        List<EvalResult> results = new EvalRunner(model, pdfText(), GenerationService.EXERCISES_PER_SET)
                .run(Path.of("eval", "pdfs"), Path.of("eval", "out"));

        results.forEach(result -> System.out.println("eval: " + result));
        System.out.println("eval: results written to eval/out/results.md");
    }

    /** With OCR_COMMAND set, scans and PDFs with garbled text are read by Tesseract, as the app does with OCR on. */
    private static PdfText pdfText() {
        String command = System.getenv("OCR_COMMAND");
        if (command == null || command.isBlank()) {
            return new PdfText();
        }
        int dpi = Integer.parseInt(System.getenv().getOrDefault("OCR_DPI", "300"));
        Ocr ocr = new TesseractOcr(command, System.getenv().getOrDefault("OCR_LANGUAGES", "ara+fra+eng"),
                Duration.ofSeconds(60), dpi);
        return new PdfText(ocr, dpi, 80);
    }
}
