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
 * Runs every PDF in a folder through both operations of a {@link TextGeneration} (a summary at each level, and an
 * exercise set) and writes what came out, with the time each took and the word count of each summary, so a person
 * can read the results and judge the model. A failure is recorded and the run goes on.
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
            text = pdfText.read(new FileSystemResource(pdf)).text();
        } catch (GenerationFailure failure) {
            String report = "FAILED " + failure.code() + ": " + failure.getMessage();
            List<EvalResult.LevelResult> unread = new ArrayList<>();
            for (SummaryLevel level : SummaryLevel.values()) {
                Files.writeString(summaryFile(outDir, base, level), report, StandardCharsets.UTF_8);
                unread.add(new EvalResult.LevelResult(level, failure.code(), 0, 0));
            }
            Files.writeString(outDir.resolve(base + ".exercises.json"), report, StandardCharsets.UTF_8);
            Files.writeString(outDir.resolve(base + ".exercises.hard.json"), report, StandardCharsets.UTF_8);
            return new EvalResult(fileName, 0, unread, failure.code(), 0, failure.code(), 0);
        }
        List<EvalResult.LevelResult> summaries = new ArrayList<>();
        for (SummaryLevel level : SummaryLevel.values()) {
            Timed summary = timed(() -> generation.summarize(text, level));
            Files.writeString(summaryFile(outDir, base, level), summary.output(), StandardCharsets.UTF_8);
            int words = summary.status().equals("DONE") ? summary.output().strip().split("\\s+").length : 0;
            summaries.add(new EvalResult.LevelResult(level, summary.status(), words, summary.millis()));
        }
        Timed exercises = exerciseSet(text, ExerciseOptions.of(null, null, exercisesPerSet));
        Files.writeString(outDir.resolve(base + ".exercises.json"), exercises.output(), StandardCharsets.UTF_8);
        Timed hard = exerciseSet(text, ExerciseOptions.of(List.of(ExerciseType.values()), Difficulty.HARD, ExerciseOptions.MAX_COUNT));
        Files.writeString(outDir.resolve(base + ".exercises.hard.json"), hard.output(), StandardCharsets.UTF_8);
        return new EvalResult(fileName, text.length(), summaries, exercises.status(), exercises.millis(),
                hard.status(), hard.millis());
    }

    private Timed exerciseSet(String text, ExerciseOptions options) {
        return timed(() -> {
            try {
                return mapper.writeValueAsString(generation.generateExercises(text, options));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static Path summaryFile(Path outDir, String base, SummaryLevel level) {
        return outDir.resolve(base + ".summary." + level.name().toLowerCase(Locale.ROOT) + ".txt");
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
                + "## Summaries\n\n"
                + "| PDF | Text chars | Level | Status | Words | ms |\n"
                + "|---|---|---|---|---|---|\n");
        for (EvalResult r : results) {
            for (EvalResult.LevelResult summary : r.summaries()) {
                table.append("| ").append(r.pdf()).append(" | ").append(r.textChars()).append(" | ")
                        .append(summary.level().name().toLowerCase(Locale.ROOT)).append(" | ")
                        .append(summary.status()).append(" | ").append(summary.words()).append(" | ")
                        .append(summary.millis()).append(" |\n");
            }
        }
        table.append("\n## Exercises\n\n| PDF | Status | ms |\n|---|---|---|\n");
        for (EvalResult r : results) {
            table.append("| ").append(r.pdf()).append(" | ").append(r.exercisesStatus()).append(" | ")
                    .append(r.exercisesMillis()).append(" |\n");
        }
        table.append("\n## Hard mixed exercise set (15)\n\n| PDF | Status | ms |\n|---|---|---|\n");
        for (EvalResult r : results) {
            table.append("| ").append(r.pdf()).append(" | ").append(r.hardMixedStatus()).append(" | ")
                    .append(r.hardMixedMillis()).append(" |\n");
        }
        return table.toString();
    }
}
