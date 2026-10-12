package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.generation.FilePageText;
import com.Classroom_ai.Classroom.generation.FilePageTextRepository;
import com.Classroom_ai.Classroom.generation.FileText;
import com.Classroom_ai.Classroom.generation.GenerationFailure;
import com.Classroom_ai.Classroom.generation.Ocr;
import com.Classroom_ai.Classroom.generation.PdfText;
import com.Classroom_ai.Classroom.material.Materials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The text of a CourseFile, page by page: read from the PDF once, then kept in the database. */
@SpringBootTest
@AutoConfigureMockMvc
class FileTextTest extends GenerationTestSupport {

    @Autowired FileText fileText;
    @Autowired FilePageTextRepository pageTexts;
    @Autowired CourseFileRepository files;
    @Autowired Materials materials;
    @Autowired Ocr ocr;

    /** PDFBox, counting how often a PDF is opened for its text, and slow enough for two callers to overlap. */
    static class CountingPdfText extends PdfText {
        final AtomicInteger readings = new AtomicInteger();
        volatile Runnable whileReading = () -> {
        };

        @Override
        public List<String> pages(Resource pdf) {
            readings.incrementAndGet();
            List<String> pages = super.pages(pdf);
            whileReading.run();
            return pages;
        }
    }

    private CourseFile stored(Course course, String... pages) throws Exception {
        return files.findById(Long.valueOf(course.upload(pdfBytes(pages)))).orElseThrow();
    }

    @Test
    void pagesComeOnePerPdfPageInOrder() throws Exception {
        CourseFile file = stored(course(), "First page", null, "Third page");

        assertEquals(List.of("First page", "", "Third page"), fileText.pagesOf(file));
        assertEquals(3, fileText.pageCount(file));
    }

    @Test
    void textOfJoinsThePagesWithABlankLine() throws Exception {
        CourseFile file = stored(course(), "First page", null, "Third page");

        assertEquals("First page\n\nThird page", fileText.textOf(file));
    }

    @Test
    void aSecondCallDoesNotOpenThePdfAgain() throws Exception {
        CourseFile file = stored(course(), "Kept", "Also kept");
        List<String> first = fileText.pagesOf(file);

        materials.delete(file);

        assertEquals(first, fileText.pagesOf(file));
        assertEquals("Kept\n\nAlso kept", fileText.textOf(file));
        assertEquals(2, fileText.pageCount(file));
    }

    @Test
    void thePageCountOfAFileNotReadYetComesFromThePdfAndStoresNothing() throws Exception {
        CourseFile file = stored(course(), "One", "Two", null);

        assertEquals(3, fileText.pageCount(file));
        assertEquals(0, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void aScanFailsWithNoTextAndStoresNothing() throws Exception {
        CourseFile file = stored(course(), null, null);

        GenerationFailure failure = assertThrows(GenerationFailure.class, () -> fileText.pagesOf(file));

        assertEquals("NO_TEXT", failure.code());
        assertEquals(0, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void garbledTextFailsUnreadableAndStoresNothing() throws Exception {
        CourseFile file = stored(course(), "What PDFBox finds is replaced below");
        PdfText garbled = new PdfText() {
            @Override
            public List<String> pages(Resource pdf) {
                return List.of("�\u0001x ".repeat(100), "�\u0001x ".repeat(100));
            }
        };

        GenerationFailure failure = assertThrows(GenerationFailure.class,
                () -> new FileText(pageTexts, files, materials, garbled).pagesOf(file));

        assertEquals("UNREADABLE_TEXT", failure.code());
        assertEquals(0, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void ocrIsOffUnlessItIsConfigured() {
        assertFalse(ocr.available());
    }

    @Test
    void pagesReadByOcrAreStoredSoASecondCallDoesNoOcr() throws Exception {
        CourseFile file = stored(course(), null, null);
        AtomicInteger calls = new AtomicInteger();
        Ocr counting = new Ocr() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public String read(BufferedImage page) {
                return "Read from the image of page " + calls.incrementAndGet();
            }
        };
        FileText reading = new FileText(pageTexts, files, materials, new PdfText(counting, 72, 80));

        List<String> first = reading.pagesOf(file);

        assertEquals(List.of("Read from the image of page 1", "Read from the image of page 2"), first);
        assertEquals(first, reading.pagesOf(file));
        assertEquals(first, fileText.pagesOf(file));
        assertEquals(2, calls.get());
        assertTrue(pageTexts.findByFileIdOrderByPageNumber(file.getId()).stream().allMatch(FilePageText::isByOcr));
    }

    @Test
    void twoThreadsAskingAtOnceReadThePdfOnce() throws Exception {
        CourseFile file = stored(course(), "Shared page");
        CountingPdfText counting = new CountingPdfText();
        counting.whileReading = () -> sleep(300);
        FileText shared = new FileText(pageTexts, files, materials, counting);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<List<String>>> asked = List.of(
                    pool.submit(() -> {
                        start.await();
                        return shared.pagesOf(file);
                    }),
                    pool.submit(() -> {
                        start.await();
                        return shared.pagesOf(file);
                    }));
            start.countDown();

            for (Future<List<String>> answer : asked) {
                assertEquals(List.of("Shared page"), answer.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, counting.readings.get());
        assertEquals(1, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void aFileDeletedWhileItIsReadStoresNothing() throws Exception {
        Course c = course();
        CourseFile file = stored(c, "Gone before the reading ends");
        CountingPdfText counting = new CountingPdfText();
        counting.whileReading = () -> {
            try {
                mvc.perform(delete("/api/files/" + file.getId()).header("Authorization", c.teacher))
                        .andExpect(status().is2xxSuccessful());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        new FileText(pageTexts, files, materials, counting).pagesOf(file);

        assertEquals(0, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void deletingTheFileDeletesItsPageTexts() throws Exception {
        Course c = course();
        CourseFile file = stored(c, "Soon gone", "Its second page");
        fileText.pagesOf(file);
        assertEquals(2, pageTexts.countByFileId(file.getId()));

        mvc.perform(delete("/api/files/" + file.getId()).header("Authorization", c.teacher))
                .andExpect(status().is2xxSuccessful());

        assertEquals(0, pageTexts.countByFileId(file.getId()));
    }

    @Test
    void deletingTheCourseDeletesItsPageTexts() throws Exception {
        Course c = course();
        CourseFile first = stored(c, "First file");
        CourseFile second = stored(c, "Second file");
        fileText.pagesOf(first);
        fileText.pagesOf(second);

        mvc.perform(delete("/api/courses/" + c.id).header("Authorization", c.teacher))
                .andExpect(status().is2xxSuccessful());

        assertEquals(0, pageTexts.countByFileId(first.getId()));
        assertEquals(0, pageTexts.countByFileId(second.getId()));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
