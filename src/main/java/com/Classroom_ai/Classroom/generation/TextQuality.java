package com.Classroom_ai.Classroom.generation;

import java.lang.Character.UnicodeScript;

/**
 * Tells real text from what a PDF gives when its fonts do not say which letters they draw: characters that stand for
 * nothing, or Arabic words cut into single letters. A model given such text invents its answer.
 *
 * <p>The thresholds come from the three PDFs in {@code eval/PDFs}: the garbled Arabic one measured 33% junk
 * characters and 42% single-letter Arabic words, the two readable ones 0% of both. Each threshold is at least a
 * factor of two away from every measured value.
 */
public final class TextQuality {

    /** Too little text to judge by proportions. */
    private static final int MIN_JUDGED_CHARS = 200;
    /** A deck with symbol-font bullets is a few percent private-use characters and must stay readable. */
    private static final double MAX_JUNK_SHARE = 0.10;
    private static final double MAX_SINGLE_LETTER_ARABIC_WORDS = 0.20;

    private TextQuality() {
    }

    /** Whether the text is garbled. Text with fewer than 200 non-space characters is never called garbled. */
    public static boolean looksBroken(String text) {
        long nonSpace = 0;
        long junk = 0;
        long letters = 0;
        long arabicLetters = 0;
        for (int i = 0; i < text.length(); ) {
            int c = text.codePointAt(i);
            i += Character.charCount(c);
            if (isJunk(c)) {
                junk++;
            } else if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                continue;
            } else if (Character.isLetter(c)) {
                letters++;
                if (isArabic(c)) {
                    arabicLetters++;
                }
            }
            nonSpace++;
        }
        if (nonSpace < MIN_JUDGED_CHARS) {
            return false;
        }
        if (junk > MAX_JUNK_SHARE * nonSpace) {
            return true;
        }
        return arabicLetters * 2 >= letters && arabicWordsAreCutUp(text);
    }

    /** The text, or {@code UNREADABLE_TEXT} when it is garbled. */
    public static String requireReadable(String text) {
        if (looksBroken(text)) {
            throw unreadable();
        }
        return text;
    }

    static GenerationFailure unreadable() {
        return new GenerationFailure("UNREADABLE_TEXT",
                "The text of this PDF cannot be read correctly. Its fonts hide which letters they draw.");
    }

    /** The replacement character, a private-use character, or a control character other than tab and line ends. */
    private static boolean isJunk(int c) {
        if (c == '\t' || c == '\n' || c == '\r') {
            return false;
        }
        return c == 0xFFFD || Character.getType(c) == Character.PRIVATE_USE || Character.isISOControl(c);
    }

    private static boolean isArabic(int c) {
        return UnicodeScript.of(c) == UnicodeScript.ARABIC;
    }

    /** Real Arabic joins its letters into words; only a few words ("و") are a single letter. */
    private static boolean arabicWordsAreCutUp(String text) {
        long arabicWords = 0;
        long singleLetter = 0;
        for (String word : text.split("\\s+")) {
            long letters = word.codePoints().filter(Character::isLetter).count();
            if (word.codePoints().anyMatch(c -> Character.isLetter(c) && isArabic(c))) {
                arabicWords++;
                if (letters == 1) {
                    singleLetter++;
                }
            }
        }
        return arabicWords > 0 && singleLetter > MAX_SINGLE_LETTER_ARABIC_WORDS * arabicWords;
    }
}
