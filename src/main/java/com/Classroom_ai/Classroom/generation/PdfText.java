package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Pulls the text out of a PDF, page by page. A scan without a text layer has none, and fails with {@code NO_TEXT}. */
@Component
public class PdfText {

    /** What PDFBox finds on each page of a stored PDF: one stripped string per page, {@code ""} for a page with no text. */
    public List<String> pages(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return pagesOf(document);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /** The text of a stored PDF, page by page. */
    public PdfReading read(Resource pdf) {
        return new PdfReading(requireText(pages(pdf)), false);
    }

    /** How many pages a stored PDF has. Its text is not read. */
    public int pageCount(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /** The text of a PDF that was sent and is not stored: its pages joined with a blank line. */
    public String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return PdfReading.join(requireText(pagesOf(document)));
        } catch (IOException e) {
            throw unreadable(e);
        }
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

    private static List<String> requireText(List<String> pages) {
        if (pages.stream().allMatch(String::isBlank)) {
            throw new GenerationFailure("NO_TEXT", "This PDF has no text to work from. It may be a scan.");
        }
        return pages;
    }

    private static GenerationFailure unreadable(IOException cause) {
        return new GenerationFailure("PDF_UNREADABLE", "This PDF could not be read.", cause);
    }
}
