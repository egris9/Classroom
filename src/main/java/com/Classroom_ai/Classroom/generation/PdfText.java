package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Pulls the text out of a stored PDF. A scan without a text layer has none, and fails with {@code NO_TEXT}. */
@Component
public class PdfText {

    public String extract(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            String text = new PDFTextStripper().getText(document).strip();
            if (text.isEmpty()) {
                throw new GenerationFailure("NO_TEXT", "This PDF has no text to work from. It may be a scan.");
            }
            return text;
        } catch (IOException e) {
            throw new GenerationFailure("PDF_UNREADABLE", "This PDF could not be read.", e);
        }
    }
}
