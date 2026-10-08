package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.generation.Exercise;
import com.Classroom_ai.Classroom.generation.FakeTextGeneration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The summary and exercise endpoints that need no course: open to anyone, once, with smaller limits for visitors. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ToolsTest.Counting.class)
class ToolsTest extends ToolsTestSupport {

    private static final String LESSON = "Photosynthesis turns light into chemical energy in the chloroplasts of plants.";

    /** The fake model, counting how often it is asked, so a refusal can be shown to have cost no model call. */
    static class CountingTextGeneration extends FakeTextGeneration {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public String summarize(String text) {
            calls.incrementAndGet();
            return super.summarize(text);
        }

        @Override
        public List<Exercise> generateExercises(String text, int count) {
            calls.incrementAndGet();
            return super.generateExercises(text, count);
        }
    }

    @TestConfiguration
    static class Counting {
        @Bean
        @Primary
        CountingTextGeneration countingTextGeneration() {
            return new CountingTextGeneration();
        }
    }

    @Autowired CountingTextGeneration model;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void resetCount() {
        model.calls.set(0);
    }

    private ResultActions send(String path, String ip, String bearer, String text) throws Exception {
        var request = post(path).with(from(ip)).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("text", text)));
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        return mvc.perform(request);
    }

    private ResultActions summarise(String ip, String text) throws Exception {
        return send("/api/tools/summaries", ip, null, text);
    }

    private ResultActions upload(String path, String ip, String bearer, byte[] bytes) throws Exception {
        var request = multipart(path).file(new MockMultipartFile("file", "lesson.pdf", "application/pdf", bytes))
                .with(from(ip));
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        return mvc.perform(request);
    }

    private static byte[] pdfOfSize(int bytes) {
        byte[] pdf = new byte[bytes];
        byte[] magic = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, pdf, 0, magic.length);
        return pdf;
    }

    @Test
    void anonymousSummaryOfTextSucceedsOnce() throws Exception {
        summarise(newIp(), LESSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Photosynthesis turns light")))
                .andExpect(jsonPath("$.model").value("fake"));
    }

    @Test
    void anonymousExerciseSetOfPdfSucceeds() throws Exception {
        upload("/api/tools/exercise-sets", newIp(), null, pdfBytes(LESSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises", hasSize(5)))
                .andExpect(jsonPath("$.exercises[0].question").value(not(emptyString())))
                .andExpect(jsonPath("$.exercises[0].answer").value(not(emptyString())))
                .andExpect(jsonPath("$.model").value("fake"));
    }

    @Test
    void anonymousSummaryOfPdfSucceeds() throws Exception {
        upload("/api/tools/summaries", newIp(), null, pdfBytes(LESSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Photosynthesis turns light")));
    }

    @Test
    void secondAnonymousCallIs403TrialUsed() throws Exception {
        String ip = newIp();
        summarise(ip, LESSON).andExpect(status().isOk());

        send("/api/tools/exercise-sets", ip, null, LESSON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRIAL_USED"));
        assertThat(model.calls).hasValue(1);
    }

    @Test
    void signedInSummaryHasNoTrialLimit() throws Exception {
        String ip = newIp();
        String auth = bearer(newUser("Signed"));

        for (int i = 0; i < 3; i++) {
            send("/api/tools/summaries", ip, auth, LESSON).andExpect(status().isOk());
        }
    }

    @Test
    void anonymousOver20000CharsGets413WithoutCallingTheModelOrUsingTheTry() throws Exception {
        String ip = newIp();

        summarise(ip, "a".repeat(20_001))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("TEXT_TOO_LONG"));

        assertThat(model.calls).hasValue(0);
        summarise(ip, "a".repeat(20_000)).andExpect(status().isOk());
    }

    @Test
    void anonymousPdfOver5MBGets413WithoutCallingTheModelOrUsingTheTry() throws Exception {
        String ip = newIp();

        upload("/api/tools/summaries", ip, null, pdfOfSize(5 * 1024 * 1024 + 1))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

        assertThat(model.calls).hasValue(0);
        upload("/api/tools/summaries", ip, null, pdfBytes(LESSON)).andExpect(status().isOk());
    }

    @Test
    void anonymousPdfWithMoreThan20000CharsOfTextGets413WithoutCallingTheModel() throws Exception {
        upload("/api/tools/summaries", newIp(), null, pdfBytes("a".repeat(20_001)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("TEXT_TOO_LONG"));

        assertThat(model.calls).hasValue(0);
    }

    @Test
    void signedInUsersGetLargerLimitsThanAnAnonymousVisitor() throws Exception {
        String auth = bearer(newUser("Signed"));

        send("/api/tools/summaries", newIp(), auth, "a".repeat(50_000)).andExpect(status().isOk());
        send("/api/tools/summaries", newIp(), auth, "a".repeat(100_000)).andExpect(status().isOk());
        send("/api/tools/summaries", newIp(), auth, "a".repeat(100_001))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("TEXT_TOO_LONG"));
        summarise(newIp(), "a".repeat(50_000)).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void emptyTextGets400WithoutUsingTheTry() throws Exception {
        String ip = newIp();

        for (String empty : new String[]{"", "   ", "\n\t"}) {
            summarise(ip, empty).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mvc.perform(post("/api/tools/summaries").with(from(ip)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(model.calls).hasValue(0);
        summarise(ip, LESSON).andExpect(status().isOk());
    }

    @Test
    void malformedJsonGets400() throws Exception {
        mvc.perform(post("/api/tools/summaries").with(from(newIp())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void aBodyThatIsNeitherJsonNorAFormGets415() throws Exception {
        mvc.perform(post("/api/tools/summaries").with(from(newIp())).contentType(MediaType.TEXT_PLAIN)
                        .content(LESSON))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void aFormWithoutAFileGets400() throws Exception {
        mvc.perform(multipart("/api/tools/summaries").param("note", "no file here").with(from(newIp())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renamedTextFileGets415WithoutUsingTheTry() throws Exception {
        String ip = newIp();

        upload("/api/tools/summaries", ip, null, "just some text, not a PDF".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        assertThat(model.calls).hasValue(0);
        summarise(ip, LESSON).andExpect(status().isOk());
    }

    @Test
    void scannedPdfGets422WithNoTextAndStillSpendsTheTryBecauseParsingFollowsAdmission() throws Exception {
        String ip = newIp();
        upload("/api/tools/summaries", ip, null, pdfBytes(null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_TEXT"));

        assertThat(model.calls).hasValue(0);
        upload("/api/tools/summaries", ip, null, pdfBytes(LESSON)).andExpect(status().isForbidden());
    }
}
