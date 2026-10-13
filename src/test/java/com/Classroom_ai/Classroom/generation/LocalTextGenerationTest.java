package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalTextGenerationTest {

    private static final String MODEL = "qwen-test";

    private StubModelServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new StubModelServer();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private LocalTextGeneration adapter(String baseUrl, Duration timeout, int chunkChars) {
        return new LocalTextGeneration(baseUrl, MODEL, timeout, chunkChars, new ObjectMapper());
    }

    private LocalTextGeneration adapter() {
        return adapter(server.baseUrl(), Duration.ofSeconds(5), 10_000);
    }

    @Test
    void summarize_sends_the_text_to_chat_completions_and_returns_the_reply() {
        server.replyWith(user -> "A short summary.");

        String summary = adapter().summarize("Photosynthesis turns light into sugar.", SummaryLevel.MEDIUM);

        assertThat(summary).isEqualTo("A short summary.");
        JsonNode request = server.requests().get(0);
        assertThat(request.path("model").asText()).isEqualTo(MODEL);
        assertThat(server.userMessage(0)).contains("Photosynthesis turns light into sugar.");
    }

    @Test
    void summaryRequestAsksForOwnWordsAndNoEllipsis() {
        adapter().summarize("Photosynthesis turns light into sugar.", SummaryLevel.MEDIUM);

        String system = server.requests().get(0).path("messages").path(0).path("content").asText();
        assertThat(system).contains("in your own words").contains("Do not copy sentences").contains("no ellipses");
    }

    // --- summary levels ---

    private static final String MEDIUM_PROMPT = "You summarise course material for students. "
            + "Write the summary in your own words. Do not copy sentences from the text, and use no ellipses. "
            + "Start with an overview of one or two sentences, then give 4 to 8 bullet points with the key points, "
            + "and put the key terms in bold. Answer in the language of the text.";

    /** An adapter with its own time limit for the long levels and its own token caps, in level order. */
    private LocalTextGeneration adapter(Duration timeout, Duration longTimeout, int chunkChars, List<Integer> caps) {
        return new LocalTextGeneration(server.baseUrl(), MODEL, timeout, longTimeout, chunkChars,
                LocalTextGeneration.DEFAULT_MAX_CHUNKS, caps, new ObjectMapper());
    }

    @Test
    void mediumPromptIsUnchanged() {
        adapter().summarize("Photosynthesis turns light into sugar.", SummaryLevel.MEDIUM);

        assertThat(system(0)).contains("4 to 8 bullet points").isEqualTo(MEDIUM_PROMPT);
    }

    @Test
    void eachLevelSendsItsOwnFormat() {
        for (SummaryLevel level : SummaryLevel.values()) {
            adapter().summarize("Photosynthesis turns light into sugar.", level);
        }

        assertThat(system(0)).contains("overview of one sentence").contains("exactly 3 bullet points");
        assertThat(system(1)).contains("4 to 8 bullet points");
        assertThat(system(2)).contains("overview of two or three sentences").contains("3 to 5 sections")
                .contains("starts with '## '").contains("if the text is short, write less");
        assertThat(system(3)).contains("overview of three or four sentences").contains("5 to 8 sections")
                .contains("'## Key terms'").contains("one-line definition").contains("if the text is short, write less");
        assertThat(List.of(system(0), system(1), system(2), system(3))).doesNotHaveDuplicates();
    }

    @Test
    void everyLevelStillAsksForOwnWordsAndNoEllipses() {
        for (SummaryLevel level : SummaryLevel.values()) {
            adapter().summarize("Photosynthesis turns light into sugar.", level);
        }

        for (int i = 0; i < SummaryLevel.values().length; i++) {
            assertThat(system(i)).as("level %s", SummaryLevel.values()[i])
                    .contains("Write the summary in your own words. Do not copy sentences from the text, and use no ellipses.")
                    .endsWith("Answer in the language of the text.");
        }
    }

    @Test
    void anArabicTextNamesItsLanguageAtEveryLevel() {
        for (SummaryLevel level : SummaryLevel.values()) {
            adapter().summarize(ARABIC, level);
        }

        for (int i = 0; i < SummaryLevel.values().length; i++) {
            assertThat(system(i)).as("level %s", SummaryLevel.values()[i]).endsWith("answer in Arabic.");
        }
    }

    @Test
    void partsOfALongDocumentUseTheDetailedPartPromptForLongAndExtraLong() {
        server.replyWith(user -> "S");

        for (SummaryLevel level : List.of(SummaryLevel.LONG, SummaryLevel.EXTRA_LONG)) {
            int before = server.requests().size();
            adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60), level);
            int last = server.requests().size() - 1;

            assertThat(last - before).as("parts of %s", level).isGreaterThan(1);
            for (int i = before; i < last; i++) {
                assertThat(system(i)).as("part %s of %s", i, level)
                        .contains("one part of a longer course document")
                        .contains("6 to 12 bullet points with the key points and the details that support them")
                        .doesNotContain("sections");
            }
            assertThat(system(last)).as("combining request of %s", level)
                    .contains("summaries of consecutive parts")
                    .contains(level == SummaryLevel.LONG ? "3 to 5 sections" : "5 to 8 sections");
        }
    }

    @Test
    void partsOfALongDocumentKeepTheOldPartPromptForShortAndMedium() {
        server.replyWith(user -> "S");

        for (SummaryLevel level : List.of(SummaryLevel.SHORT, SummaryLevel.MEDIUM)) {
            int before = server.requests().size();
            adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60), level);
            int last = server.requests().size() - 1;

            for (int i = before; i < last; i++) {
                assertThat(system(i)).as("part %s of %s", i, level)
                        .contains("one part of a longer course document").contains("4 to 8 bullet points");
            }
            assertThat(system(last)).as("combining request of %s", level)
                    .contains(level == SummaryLevel.SHORT ? "exactly 3 bullet points" : "4 to 8 bullet points");
        }
    }

    private int maxTokens(int request) {
        return server.requests().get(request).path("max_tokens").asInt(-1);
    }

    @Test
    void aSummaryCallCarriesTheTokenCapOfItsLevel() {
        LocalTextGeneration capped = adapter(Duration.ofSeconds(5), Duration.ofSeconds(5), 10_000, List.of(11, 22, 33, 44));

        for (SummaryLevel level : SummaryLevel.values()) {
            capped.summarize("Photosynthesis turns light into sugar.", level);
        }

        assertThat(List.of(maxTokens(0), maxTokens(1), maxTokens(2), maxTokens(3))).containsExactly(11, 22, 33, 44);
    }

    @Test
    void aPartCallCarriesTheMediumOrTheLongCapAndTheCombiningCallTheLevelsOwn() {
        server.replyWith(user -> "S");
        LocalTextGeneration capped = adapter(Duration.ofSeconds(5), Duration.ofSeconds(5), 150, List.of(11, 22, 33, 44));
        int[] partCap = {22, 22, 33, 33};
        int[] ownCap = {11, 22, 33, 44};

        for (SummaryLevel level : SummaryLevel.values()) {
            int before = server.requests().size();
            capped.summarize(words(60), level);
            int last = server.requests().size() - 1;

            for (int i = before; i < last; i++) {
                assertThat(maxTokens(i)).as("part %s of %s", i, level).isEqualTo(partCap[level.ordinal()]);
            }
            assertThat(maxTokens(last)).as("combining request of %s", level).isEqualTo(ownCap[level.ordinal()]);
        }
    }

    @Test
    void theDefaultTokenCapsAreTheConfiguredDefaults() {
        for (SummaryLevel level : SummaryLevel.values()) {
            adapter().summarize("Photosynthesis turns light into sugar.", level);
        }

        assertThat(List.of(maxTokens(0), maxTokens(1), maxTokens(2), maxTokens(3)))
                .containsExactlyElementsOf(LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS);
    }

    @Test
    void aSummaryCutByTheTokenCapIsAskedForOnceMore() {
        server.thenCutShort("The summary stops in the mid").replyWith(user -> "The whole summary.");

        assertThat(adapter().summarize("Some text.", SummaryLevel.EXTRA_LONG)).isEqualTo("The whole summary.");

        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests().get(1)).as("the second request asks the same thing").isEqualTo(server.requests().get(0));
    }

    @Test
    void aSummaryCutByTheTokenCapFailsBadOutput() {
        server.replyWith(user -> "The summary stops in the mid").finishesWith("length");

        assertThatThrownBy(() -> adapter().summarize("Some text.", SummaryLevel.EXTRA_LONG))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> {
                    assertThat(f.code()).isEqualTo("BAD_OUTPUT");
                    assertThat(f.getMessage()).isEqualTo("The summary was cut short. Try a shorter level.");
                });
        assertThat(server.requests()).as("asked for once more, then given up on").hasSize(2);
    }

    @Test
    void aPartCutOnceByTheTokenCapIsAskedForAgainAndTheSummaryGoesOn() {
        server.thenCutShort("A part that stops in the mid").replyWith(user -> "S");

        assertThat(adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60), SummaryLevel.LONG))
                .isEqualTo("S");

        int last = server.requests().size() - 1;
        assertThat(server.userMessage(1)).as("the first part again").isEqualTo(server.userMessage(0));
        assertThat(server.userMessage(last)).as("no cut text reaches the combining request").doesNotContain("stops in the mid");
    }

    @Test
    void aPartCutTwiceByTheTokenCapFailsBadOutputBeforeAnyOtherPart() {
        server.replyWith(user -> "S").finishesWith("length");

        assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60), SummaryLevel.LONG))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
        assertThat(server.requests()).hasSize(2);
        assertThat(server.userMessage(1)).isEqualTo(server.userMessage(0));
    }

    @Test
    void longLevelsUseTheLongTimeout() {
        server.replyWith(user -> "S").delayBy(900);
        LocalTextGeneration adapter = adapter(Duration.ofMillis(300), Duration.ofSeconds(10), 10_000,
                LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS);

        assertThat(adapter.summarize("Some text.", SummaryLevel.LONG)).isEqualTo("S");
        assertThat(adapter.summarize("Some text.", SummaryLevel.EXTRA_LONG)).isEqualTo("S");
        for (SummaryLevel level : List.of(SummaryLevel.SHORT, SummaryLevel.MEDIUM)) {
            assertFailsWith("MODEL_TIMEOUT", () -> adapter.summarize("Some text.", level));
        }
    }

    @Test
    void aLongLevelSlowerThanTheLongTimeoutFailsWithModelTimeout() {
        server.delayBy(1500);
        LocalTextGeneration adapter = adapter(Duration.ofSeconds(10), Duration.ofMillis(300), 10_000,
                LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS);

        assertFailsWith("MODEL_TIMEOUT", () -> adapter.summarize("Some text.", SummaryLevel.EXTRA_LONG));
    }

    @Test
    void tokenCapsThatAreNotFourPositiveNumbersAreRefusedAtConstruction() {
        for (List<Integer> bad : List.of(List.of(250, 700, 1400), List.of(250, 700, 1400, 2400, 3000), List.of(250, 0, 1400, 2400))) {
            assertThatThrownBy(() -> adapter(Duration.ofSeconds(5), Duration.ofSeconds(5), 10_000, bad))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.summary.max-tokens");
        }
    }

    @Test
    void aMissingOrNonPositiveLongTimeoutIsRefusedAtConstruction() {
        for (Duration bad : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            assertThatThrownBy(() -> adapter(Duration.ofSeconds(5), bad, 10_000, LocalTextGeneration.DEFAULT_SUMMARY_MAX_TOKENS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.long-timeout");
        }
    }

    private static final String ARABIC ="صلح الحديبية هو اتفاق عقد بين المسلمين وقريش في السنة السادسة للهجرة";

    private String system(int request) {
        return server.requests().get(request).path("messages").path(0).path("content").asText();
    }

    @Test
    void anArabicTextIsSummarisedWithItsLanguageNamed() {
        adapter().summarize(ARABIC, SummaryLevel.MEDIUM);

        assertThat(system(0)).contains("answer in Arabic");
    }

    @Test
    void arabicReadFromPageImagesWithStrayLatinWordsStillNamesTheLanguage() {
        adapter().summarize("@ AlloSchool 9/13/2026\n" + ARABIC + " BE abo\nhttps://www.alloschool.com/element/1/doc",
                SummaryLevel.MEDIUM);

        assertThat(system(0)).contains("answer in Arabic");
    }

    @Test
    void aTextThatIsNotArabicGetsNoLanguageNamed() {
        adapter().summarize("Photosynthesis turns light into sugar.", SummaryLevel.MEDIUM);
        adapter().summarize("La dérivée d'une fonction mesure sa variation. Le mot arabe صلح veut dire paix.",
                SummaryLevel.MEDIUM);

        assertThat(system(0)).doesNotContain("Arabic");
        assertThat(system(1)).doesNotContain("Arabic");
    }

    @Test
    void everyRequestOfALongArabicSummaryNamesTheLanguage() {
        server.replyWith(user -> "S");

        adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize((ARABIC + " ").repeat(8), SummaryLevel.MEDIUM);

        assertThat(server.requests().size()).isGreaterThan(2);
        for (int i = 0; i < server.requests().size(); i++) {
            assertThat(system(i)).as("request %s", i).contains("answer in Arabic");
        }
    }

    @Test
    void exercisesFromAnArabicTextAreAskedForInArabic() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);

        adapter().generateExercises(ARABIC, 5);

        assertThat(system(0)).contains("answer in Arabic");
    }

    @Test
    void requests_switch_the_model_thinking_off() {
        adapter().summarize("Some text.", SummaryLevel.MEDIUM);

        assertThat(server.requests().get(0).path("chat_template_kwargs").path("enable_thinking").asBoolean(true))
                .isFalse();
    }

    @Test
    void the_model_name_is_the_configured_one() {
        assertThat(adapter().modelName()).isEqualTo(MODEL);
    }

    @Test
    void a_reply_has_its_think_block_removed() {
        server.replyWith(user -> "<think>let me think</think>\n\nThe summary.");

        assertThat(adapter().summarize("Some text.", SummaryLevel.MEDIUM)).isEqualTo("The summary.");
    }

    @Test
    void a_server_that_is_down_fails_with_MODEL_UNAVAILABLE() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        LocalTextGeneration down = adapter("http://127.0.0.1:" + closedPort, Duration.ofSeconds(5), 10_000);

        assertThatThrownBy(() -> down.summarize("Some text.", SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_UNAVAILABLE"));
    }

    @Test
    void a_server_slower_than_the_timeout_fails_with_MODEL_TIMEOUT() {
        server.delayBy(1500);
        LocalTextGeneration slow = adapter(server.baseUrl(), Duration.ofMillis(300), 10_000);

        assertThatThrownBy(() -> slow.summarize("Some text.", SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_TIMEOUT"));
    }

    @Test
    void an_error_status_fails_with_MODEL_ERROR() {
        server.failWith(500);

        assertThatThrownBy(() -> adapter().summarize("Some text.", SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_ERROR"));
    }

    private static String words(int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append("word").append(i).append(' ');
        }
        return text.toString().strip();
    }

    @Test
    void text_that_fits_in_one_chunk_costs_one_request() {
        adapter(server.baseUrl(), Duration.ofSeconds(5), 1000).summarize(words(20), SummaryLevel.MEDIUM);

        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void long_text_is_summarised_chunk_by_chunk_and_then_the_summaries_are_summarised() {
        server.replyWith(user -> "S");
        String text = words(60);

        String summary = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text, SummaryLevel.MEDIUM);

        int chunks = server.requests().size() - 1;
        assertThat(chunks).isGreaterThan(1);
        assertThat(summary).isEqualTo("S");
        assertThat(server.userMessage(chunks)).isEqualTo(String.join("\n\n", java.util.Collections.nCopies(chunks, "S")));
    }

    @Test
    void no_chunk_is_longer_than_the_limit_and_no_word_is_cut_in_two() {
        server.replyWith(user -> "S");
        String text = words(60);

        adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text, SummaryLevel.MEDIUM);

        StringBuilder sent = new StringBuilder();
        for (int i = 0; i < server.requests().size() - 1; i++) {
            String chunk = server.userMessage(i);
            assertThat(chunk.length()).isLessThanOrEqualTo(150);
            sent.append(chunk).append(' ');
        }
        assertThat(sent.toString().strip()).isEqualTo(text);
    }

    @Test
    void summaries_that_are_still_too_long_are_summarised_again() {
        server.replyWith(user -> user.startsWith("word") ? "x".repeat(100) : "short");
        String text = words(120);

        String summary = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(text, SummaryLevel.MEDIUM);

        assertThat(summary).isEqualTo("short");
        for (JsonNode request : server.requests()) {
            String user = request.path("messages").path(1).path("content").asText();
            assertThat(user.length()).isLessThanOrEqualTo(150);
        }
    }

    @Test
    void summaries_that_never_get_shorter_fail_with_BAD_OUTPUT() {
        server.replyWith(user -> "x".repeat(140));

        assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(120), SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    private static String exercisesJson(int n) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 1; i <= n; i++) {
            json.append(i > 1 ? "," : "").append("{\"question\":\"Q").append(i).append("?\",\"answer\":\"A").append(i).append("\"}");
        }
        return json.append("]").toString();
    }

    /** Answers any exercise request with as many exercises as it asks for ("exactly N"). */
    private static String asManyAsAsked(String user) {
        java.util.regex.Matcher asked = java.util.regex.Pattern.compile("exactly (\\d+)").matcher(user);
        return asked.find() ? exercisesJson(Integer.parseInt(asked.group(1))) : "no number asked";
    }

    @Test
    void exercises_are_read_from_a_json_array_of_question_and_answer() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter().generateExercises("Photosynthesis turns light into sugar.", 5);

        assertThat(exercises).hasSize(5);
        assertThat(exercises.get(0)).isEqualTo(new Exercise("Q1?", "A1"));
        assertThat(server.requests()).hasSize(1);
        assertThat(server.userMessage(0)).contains("Photosynthesis turns light into sugar.").contains("exactly 5");
    }

    @Test
    void a_json_array_inside_a_code_fence_is_accepted() {
        server.replyWith(user -> "Here you go:\n```json\n" + exercisesJson(5) + "\n```");

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
    }

    @Test
    void invalid_output_is_retried_once_and_the_retry_can_succeed() {
        server.thenReply("Sorry, I cannot do that.").replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter().generateExercises("Some text.", 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests()).hasSize(2);
        assertThat(server.userMessage(1)).contains("JSON");
    }

    @Test
    void output_that_is_invalid_twice_fails_with_BAD_OUTPUT_after_two_requests() {
        server.replyWith(user -> "not json at all");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
        assertThat(server.requests()).hasSize(2);
    }

    @Test
    void an_exercise_without_an_answer_is_invalid() {
        server.replyWith(user -> "[{\"question\":\"Q1?\"},{\"question\":\"Q2?\",\"answer\":\"A2\"}]");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 2))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void a_blank_question_is_invalid() {
        server.replyWith(user -> "[{\"question\":\" \",\"answer\":\"A1\"}]");

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 1))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void fewer_exercises_than_asked_for_is_invalid() {
        server.replyWith(user -> exercisesJson(3));

        assertThatThrownBy(() -> adapter().generateExercises("Some text.", 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void more_exercises_than_asked_for_are_cut_to_the_count() {
        server.replyWith(user -> exercisesJson(8));

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
    }

    @Test
    void long_text_gets_its_exercises_from_chunks_spread_across_the_text() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);
        String text = words(400);

        var exercises = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).generateExercises(text, 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests()).hasSize(5);
        assertThat(firstWordNumber(server.userMessage(0))).isLessThan(100);
        assertThat(firstWordNumber(server.userMessage(4))).isGreaterThan(300);
        for (int i = 1; i < 5; i++) {
            assertThat(firstWordNumber(server.userMessage(i))).isGreaterThan(firstWordNumber(server.userMessage(i - 1)));
        }
    }

    private static int firstWordNumber(String message) {
        java.util.regex.Matcher word = java.util.regex.Pattern.compile("word(\\d+)").matcher(message);
        assertThat(word.find()).isTrue();
        return Integer.parseInt(word.group(1));
    }

    @Test
    void short_text_in_few_chunks_shares_the_count_between_them() {
        server.replyWith(LocalTextGenerationTest::asManyAsAsked);

        var exercises = adapter(server.baseUrl(), Duration.ofSeconds(5), 150).generateExercises(words(30), 5);

        assertThat(exercises).hasSize(5);
        assertThat(server.requests().size()).isGreaterThan(1).isLessThanOrEqualTo(5);
    }

    // --- review fixes ---

    @Test
    void a_chunk_size_below_one_is_refused_at_construction() {
        for (int bad : new int[]{0, -5}) {
            assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), bad))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.chunk-chars");
        }
    }

    @Test
    void a_missing_or_non_positive_timeout_is_refused_at_construction() {
        for (Duration bad : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            assertThatThrownBy(() -> adapter(server.baseUrl(), bad, 10_000))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("generation.timeout");
        }
    }

    @Test
    void text_needing_more_chunks_than_the_limit_fails_with_TOO_LONG_before_any_request() {
        LocalTextGeneration limited = new LocalTextGeneration(server.baseUrl(), MODEL, Duration.ofSeconds(5), 100, 3,
                new ObjectMapper());
        String text = words(200);

        assertThatThrownBy(() -> limited.summarize(text, SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("TOO_LONG"));
        assertThatThrownBy(() -> limited.generateExercises(text, 5))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("TOO_LONG"));
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_reply_that_is_only_a_think_block_is_BAD_OUTPUT_not_a_blank_summary() {
        server.replyWith(user -> "<think>nothing else</think>");

        assertThatThrownBy(() -> adapter().summarize("Some text.", SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void a_blank_chunk_summary_does_not_reach_the_combining_request() {
        server.replyWith(user -> "  ");

        assertThatThrownBy(() -> adapter(server.baseUrl(), Duration.ofSeconds(5), 150).summarize(words(60), SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void an_unterminated_think_block_is_cut_off_and_never_stored() {
        server.replyWith(user -> "The summary.\n<think>reasoning that never ends");

        assertThat(adapter().summarize("Some text.", SummaryLevel.MEDIUM)).isEqualTo("The summary.");
    }

    @Test
    void a_reply_that_is_only_an_unterminated_think_block_is_BAD_OUTPUT() {
        server.replyWith(user -> "<think>reasoning that never ends");

        assertThatThrownBy(() -> adapter().summarize("Some text.", SummaryLevel.MEDIUM))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("BAD_OUTPUT"));
    }

    @Test
    void prose_with_brackets_before_the_array_does_not_hide_it() {
        server.replyWith(user -> "Here are 5 [easy] exercises: " + exercisesJson(5));

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void a_reference_after_the_array_does_not_hide_it() {
        server.replyWith(user -> exercisesJson(5) + "\n\nSee [1] for details.");

        assertThat(adapter().generateExercises("Some text.", 5)).hasSize(5);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void a_blank_base_url_is_refused_at_construction() {
        assertThatThrownBy(() -> adapter(" ", Duration.ofSeconds(5), 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("generation.base-url");
    }

    // --- chat ---

    private List<String> chatDeltas(LocalTextGeneration adapter, String question) {
        List<String> deltas = new ArrayList<>();
        adapter.chat(List.of(new ChatMessage("user", question)), deltas::add);
        return deltas;
    }

    private static void assertFailsWith(String code, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo(code));
    }

    @Test
    void chatRequestSetsStreamTrueAndFixedSystemPrompt() {
        adapter().chat(List.of(
                new ChatMessage("user", "What is a chloroplast?"),
                new ChatMessage("assistant", "A plant organelle."),
                new ChatMessage("user", "What does it do?")), delta -> { });

        JsonNode request = server.requests().get(0);
        assertThat(request.path("stream").asBoolean(false)).isTrue();
        assertThat(request.path("model").asText()).isEqualTo(MODEL);
        assertThat(request.path("chat_template_kwargs").path("enable_thinking").asBoolean(true)).isFalse();
        JsonNode messages = request.path("messages");
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText())
                .contains("study assistant").contains("language").contains("do not know");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("content").asText()).isEqualTo("What is a chloroplast?");
        assertThat(messages.get(2).path("role").asText()).isEqualTo("assistant");
        assertThat(messages.get(2).path("content").asText()).isEqualTo("A plant organelle.");
        assertThat(messages.get(3).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(3).path("content").asText()).isEqualTo("What does it do?");
    }

    @Test
    void chatPassesDeltasInOrder() {
        server.streamsChunks("Chloro", "plasts", " make", " sugar.");

        assertThat(chatDeltas(adapter(), "What do chloroplasts do?"))
                .containsExactly("Chloro", "plasts", " make", " sugar.");
    }

    @Test
    void chatStripsThinkBlocks() {
        server.streamsChunks("<think>The user wants", " the answer.</think>", "\n\n", "Sugar.");

        assertThat(chatDeltas(adapter(), "What do chloroplasts make?")).containsExactly("Sugar.");
    }

    @Test
    void chatStripsAThinkTagSplitAcrossChunks() {
        server.streamsChunks("Before.\n", "<thi", "nk>hidden</th", "ink>\nAfter.");

        assertThat(String.join("", chatDeltas(adapter(), "Go on."))).isEqualTo("Before.\n\nAfter.");
    }

    @Test
    void chatNeverSendsAWhitespaceOnlyDelta() {
        server.streamsChunks("\n\n", "Hello", "\n\n", "world", " ");

        assertThat(chatDeltas(adapter(), "Hi")).containsExactly("Hello", "\n\nworld");
    }

    @Test
    void chatReplyThatIsOnlyAThinkBlockFailsWithBadOutput() {
        server.streamsChunks("<think>nothing", " else</think>");

        assertFailsWith("BAD_OUTPUT", () -> chatDeltas(adapter(), "Hi"));
    }

    @Test
    void chatFailsWithModelUnavailableWhenServerIsDown() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        LocalTextGeneration down = adapter("http://127.0.0.1:" + closedPort, Duration.ofSeconds(5), 10_000);

        assertFailsWith("MODEL_UNAVAILABLE", () -> chatDeltas(down, "Hi"));
    }

    @Test
    void chatErrorStatusFailsWithModelError() {
        server.failWith(500);

        assertFailsWith("MODEL_ERROR", () -> chatDeltas(adapter(), "Hi"));
    }

    @Test
    void chatChunkThatReportsAnErrorFailsWithModelError() {
        server.streamsData("{\"error\":{\"message\":\"boom\"}}");

        assertFailsWith("MODEL_ERROR", () -> chatDeltas(adapter(), "Hi"));
    }

    @Test
    void chatThatEndsBeforeTheModelFinishesFailsWithModelErrorAfterTheTextSoFar() {
        server.streamsChunks("Partial ").cutsStreamShort(0);
        List<String> deltas = new ArrayList<>();

        assertFailsWith("MODEL_ERROR",
                () -> adapter().chat(List.of(new ChatMessage("user", "Hi")), deltas::add));
        assertThat(deltas).containsExactly("Partial");
    }

    @Test
    void chatThatStallsMidStreamFailsWithModelTimeoutInsteadOfHoldingTheCallerForever() {
        server.streamsChunks("Partial").cutsStreamShort(5000);
        LocalTextGeneration impatient = adapter(server.baseUrl(), Duration.ofMillis(400), 10_000);
        long start = System.nanoTime();

        assertFailsWith("MODEL_TIMEOUT", () -> chatDeltas(impatient, "Hi"));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void chatEndsAtOnceWhenTheCallingThreadIsInterruptedInsteadOfWaitingForTheTimeout() throws Exception {
        server.streamsChunks("Partial").cutsStreamShort(4000);
        LocalTextGeneration patient = adapter(server.baseUrl(), Duration.ofSeconds(30), 10_000);
        List<String> deltas = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                patient.chat(List.of(new ChatMessage("user", "Hi")), deltas::add);
            } catch (Throwable thrown) {
                outcome.set(thrown);
            }
        });
        caller.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> !deltas.isEmpty());

        caller.interrupt();
        caller.join(3000);

        assertThat(caller.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOfSatisfying(GenerationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo("MODEL_UNAVAILABLE"));
    }

    @Test
    void chatStopsReadingWhenTheConsumerThrows() {
        server.streamsChunks("a", "b", "c");
        List<String> seen = new ArrayList<>();
        RuntimeException clientGone = new RuntimeException("client gone");

        assertThatThrownBy(() -> adapter().chat(List.of(new ChatMessage("user", "Hi")), delta -> {
            seen.add(delta);
            if (delta.equals("b")) {
                throw clientGone;
            }
        })).isSameAs(clientGone);
        assertThat(seen).containsExactly("a", "b");
    }
}
