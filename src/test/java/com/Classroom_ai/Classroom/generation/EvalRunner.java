package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Runs every PDF in a folder through both operations of a {@link TextGeneration} and writes what came out, with the
 * time each took, so a person can read the results and judge the model. A failure is recorded and the run goes on.
 */
final class EvalRunner {

    private final TextGeneration generation;
    private final PdfText pdfText;
    private final int exercisesPerSet;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    EvalRunner(TextGeneration generation, PdfText pdfText, int exercisesPerSet) {
        this.generation = generation;
        this.pdfText = pdfText;
        this.exercisesPerSet = exercisesPerSet;
    }

    List<EvalResult> run(Path pdfDir, Path outDir) throws IOException {
        List<Path> pdfs = pdfsIn(pdfDir);
        if (pdfs.isEmpty()) {
            throw new IllegalArgumentException("No PDFs found in " + pdfDir);
        }
        Files.createDirectories(outDir);
        List<EvalResult> results = new ArrayList<>();
        for (Path pdf : pdfs) {
            results.add(evaluate(pdf, outDir));
        }
        Files.writeString(outDir.resolve("results.md"), table(results), StandardCharsets.UTF_8);
        return results;
    }

    private static List<Path> pdfsIn(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted().toList();
        }
    }

    private EvalResult evaluate(Path pdf, Path outDir) throws IOException {
        String fileName = pdf.getFileName().toString();
        String base = fileName.substring(0, fileName.length() - ".pdf".length());
        String text;
        try {
            text = pdfText.extract(new FileSystemResource(pdf));
        } catch (GenerationFailure failure) {
            String report = "FAILED " + failure.code() + ": " + failure.getMessage();
            Files.writeString(outDir.resolve(base + ".summary.txt"), report, StandardCharsets.UTF_8);
            Files.writeString(outDir.resolve(base + ".exercises.json"), report, StandardCharsets.UTF_8);
            return new EvalResult(fileName, 0, failure.code(), 0, failure.code(), 0);
        }
        Timed summary = timed(() -> generation.summarize(text));
        Files.writeString(outDir.resolve(base + ".summary.txt"), summary.output(), StandardCharsets.UTF_8);
        Timed exercises = timed(() -> {
            try {
                return mapper.writeValueAsString(generation.generateExercises(text, exercisesPerSet));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Files.writeString(outDir.resolve(base + ".exercises.json"), exercises.output(), StandardCharsets.UTF_8);
        return new EvalResult(fileName, text.length(), summary.status(), summary.millis(),
                exercises.status(), exercises.millis());
    }

    private record Timed(String status, long millis, String output) {
    }

    private Timed timed(Supplier<String> operation) {
        long start = System.nanoTime();
        try {
            String output = operation.get();
            return new Timed("DONE", (System.nanoTime() - start) / 1_000_000, output);
        } catch (GenerationFailure failure) {
            return new Timed(failure.code(), (System.nanoTime() - start) / 1_000_000,
                    "FAILED " + failure.code() + ": " + failure.getMessage());
        } catch (RuntimeException e) {
            return new Timed("GENERATION_FAILED", (System.nanoTime() - start) / 1_000_000, "FAILED: " + e);
        }
    }

    private String table(List<EvalResult> results) {
        StringBuilder table = new StringBuilder("# Eval results\n\nModel: " + generation.modelName() + "\n\n"
                + "| PDF | Text chars | Summary | Summary ms | Exercises | Exercises ms |\n"
                + "|---|---|---|---|---|---|\n");
        for (EvalResult r : results) {
            table.append("| ").append(r.pdf()).append(" | ").append(r.textChars()).append(" | ")
                    .append(r.summaryStatus()).append(" | ").append(r.summaryMillis()).append(" | ")
                    .append(r.exercisesStatus()).append(" | ").append(r.exercisesMillis()).append(" |\n");
        }
        return table.toString();
    }
}
