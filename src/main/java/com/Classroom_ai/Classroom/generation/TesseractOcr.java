package com.Classroom_ai.Classroom.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OCR by Tesseract, run as a separate program for each page ({@code generation.ocr.adapter=tesseract}). A crash or
 * a page that never finishes stays in that program: it is killed after {@code generation.ocr.page-timeout} and the
 * page fails with {@code OCR_FAILED}. The app refuses to start when the program or one of the configured languages
 * is not installed.
 */
@Component
@ConditionalOnProperty(name = "generation.ocr.adapter", havingValue = "tesseract")
public class TesseractOcr implements Ocr {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcr.class);
    private static final Duration LIST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);
    private static final int DELETE_ATTEMPTS = 20;
    private static final Duration DELETE_PAUSE = Duration.ofMillis(100);

    private final String command;
    private final String languages;
    private final Duration pageTimeout;
    private final int dpi;

    public TesseractOcr(@Value("${generation.ocr.command:tesseract}") String command,
                        @Value("${generation.ocr.languages:ara+fra+eng}") String languages,
                        @Value("${generation.ocr.page-timeout:60s}") Duration pageTimeout,
                        @Value("${generation.ocr.dpi:300}") int dpi) {
        if (command == null || command.isBlank()) {
            throw new IllegalStateException("generation.ocr.command must be set when generation.ocr.adapter=tesseract");
        }
        if (languages == null || languages.isBlank()) {
            throw new IllegalStateException("generation.ocr.languages must name at least one language, such as ara+fra+eng");
        }
        if (pageTimeout == null || pageTimeout.isZero() || pageTimeout.isNegative()) {
            throw new IllegalStateException("generation.ocr.page-timeout must be a positive duration such as 60s");
        }
        this.command = command.strip();
        this.languages = languages.strip();
        this.pageTimeout = pageTimeout;
        this.dpi = dpi;
        requireInstalled();
        log.info("OCR is using Tesseract with the languages {}", this.languages);
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String read(BufferedImage page) {
        Path image = null;
        Path text = null;
        Process process = null;
        try {
            image = Files.createTempFile("ocr-page-", ".png");
            text = Files.createTempFile("ocr-page-", ".txt");
            ImageIO.write(page, "png", image.toFile());
            process = new ProcessBuilder(command, image.toString(), "stdout", "-l", languages,
                    "--dpi", String.valueOf(dpi))
                    .redirectOutput(text.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(pageTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("Tesseract did not finish a page within {}", pageTimeout);
                throw failed(null);
            }
            if (process.exitValue() != 0) {
                log.warn("Tesseract exited with {} on a page", process.exitValue());
                throw failed(null);
            }
            return Files.readString(text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Tesseract could not read a page", e);
            throw failed(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failed(e);
        } finally {
            stop(process);
            delete(image);
            delete(text);
        }
    }

    private static GenerationFailure failed(Exception cause) {
        return new GenerationFailure("OCR_FAILED", "A page of this PDF could not be read.", cause);
    }

    /** Runs {@code <command> --list-langs} once, so a missing install is found at startup and not on a Teacher's file. */
    private void requireInstalled() {
        String listing = listLanguages();
        Set<String> installed = listing.lines().map(String::strip).collect(Collectors.toSet());
        List<String> missing = Arrays.stream(languages.split("\\+")).map(String::strip)
                .filter(language -> !installed.contains(language)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tesseract has no language data for " + String.join(", ", missing)
                    + ". Install it, or change GENERATION_OCR_LANGUAGES (now " + languages + ").");
        }
    }

    private String listLanguages() {
        Path listing = null;
        Process process = null;
        try {
            listing = Files.createTempFile("ocr-langs-", ".txt");
            process = new ProcessBuilder(command, "--list-langs")
                    .redirectErrorStream(true)
                    .redirectOutput(listing.toFile())
                    .start();
            if (!process.waitFor(LIST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
                throw notInstalled(null);
            }
            return Files.readString(listing, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw notInstalled(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw notInstalled(e);
        } finally {
            stop(process);
            delete(listing);
        }
    }

    private IllegalStateException notInstalled(Exception cause) {
        return new IllegalStateException("Tesseract could not be run as \"" + command + "\". Install Tesseract 5 and "
                + "set GENERATION_OCR_COMMAND to its program, or set GENERATION_OCR_ADAPTER=none to switch OCR off.",
                cause);
    }

    /** Waits for a killed program to be gone: until then it holds its files open and they cannot be deleted. */
    private static void stop(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        try {
            process.destroyForcibly().waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Tries for a while: Windows keeps the files of a killed program open for a moment after it is gone. */
    private static void delete(Path file) {
        if (file == null) {
            return;
        }
        for (int attempt = 1; ; attempt++) {
            try {
                Files.deleteIfExists(file);
                return;
            } catch (IOException e) {
                if (attempt == DELETE_ATTEMPTS || !pause()) {
                    log.warn("Could not delete the temporary OCR file {}", file.getFileName(), e);
                    return;
                }
            }
        }
    }

    private static boolean pause() {
        try {
            Thread.sleep(DELETE_PAUSE.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
