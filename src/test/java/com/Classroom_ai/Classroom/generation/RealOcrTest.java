package com.Classroom_ai.Classroom.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Tesseract adapter. The tests that need the engine run only when OCR_COMMAND names it in the environment, for
 * example {@code OCR_COMMAND="C:\Program Files\Tesseract-OCR\tesseract.exe"}, with the {@code ara}, {@code fra} and
 * {@code eng} language data installed.
 */
class RealOcrTest {

    private static final String LANGUAGES = "ara+fra+eng";
    private static final Duration A_MINUTE = Duration.ofSeconds(60);

    private static TesseractOcr tesseract(String languages, Duration pageTimeout) {
        return new TesseractOcr(System.getenv("OCR_COMMAND"), languages, pageTimeout, 300);
    }

    /** A line of text drawn in black on white, 36pt at 300 dpi. */
    private static BufferedImage imageOf(String line) {
        BufferedImage image = new BufferedImage(4200, 400, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 150));
        g.drawString(line, 100, 250);
        g.dispose();
        return image;
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OCR_COMMAND", matches = ".+")
    void readsAnEnglishLine(@TempDir Path dir) throws IOException {
        Resource pdf = TestPdfs.file(dir.resolve("line.pdf"), "The mitochondria is the powerhouse of the cell");
        TesseractOcr ocr = tesseract(LANGUAGES, A_MINUTE);

        String read = ocr.read(new PdfText(ocr, 300, 80).render(pdf, 0));

        assertThat(read).containsIgnoringCase("mitochondria");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OCR_COMMAND", matches = ".+")
    void readsAnArabicLine() {
        String read = tesseract(LANGUAGES, A_MINUTE).read(imageOf(TextQualityTest.ARABIC));

        long letters = read.codePoints().filter(Character::isLetter).count();
        long arabic = read.codePoints().filter(c -> Character.isLetter(c)
                && Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC).count();
        assertThat(letters).as("letters read from: %s", read).isGreaterThan(20);
        assertThat((double) arabic / letters).as("share of Arabic letters in: %s", read).isGreaterThanOrEqualTo(0.6);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OCR_COMMAND", matches = ".+")
    void aOneMillisecondPageTimeoutFailsOcrFailed() {
        TesseractOcr hurried = tesseract(LANGUAGES, Duration.ofMillis(1));

        assertThatThrownBy(() -> hurried.read(imageOf("Too slow to be read in a millisecond")))
                .isInstanceOfSatisfying(GenerationFailure.class,
                        failure -> assertThat(failure.code()).isEqualTo("OCR_FAILED"))
                .hasMessage("A page of this PDF could not be read.");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OCR_COMMAND", matches = ".+")
    void aLanguageThatIsNotInstalledRefusesToStart() {
        assertThatThrownBy(() -> tesseract("eng+zzz", A_MINUTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zzz").hasMessageContaining("GENERATION_OCR_LANGUAGES");
    }

    @Test
    void aCommandThatIsNotInstalledRefusesToStart() {
        String missing = "no-such-ocr-command-" + UUID.randomUUID();

        assertThatThrownBy(() -> new TesseractOcr(missing, "eng", A_MINUTE, 300))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missing).hasMessageContaining("GENERATION_OCR_COMMAND");
    }

    @Test
    void settingsThatMakeNoSenseRefuseToStart() {
        assertThatThrownBy(() -> new TesseractOcr("tesseract", " ", A_MINUTE, 300))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("generation.ocr.languages");
        assertThatThrownBy(() -> new TesseractOcr("tesseract", "eng", Duration.ZERO, 300))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("generation.ocr.page-timeout");
        assertThatThrownBy(() -> new PdfText(new NoOcr(), 0, 80))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("generation.ocr.dpi");
        assertThatThrownBy(() -> new PdfText(new NoOcr(), 300, 0))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("generation.ocr.max-pages");
    }
}
