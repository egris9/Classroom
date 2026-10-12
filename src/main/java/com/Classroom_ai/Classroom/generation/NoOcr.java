package com.Classroom_ai.Classroom.generation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/** OCR switched off: a PDF is read from its own text only, and no OCR engine needs to be installed. */
@Component
@ConditionalOnProperty(name = "generation.ocr.adapter", havingValue = "none", matchIfMissing = true)
public class NoOcr implements Ocr {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String read(BufferedImage page) {
        throw new GenerationFailure("OCR_FAILED", "A page of this PDF could not be read.");
    }
}
