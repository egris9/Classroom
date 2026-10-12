package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** When PDFBox finds no text, or garbled text, the pages are read from their images, if OCR is on. */
class PdfTextOcrTest {

    private static final String GARBLED = "�\u0001x ".repeat(100);
    private static final String READ = "Photosynthesis turns light into chemical energy, page ";

    @TempDir Path dir;

    /** An OCR engine that answers from a function of the call number (from 1) and keeps the images it was given. */
    static class StubOcr implements Ocr {
        final List<BufferedImage> seen = new ArrayList<>();
        private final IntFunction<String> answer;

        StubOcr(IntFunction<String> answer) {
            this.answer = answer;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public String read(BufferedImage page) {
            seen.add(page);
            return answer.apply(seen.size());
        }
    }

    /** PDFBox stood in for: every page of the PDF comes out as {@code text}. */
    private static PdfText findingOnEveryPage(String text, Ocr ocr) {
        return new PdfText(ocr, 72, 80) {
            @Override
            public List<String> pages(Resource pdf) {
                return super.pages(pdf).stream().map(page -> text).toList();
            }
        };
    }

    private Resource pdf(String... pages) throws IOException {
        return TestPdfs.file(dir.resolve("lesson.pdf"), pages);
    }

    private static void assertFails(PdfText pdfText, Resource pdf, String code) {
        assertThatThrownBy(() -> pdfText.read(pdf)).isInstanceOfSatisfying(GenerationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo(code));
    }

    @Test
    void aScanIsReadByOcr() throws IOException {
        StubOcr ocr = new StubOcr(call -> "  " + READ + call + " \n");

        PdfReading reading = new PdfText(ocr, 72, 80).read(pdf(null, null));

        assertThat(reading.pages()).containsExactly(READ + 1, READ + 2);
        assertThat(reading.byOcr()).isTrue();
        assertThat(ocr.seen).hasSize(2);
    }

    @Test
    void garbledTextIsReplacedByWhatOcrReads() throws IOException {
        StubOcr ocr = new StubOcr(call -> READ + call);

        PdfReading reading = findingOnEveryPage(GARBLED, ocr).read(pdf("one", "two", "three"));

        assertThat(reading.pages()).containsExactly(READ + 1, READ + 2, READ + 3);
        assertThat(reading.byOcr()).isTrue();
    }

    @Test
    void readableTextNeverReachesOcr() throws IOException {
        StubOcr ocr = new StubOcr(call -> READ + call);

        PdfReading reading = new PdfText(ocr, 72, 80).read(pdf("A lesson with text.", null));

        assertThat(reading.pages()).containsExactly("A lesson with text.", "");
        assertThat(reading.byOcr()).isFalse();
        assertThat(ocr.seen).isEmpty();
    }

    @Test
    void ocrThatReadsJunkStillFailsUnreadable() throws IOException {
        StubOcr ocr = new StubOcr(call -> GARBLED);

        assertFails(new PdfText(ocr, 72, 80), pdf(null, null), "UNREADABLE_TEXT");
    }

    @Test
    void ocrThatReadsNothingFailsNoText() throws IOException {
        StubOcr ocr = new StubOcr(call -> " \n");

        assertFails(new PdfText(ocr, 72, 80), pdf(null, null), "NO_TEXT");
        assertThat(ocr.seen).hasSize(2);
    }

    @Test
    void morePagesThanTheLimitFailsTooLongAndNeverCallsOcr() throws IOException {
        StubOcr ocr = new StubOcr(call -> READ + call);
        PdfText twoPagesAtMost = new PdfText(ocr, 72, 2);

        assertThatThrownBy(() -> twoPagesAtMost.read(pdf(null, null, null)))
                .isInstanceOfSatisfying(GenerationFailure.class,
                        failure -> assertThat(failure.code()).isEqualTo("TOO_LONG"))
                .hasMessage("This PDF has more pages than can be read from their images.");
        assertThat(ocr.seen).isEmpty();
        assertThat(twoPagesAtMost.read(pdf(null, null)).pages()).hasSize(2);
    }

    @Test
    void aFileThatTakesLongerThanTheWholeFileTimeoutStopsBeforeItsNextPage() throws IOException {
        StubOcr slow = new StubOcr(call -> {
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return READ + call;
        });
        PdfText hurried = new PdfText(slow, 72, 80, Duration.ofMillis(30));

        assertThatThrownBy(() -> hurried.read(pdf(null, null, null)))
                .isInstanceOfSatisfying(GenerationFailure.class,
                        failure -> assertThat(failure.code()).isEqualTo("TOO_LONG"))
                .hasMessage("This PDF takes too long to read from the images of its pages.");
        assertThat(slow.seen).hasSize(1);
    }

    @Test
    void withOcrOffAScanIsStillNoText() throws IOException {
        assertFails(new PdfText(), pdf(null, null), "NO_TEXT");
        assertFails(new PdfText(new NoOcr(), 300, 80), pdf(null, null), "NO_TEXT");
    }

    @Test
    void withOcrOffGarbledTextIsStillUnreadable() throws IOException {
        assertFails(findingOnEveryPage(GARBLED, new NoOcr()), pdf("one", "two"), "UNREADABLE_TEXT");
    }

    @Test
    void withOcrOffAPdfWithTextIsReadAsBefore() throws IOException {
        PdfReading reading = new PdfText().read(pdf("First page", "Second page"));

        assertThat(reading.pages()).containsExactly("First page", "Second page");
        assertThat(reading.byOcr()).isFalse();
    }

    @Test
    void aPageThatFailsFailsTheWholeReading() throws IOException {
        StubOcr ocr = new StubOcr(call -> {
            if (call == 2) {
                throw new GenerationFailure("OCR_FAILED", "A page of this PDF could not be read.");
            }
            return READ + call;
        });

        assertFails(new PdfText(ocr, 72, 80), pdf(null, null, null), "OCR_FAILED");
        assertThat(ocr.seen).hasSize(2);
    }

    @Test
    void aPageIsRenderedInGrayscaleAtTheDpi() throws IOException {
        Resource letter = pdf("Some text");

        BufferedImage at72 = new PdfText(new NoOcr(), 72, 80).render(letter, 0);
        BufferedImage at144 = new PdfText(new NoOcr(), 144, 80).render(letter, 0);

        assertThat(at72.getType()).isEqualTo(BufferedImage.TYPE_BYTE_GRAY);
        assertThat(List.of(at72.getWidth(), at72.getHeight())).containsExactly(612, 792);
        assertThat(List.of(at144.getWidth(), at144.getHeight())).containsExactly(1224, 1584);
    }

    @Test
    void ocrIsGivenTheImageOfEachPageInOrder() throws IOException {
        Path file = dir.resolve("sizes.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(new PDRectangle(100, 200)));
            doc.addPage(new PDPage(new PDRectangle(300, 400)));
            doc.save(file.toFile());
        }
        StubOcr ocr = new StubOcr(call -> READ + call);

        new PdfText(ocr, 72, 80).read(new FileSystemResource(file));

        assertThat(ocr.seen).extracting(BufferedImage::getWidth).containsExactly(100, 300);
    }

    @Test
    void aPosterSizedPageIsRenderedSmallerSoItCannotExhaustMemory() throws IOException {
        Path file = dir.resolve("poster.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(new PDRectangle(14_400, 7_200)));
            doc.save(file.toFile());
        }

        BufferedImage image = new PdfText(new NoOcr(), 300, 80).render(new FileSystemResource(file), 0);

        assertThat(image.getWidth()).isEqualTo(PdfText.MAX_IMAGE_SIDE);
        assertThat(image.getHeight()).isEqualTo(PdfText.MAX_IMAGE_SIDE / 2);
    }
}
