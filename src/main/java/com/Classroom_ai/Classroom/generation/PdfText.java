package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Pulls the text out of a PDF, page by page. PDFBox reads the PDF's own text. When there is none (a scan) or it is
 * garbled, and OCR is switched on, a stored PDF is read from the images of its pages instead; with OCR off those
 * fail with {@code NO_TEXT} and {@code UNREADABLE_TEXT}.
 */
@Component
public class PdfText {

    /** The longer side of a page image, in pixels: an A3 page at 300 dpi fits, a poster is rendered smaller. */
    static final int MAX_IMAGE_SIDE = 6000;

    private static final Logger log = LoggerFactory.getLogger(PdfText.class);

    private final Ocr ocr;
    private final int dpi;
    private final int maxOcrPages;
    private final Duration ocrTimeout;

    /** PDFBox only: OCR is off. */
    public PdfText() {
        this(new NoOcr(), 300, 80);
    }

    /** With ten minutes for the OCR of a whole file, the default. */
    public PdfText(Ocr ocr, int dpi, int maxOcrPages) {
        this(ocr, dpi, maxOcrPages, Duration.ofMinutes(10));
    }

    @Autowired
    public PdfText(Ocr ocr, @Value("${generation.ocr.dpi:300}") int dpi,
                   @Value("${generation.ocr.max-pages:80}") int maxOcrPages,
                   @Value("${generation.ocr.timeout:10m}") Duration ocrTimeout) {
        if (dpi < 1) {
            throw new IllegalStateException("generation.ocr.dpi must be at least 1");
        }
        if (maxOcrPages < 1) {
            throw new IllegalStateException("generation.ocr.max-pages must be at least 1");
        }
        if (ocrTimeout == null || ocrTimeout.isZero() || ocrTimeout.isNegative()) {
            throw new IllegalStateException("generation.ocr.timeout must be a positive duration such as 10m");
        }
        this.ocr = ocr;
        this.dpi = dpi;
        this.maxOcrPages = maxOcrPages;
        this.ocrTimeout = ocrTimeout;
    }

    /** What PDFBox finds on each page of a stored PDF: one stripped string per page, {@code ""} for a page with no text. */
    public List<String> pages(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return pagesOf(document);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /**
     * The text of a stored PDF, page by page. The choice is made for the whole document: PDFBox's text when it has
     * some and it is readable, otherwise every page is read by OCR. A PDF with more pages than
     * {@code generation.ocr.max-pages} is not given to OCR and fails with {@code TOO_LONG}, and so does one whose
     * pages are not all read within {@code generation.ocr.timeout}: nothing else is generated while a file is read.
     */
    public PdfReading read(Resource pdf) {
        List<String> pages = pages(pdf);
        boolean blank = isBlank(pages);
        if (!blank && !TextQuality.looksBroken(PdfReading.join(pages))) {
            return new PdfReading(pages, false);
        }
        if (!ocr.available()) {
            throw blank ? noText() : TextQuality.unreadable();
        }
        if (pages.size() > maxOcrPages) {
            throw new GenerationFailure("TOO_LONG", "This PDF has more pages than can be read from their images.");
        }
        List<String> read = ocrPages(pdf, pages.size());
        if (isBlank(read)) {
            throw noText();
        }
        TextQuality.requireReadable(PdfReading.join(read));
        return new PdfReading(read, true);
    }

    /** How many pages a stored PDF has. Its text is not read. */
    public int pageCount(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /** The image of one page of a stored PDF (the first is 0), in grayscale at {@code generation.ocr.dpi}. */
    public BufferedImage render(Resource pdf, int pageIndex) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return render(document, new PDFRenderer(document), pageIndex);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /**
     * The text of a PDF that was sent and is not stored: its pages joined with a blank line. PDFBox only, never
     * OCR: this is the AI tools page, where a visitor must not be able to spend minutes of CPU.
     */
    public String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> pages = pagesOf(document);
            if (isBlank(pages)) {
                throw noText();
            }
            return PdfReading.join(pages);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    private List<String> ocrPages(Resource pdf, int count) {
        long start = System.nanoTime();
        List<String> read = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < count; page++) {
                if (System.nanoTime() - start > ocrTimeout.toNanos()) {
                    log.warn("Gave up reading a PDF by OCR after {} of {} pages and {}", page, count, ocrTimeout);
                    throw new GenerationFailure("TOO_LONG",
                            "This PDF takes too long to read from the images of its pages.");
                }
                read.add(ocr.read(render(document, renderer, page)).strip());
            }
        } catch (IOException e) {
            throw unreadable(e);
        }
        log.info("Read {} pages of a PDF by OCR in {} ms", count, (System.nanoTime() - start) / 1_000_000);
        return read;
    }

    private BufferedImage render(PDDocument document, PDFRenderer renderer, int pageIndex) throws IOException {
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new IllegalArgumentException("This PDF has no page " + (pageIndex + 1));
        }
        PDRectangle box = document.getPage(pageIndex).getCropBox();
        float longerSide = Math.max(box.getWidth(), box.getHeight());
        float scale = Math.min(dpi / 72f, MAX_IMAGE_SIDE / longerSide);
        return renderer.renderImage(pageIndex, scale, ImageType.GRAY);
    }

    private static List<String> pagesOf(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            pages.add(stripper.getText(document).strip());
        }
        return pages;
    }

    private static boolean isBlank(List<String> pages) {
        return pages.stream().allMatch(String::isBlank);
    }

    private static GenerationFailure noText() {
        return new GenerationFailure("NO_TEXT", "This PDF has no text to work from. It may be a scan.");
    }

    private static GenerationFailure unreadable(IOException cause) {
        return new GenerationFailure("PDF_UNREADABLE", "This PDF could not be read.", cause);
    }
}
