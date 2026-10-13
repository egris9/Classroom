package com.Classroom_ai.Classroom.generation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvalRunnerTest {

    @TempDir Path dir;

    private EvalRunner runner() {
        return new EvalRunner(new FakeTextGeneration(), new PdfText(), 5);
    }

    private Path pdfs() throws IOException {
        return Files.createDirectories(dir.resolve("pdfs"));
    }

    private Path out() {
        return dir.resolve("out");
    }

    private static void writePdf(Path file, String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
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
            doc.save(file.toFile());
        }
    }

    @Test
    void each_pdf_gets_a_summary_file_an_exercises_file_and_a_row_with_timings() throws IOException {
        writePdf(pdfs().resolve("biology.pdf"), "Photosynthesis turns light into chemical energy.");

        List<EvalResult> results = runner().run(pdfs(), out());

        assertThat(results).hasSize(1);
        EvalResult result = results.get(0);
        assertThat(result.pdf()).isEqualTo("biology.pdf");
        assertThat(result.exercisesStatus()).isEqualTo("DONE");
        assertThat(result.textChars()).isGreaterThan(0);
        assertThat(result.summaries()).extracting(EvalResult.LevelResult::level).containsExactly(SummaryLevel.values());
        assertThat(result.summaries()).allSatisfy(summary -> {
            assertThat(summary.status()).isEqualTo("DONE");
            assertThat(summary.millis()).isGreaterThanOrEqualTo(0);
        });
        for (String level : List.of("short", "medium", "long", "extra_long")) {
            assertThat(Files.readString(out().resolve("biology.summary." + level + ".txt")))
                    .startsWith("Demo summary (no model connected): ");
        }
        assertThat(Files.readString(out().resolve("biology.exercises.json"))).contains("\"question\"").contains("\"answer\"");
    }

    @Test
    void each_pdf_also_gets_a_hard_set_of_fifteen_exercises_of_all_three_types_with_its_time() throws IOException {
        writePdf(pdfs().resolve("biology.pdf"), "Photosynthesis turns light into chemical energy.");

        EvalResult result = runner().run(pdfs(), out()).get(0);

        assertThat(result.hardMixedStatus()).isEqualTo("DONE");
        assertThat(result.hardMixedMillis()).isGreaterThanOrEqualTo(0);
        String hard = Files.readString(out().resolve("biology.exercises.hard.json"));
        assertThat(hard).contains("\"MULTIPLE_CHOICE\"").contains("\"TRUE_FALSE\"").contains("\"OPEN\"");
        assertThat(hard.split("\"question\"", -1)).hasSize(16);
        assertThat(Files.readString(out().resolve("results.md")))
                .contains("## Hard mixed exercise set (15)").contains("| biology.pdf | DONE | ");
    }

    @Test
    void each_level_is_recorded_with_the_word_count_of_its_summary() throws IOException {
        writePdf(pdfs().resolve("biology.pdf"), "Light becomes sugar. ".repeat(20).strip());

        EvalResult result = runner().run(pdfs(), out()).get(0);

        for (EvalResult.LevelResult summary : result.summaries()) {
            String file = "biology.summary." + summary.level().name().toLowerCase() + ".txt";
            assertThat(summary.words()).as(file)
                    .isEqualTo(Files.readString(out().resolve(file)).strip().split("\\s+").length);
        }
        assertThat(result.summaries()).extracting(EvalResult.LevelResult::words).isSorted().doesNotHaveDuplicates();
        String table = Files.readString(out().resolve("results.md"));
        assertThat(table).contains("| Level | Status | Words | ms |");
        for (EvalResult.LevelResult summary : result.summaries()) {
            assertThat(table).contains("| biology.pdf | " + result.textChars() + " | "
                    + summary.level().name().toLowerCase() + " | DONE | " + summary.words() + " | ");
        }
    }

    @Test
    void results_md_has_a_row_per_pdf_in_name_order() throws IOException {
        writePdf(pdfs().resolve("b.pdf"), "Second lesson text.");
        writePdf(pdfs().resolve("a.pdf"), "First lesson text.");

        runner().run(pdfs(), out());

        String table = Files.readString(out().resolve("results.md"));
        assertThat(table).contains("| a.pdf |").contains("| b.pdf |");
        assertThat(table.indexOf("| a.pdf |")).isLessThan(table.indexOf("| b.pdf |"));
    }

    @Test
    void a_scan_with_no_text_is_recorded_as_NO_TEXT_and_the_run_goes_on() throws IOException {
        writePdf(pdfs().resolve("a-scan.pdf"), null);
        writePdf(pdfs().resolve("b-text.pdf"), "A lesson with text.");

        List<EvalResult> results = runner().run(pdfs(), out());

        assertThat(results.get(0).summaries()).hasSize(4)
                .allSatisfy(summary -> assertThat(summary.status()).isEqualTo("NO_TEXT"));
        assertThat(results.get(0).exercisesStatus()).isEqualTo("NO_TEXT");
        assertThat(results.get(0).hardMixedStatus()).isEqualTo("NO_TEXT");
        assertThat(Files.readString(out().resolve("a-scan.exercises.hard.json"))).startsWith("FAILED NO_TEXT");
        assertThat(Files.readString(out().resolve("a-scan.summary.extra_long.txt"))).startsWith("FAILED NO_TEXT");
        assertThat(results.get(1).summaries()).hasSize(4)
                .allSatisfy(summary -> assertThat(summary.status()).isEqualTo("DONE"));
    }

    @Test
    void a_file_that_is_not_a_pdf_is_recorded_as_PDF_UNREADABLE() throws IOException {
        Files.writeString(pdfs().resolve("broken.pdf"), "this is not a pdf", StandardCharsets.UTF_8);

        List<EvalResult> results = runner().run(pdfs(), out());

        assertThat(results.get(0).summaries()).hasSize(4)
                .allSatisfy(summary -> assertThat(summary.status()).isEqualTo("PDF_UNREADABLE"));
    }

    @Test
    void files_without_the_pdf_extension_are_ignored() throws IOException {
        writePdf(pdfs().resolve("lesson.pdf"), "A lesson.");
        Files.writeString(pdfs().resolve("notes.txt"), "ignore me");

        assertThat(runner().run(pdfs(), out())).extracting(EvalResult::pdf).containsExactly("lesson.pdf");
    }

    @Test
    void a_folder_with_no_pdfs_is_refused_with_a_message_naming_the_folder() throws IOException {
        Path empty = pdfs();

        assertThatThrownBy(() -> runner().run(empty, out()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No PDFs").hasMessageContaining(empty.toString());
    }
}
