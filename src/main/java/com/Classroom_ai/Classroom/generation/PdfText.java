package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Pulls the text out of a PDF. A scan without a text layer has none, and fails with {@code NO_TEXT}. */
@Component
public class PdfText {

    /** The text of a stored PDF. */
    public String extract(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getFile())) {
            return textOf(document);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /** The text of a PDF that was sent and is not stored. */
    public String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return textOf(document);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    private static String textOf(PDDocument document) throws IOException {
        String text = new PDFTextStripper().getText(document).strip();
        if (text.isEmpty()) {
            throw new GenerationFailure("NO_TEXT", "This PDF has no text to work from. It may be a scan.");
        }
        return text;
    }

    private static GenerationFailure unreadable(IOException cause) {
        return new GenerationFailure("PDF_UNREADABLE", "This PDF could not be read.", cause);
    }
}
