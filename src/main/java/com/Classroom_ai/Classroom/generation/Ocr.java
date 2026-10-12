package com.Classroom_ai.Classroom.generation;

import java.awt.image.BufferedImage;

/**
 * Reads the text of a page from its image, for the PDFs whose own text is missing (a scan) or garbled. Which engine
 * does it is chosen by {@code generation.ocr.adapter}; {@code none}, the default, switches it off.
 */
public interface Ocr {

    /** False when OCR is switched off: {@link #read} is then never called. */
    boolean available();

    /** The text on the page. Fails with {@code OCR_FAILED} when the page cannot be read. */
    String read(BufferedImage page);
}
