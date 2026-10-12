package com.Classroom_ai.Classroom.generation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What was read from a PDF: one stripped string per page, in order, {@code ""} for a page with no text.
 * {@code byOcr} says the pages were read from their images, not from the PDF's own text.
 */
public record PdfReading(List<String> pages, boolean byOcr) {

    public PdfReading {
        pages = List.copyOf(pages);
    }

    /** The pages that have text, joined with a blank line. */
    public String text() {
        return join(pages);
    }

    static String join(List<String> pages) {
        return pages.stream().filter(page -> !page.isBlank()).collect(Collectors.joining("\n\n"));
    }
}
