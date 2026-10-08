package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.generation.Exercise;
import com.Classroom_ai.Classroom.generation.GenerationService;
import com.Classroom_ai.Classroom.generation.Summary;
import com.Classroom_ai.Classroom.generation.SummaryRepository;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S4 Generation, fake model. Summaries and Exercise sets are requested on a CourseFile, run in the background
 * through the {@link TextGeneration} seam, and polled with GET. A Teacher's result is published to the course;
 * a Student's is private to that Student.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GenerationTest extends GenerationTestSupport {

    private static final String LESSON = "Photosynthesis turns light into chemical energy in the chloroplasts of plant cells.";

    @MockitoSpyBean TextGeneration textGeneration;
    @Autowired GenerationService generation;
    @Autowired SummaryRepository summaries;
    @Autowired CourseFileRepository files;

    // --- the fake adapter ---

    @Test
    void fakeAdapter_isDeterministic_andNamesItself() {
        assertEquals(textGeneration.summarize(LESSON), textGeneration.summarize(LESSON));
        List<Exercise> exercises = textGeneration.generateExercises(LESSON, 5);
        assertEquals(5, exercises.size());
        assertEquals(exercises, textGeneration.generateExercises(LESSON, 5));
        assertEquals("fake", textGeneration.modelName());
    }

    // --- POST /api/files/{id}/summaries (member) ---

    @Test
    void summary_byTeacher_is202ThenDoneAndPublished() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("summaries", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        String list = awaitDone("summaries", file, c.teacher);
        assertEquals("DONE", JsonPath.read(list, "$[0].status"));
        assertEquals(true, JsonPath.read(list, "$[0].published"));
        assertEquals("fake", JsonPath.read(list, "$[0].model"));
        assertFalse(((String) JsonPath.read(list, "$[0].text")).isBlank());
    }

    @Test
    void summary_byTeacher_isVisibleToAStudentOfTheCourse() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());
        awaitDone("summaries", file, c.teacher);

        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].published").value(true));
    }

    @Test
    void summary_byStudent_isPrivateToThatStudent() throws Exception {
        Course c = course();
        String otherStudent = c.enrol("Other");
        String file = c.uploadText(LESSON);

        c.generate("summaries", file, c.student).andExpect(status().isAccepted());
        String own = awaitDone("summaries", file, c.student);
        assertEquals(false, JsonPath.read(own, "$[0].published"));

        for (String other : List.of(c.teacher, otherStudent)) {
            mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", other))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void summary_nonMember_is403() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        String outsider = bearer(newUser("Outsider"));

        c.generate("summaries", file, outsider).andExpect(status().isForbidden());
        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_signedOut_is401() throws Exception {
        mvc.perform(post("/api/files/1/summaries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/files/1/summaries")).andExpect(status().isUnauthorized());
    }

    @Test
    void summary_ofMissingFile_is404() throws Exception {
        Course c = course();
        c.generate("summaries", "999999", c.teacher).andExpect(status().isNotFound());
        mvc.perform(get("/api/files/999999/summaries").header("Authorization", c.teacher))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/files/{id}/exercise-sets (teacher) ---

    @Test
    void exerciseSet_byTeacher_hasFiveQuestionsWithModelAnswers() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("exercise-sets", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        String list = awaitDone("exercise-sets", file, c.teacher);
        assertEquals(true, JsonPath.read(list, "$[0].published"));
        assertEquals("fake", JsonPath.read(list, "$[0].model"));
        assertEquals(5, ((List<?>) JsonPath.read(list, "$[0].exercises")).size());
        assertFalse(((String) JsonPath.read(list, "$[0].exercises[0].question")).isBlank());
        assertFalse(((String) JsonPath.read(list, "$[0].exercises[0].answer")).isBlank());
    }

    @Test
    void exerciseSet_byTeacher_isVisibleToAStudent() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
        awaitDone("exercise-sets", file, c.teacher);

        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", c.student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void exerciseSet_byStudent_is403() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("exercise-sets", file, c.student).andExpect(status().isForbidden());
    }

    @Test
    void exerciseSet_nonMemberAndSignedOut_are403And401() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        String outsider = bearer(newUser("Outsider"));

        c.generate("exercise-sets", file, outsider).andExpect(status().isForbidden());
        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", outsider))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/files/" + file + "/exercise-sets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/files/" + file + "/exercise-sets")).andExpect(status().isUnauthorized());
    }

    @Test
    void exerciseSet_ofMissingFile_is404() throws Exception {
        Course c = course();
        c.generate("exercise-sets", "999999", c.teacher).andExpect(status().isNotFound());
    }

    // --- failures the user must see ---

    @Test
    void scannedPdfWithNoText_failsWithNoText() throws Exception {
        Course c = course();
        String file = c.upload(pdfBytes(null));

        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("summaries", file, c.teacher, "FAILED");
        assertEquals("NO_TEXT", JsonPath.read(list, "$[0].failureCode"));
        assertFalse(((String) JsonPath.read(list, "$[0].failureMessage")).isBlank());
    }

    @Test
    void exerciseSetOnScannedPdf_failsWithNoText() throws Exception {
        Course c = course();
        String file = c.upload(pdfBytes(null));

        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("exercise-sets", file, c.teacher, "FAILED");
        assertEquals("NO_TEXT", JsonPath.read(list, "$[0].failureCode"));
    }

    @Test
    void corruptPdf_failsWithPdfUnreadable() throws Exception {
        Course c = course();
        String file = c.upload("%PDF-1.4 this is not a real document".getBytes(StandardCharsets.UTF_8));

        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("summaries", file, c.teacher, "FAILED");
        assertEquals("PDF_UNREADABLE", JsonPath.read(list, "$[0].failureCode"));
    }

    @Test
    void requestLeftPendingByARestart_isFailedAsInterrupted() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Summary stuck = new Summary();
        stuck.setFile(files.findById(Long.valueOf(file)).orElseThrow());
        stuck.setAuthor(users.findAll().stream().findFirst().orElseThrow());
        stuck.setPublished(true);
        summaries.save(stuck);

        generation.failInterrupted();

        String list = mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.teacher))
                .andReturn().getResponse().getContentAsString();
        assertEquals("FAILED", JsonPath.read(list, "$[0].status"));
        assertEquals("INTERRUPTED", JsonPath.read(list, "$[0].failureCode"));
    }

    // --- the queue ---

    @Test
    void requests_run_one_at_a_time_and_the_others_wait_pending() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger mostAtOnce = new AtomicInteger();
        doAnswer(call -> {
            mostAtOnce.accumulateAndGet(running.incrementAndGet(), Math::max);
            firstStarted.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
                return call.callRealMethod();
            } finally {
                running.decrementAndGet();
            }
        }).when(textGeneration).summarize(anyString());

        for (int i = 0; i < 3; i++) {
            c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());
        }
        assertTrue(firstStarted.await(10, TimeUnit.SECONDS));
        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.teacher))
                .andExpect(jsonPath("$[?(@.status=='PENDING')]", hasSize(3)));
        release.countDown();

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).untilAsserted(() ->
                mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.teacher))
                        .andExpect(jsonPath("$[?(@.status=='DONE')]", hasSize(3))));
        assertEquals(1, mostAtOnce.get());
    }
}
