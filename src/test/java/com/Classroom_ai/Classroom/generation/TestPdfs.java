package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small PDFs for tests. */
public final class TestPdfs {

    private TestPdfs() {
    }

    /**
     * One PDF page per argument, each with its text drawn on it. A null argument is a blank page (what a scan without
     * OCR looks like), and {@code bytes(null)} is one blank page.
     */
    public static byte[] bytes(String... pages) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : pages == null ? new String[]{null} : pages) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (text != null) {
                    try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 700);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** {@link #bytes} written to {@code file}. */
    public static Resource file(Path file, String... pages) throws IOException {
        Files.write(file, bytes(pages));
        return new FileSystemResource(file);
    }
}
