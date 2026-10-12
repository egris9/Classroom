package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.material.Materials;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The text of a CourseFile, page by page. The PDF is read the first time it is asked for and its pages are stored,
 * so every later call answers from the database without opening the PDF. A reading that fails stores nothing and is
 * tried again on the next call.
 */
@Component
public class FileText {

    private final FilePageTextRepository pageTexts;
    private final CourseFileRepository files;
    private final Materials materials;
    private final PdfText pdfText;
    private final ConcurrentHashMap<Long, Object> readings = new ConcurrentHashMap<>();

    public FileText(FilePageTextRepository pageTexts, CourseFileRepository files, Materials materials,
                    PdfText pdfText) {
        this.pageTexts = pageTexts;
        this.files = files;
        this.materials = materials;
        this.pdfText = pdfText;
    }

    /** One string per page of the file's PDF, in order, {@code ""} for a page with no text. */
    public List<String> pagesOf(CourseFile file) {
        Long fileId = file.getId();
        List<String> stored = stored(fileId);
        if (!stored.isEmpty()) {
            return stored;
        }
        // One reading per file at a time: whoever waited here finds the pages stored.
        synchronized (readings.computeIfAbsent(fileId, id -> new Object())) {
            stored = stored(fileId);
            if (!stored.isEmpty()) {
                return stored;
            }
            PdfReading reading = pdfText.read(materials.open(file));
            store(fileId, reading);
            return reading.pages();
        }
    }

    /** The pages that have text, joined with a blank line. */
    public String textOf(CourseFile file) {
        return PdfReading.join(pagesOf(file));
    }

    /** How many pages the file's PDF has. A file whose text was never read is counted from the PDF, without reading it. */
    public int pageCount(CourseFile file) {
        long stored = pageTexts.countByFileId(file.getId());
        return stored > 0 ? (int) stored : pdfText.pageCount(materials.open(file));
    }

    private List<String> stored(Long fileId) {
        return pageTexts.findByFileIdOrderByPageNumber(fileId).stream().map(FilePageText::getText).toList();
    }

    /** A file that was deleted while its PDF was being read keeps no pages: nothing would ever delete them. */
    private void store(Long fileId, PdfReading reading) {
        if (!files.existsById(fileId)) {
            return;
        }
        List<FilePageText> rows = new ArrayList<>();
        for (int i = 0; i < reading.pages().size(); i++) {
            FilePageText row = new FilePageText();
            row.setFileId(fileId);
            row.setPageNumber(i + 1);
            row.setText(reading.pages().get(i));
            row.setByOcr(reading.byOcr());
            rows.add(row);
        }
        pageTexts.saveAll(rows);
    }
}
