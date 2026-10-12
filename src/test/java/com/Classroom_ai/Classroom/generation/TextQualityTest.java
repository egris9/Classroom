package com.Classroom_ai.Classroom.generation;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Text pulled out of a PDF is either real text or what a font with no Unicode map leaves behind. */
class TextQualityTest {

    static final String ARABIC = "صلح الحديبية هو اتفاق عقد بين المسلمين وقريش في السنة السادسة للهجرة";

    private static final String ENGLISH = ("Photosynthesis turns light into chemical energy in the chloroplasts of "
            + "plant cells. The light reactions make ATP and NADPH; the Calvin cycle uses them to fix carbon. ").repeat(2);
    private static final String FRENCH = ("Le roman est un genre littéraire narratif : il raconte une histoire de la "
            + "manière ordonnée que choisit son auteur. C’est une œuvre de fiction, écrite en prose — où l’on suit "
            + "des personnages. ").repeat(2);
    private static final String MATHS = ("f(x) = x² + 3x − 5 ⇒ f′(x) = 2x + 3 ; ∀ ε > 0 ∃ δ > 0 : |x − a| < δ ⇒ "
            + "|f(x) − f(a)| < ε ; ∫₀¹ x dx = ½ ; ∑ᵢ aᵢ ≤ ∏ᵢ bᵢ ; ℝ ⊂ ℂ ; lim h→0 (f(a+h) − f(a)) / h ≠ ∞ ; ").repeat(3);

    @Test
    void plainEnglishFrenchAndArabicAreReadable() {
        assertThat(TextQuality.looksBroken(ENGLISH)).isFalse();
        assertThat(TextQuality.looksBroken(FRENCH)).isFalse();
        assertThat(TextQuality.looksBroken((ARABIC + "\n").repeat(5))).isFalse();
    }

    @Test
    void mathHeavyTextIsReadable() {
        assertThat(TextQuality.looksBroken(MATHS)).isFalse();
    }

    @Test
    void symbolFontBulletsDoNotMakeASlideDeckBroken() {
        String slides = " Light reactions make ATP and NADPH\n The Calvin cycle fixes carbon\n".repeat(5);

        assertThat(TextQuality.looksBroken(slides)).isFalse();
    }

    @Test
    void replacementCharactersMakeItBroken() {
        assertThat(TextQuality.looksBroken("abcd� ".repeat(60))).isTrue();
    }

    @Test
    void privateUseGlyphsMakeItBroken() {
        assertThat(TextQuality.looksBroken(" ".repeat(60))).isTrue();
    }

    @Test
    void controlCharactersInPlaceOfLettersMakeItBroken() {
        assertThat(TextQuality.looksBroken("ab\u0001c\u0003d ".repeat(60))).isTrue();
    }

    @Test
    void tabsAndLineEndsAreNotJunk() {
        assertThat(TextQuality.looksBroken("one\ttwo\r\nthree\n".repeat(40))).isFalse();
    }

    @Test
    void arabicCutIntoSingleLettersIsBroken() {
        String cut = String.join(" ", ARABIC.replace(" ", "").split(""));

        assertThat(TextQuality.looksBroken((cut + "\n").repeat(5))).isTrue();
    }

    @Test
    void textUnderTwoHundredCharactersIsNeverJudged() {
        assertThat(TextQuality.looksBroken("�".repeat(199))).isFalse();
        assertThat(TextQuality.looksBroken("� ".repeat(199))).isFalse();
        assertThat(TextQuality.looksBroken("�".repeat(200))).isTrue();
        assertThat(TextQuality.looksBroken("")).isFalse();
    }

    @Test
    void requireReadableReturnsReadableTextAndRefusesBrokenText() {
        assertThat(TextQuality.requireReadable(ENGLISH)).isEqualTo(ENGLISH);
        assertThatThrownBy(() -> TextQuality.requireReadable("�".repeat(300)))
                .isInstanceOfSatisfying(GenerationFailure.class,
                        failure -> assertThat(failure.code()).isEqualTo("UNREADABLE_TEXT"))
                .hasMessage("The text of this PDF cannot be read correctly. Its fonts hide which letters they draw.");
    }

    @Test
    void theEvalPdfsAreJudgedAsMeasured() {
        Path arabic = Path.of("eval", "PDFs", "alaqtdaa-1-slh-alhdibia-ofth-mka-dros-oabr.pdf");
        Path calculus = Path.of("eval", "PDFs", "Fonctionderive.pdf");
        Path french = Path.of("eval", "PDFs", "L1 ITL Boussaa.pdf");
        assumeTrue(Files.isRegularFile(arabic) && Files.isRegularFile(calculus) && Files.isRegularFile(french));

        assertThat(TextQuality.looksBroken(pdfBoxTextOf(arabic))).isTrue();
        assertThat(TextQuality.looksBroken(pdfBoxTextOf(calculus))).isFalse();
        assertThat(TextQuality.looksBroken(pdfBoxTextOf(french))).isFalse();
    }

    private static String pdfBoxTextOf(Path pdf) {
        return PdfReading.join(new PdfText().pages(new FileSystemResource(pdf)));
    }
}
