package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.generation.ExerciseSet;
import com.Classroom_ai.Classroom.generation.ExerciseSetRepository;
import com.Classroom_ai.Classroom.generation.Status;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An Exercise set is asked for with types, a difficulty and a count, by any member of the course, and a member has
 * one request of each kind running per file at a time.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExerciseSetOptionsTest extends GenerationTestSupport {

    private static final String LESSON = "Photosynthesis turns light into chemical energy in the chloroplasts of plant "
            + "cells. Chlorophyll absorbs the light. Water and carbon dioxide become glucose and oxygen.";

    @MockitoSpyBean TextGeneration textGeneration;
    @Autowired ExerciseSetRepository exerciseSets;
    @Autowired CourseFileRepository files;

    private ResultActions ask(String file, String auth, String body) throws Exception {
        return mvc.perform(post("/api/files/" + file + "/exercise-sets").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Holds the model inside its first exercise or summary call until {@link #release} is called. */
    private final class Hold {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        Hold() {
            doAnswer(this::wait).when(textGeneration).generateExercises(anyString(), any());
            doAnswer(this::wait).when(textGeneration).summarize(anyString(), any());
        }

        private Object wait(org.mockito.invocation.InvocationOnMock call) throws Throwable {
            started.countDown();
            release.await(10, TimeUnit.SECONDS);
            return call.callRealMethod();
        }

        void started() throws InterruptedException {
            assertTrue(started.await(10, TimeUnit.SECONDS), "the model was never asked");
        }

        void release() {
            release.countDown();
        }
    }

    @Test
    void noBodyGivesFiveOpenMediumQuestions() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("exercise-sets", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.types[0]").value("OPEN"))
                .andExpect(jsonPath("$.types.length()").value(1))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.count").value(5));

        String list = awaitDone("exercise-sets", file, c.teacher);
        assertEquals(5, ((List<?>) JsonPath.read(list, "$[0].exercises")).size());
        assertThat((List<String>) JsonPath.read(list, "$[0].exercises[*].type")).containsOnly("OPEN");
    }

    @Test
    void optionsAreStoredAndReturned() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        ask(file, c.teacher, "{\"types\":[\"MULTIPLE_CHOICE\",\"TRUE_FALSE\"],\"difficulty\":\"HARD\",\"count\":7}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.types[0]").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.types[1]").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.difficulty").value("HARD"))
                .andExpect(jsonPath("$.count").value(7));

        String list = awaitDone("exercise-sets", file, c.teacher);
        assertEquals("HARD", JsonPath.read(list, "$[0].difficulty"));
        assertEquals(7, (int) JsonPath.read(list, "$[0].count"));
        assertEquals(7, ((List<?>) JsonPath.read(list, "$[0].exercises")).size());
        assertThat((List<String>) JsonPath.read(list, "$[0].exercises[*].type"))
                .containsOnly("MULTIPLE_CHOICE", "TRUE_FALSE");
        assertEquals(4, ((List<?>) JsonPath.read(list, "$[0].exercises[0].options")).size());
    }

    @Test
    void countOfTwoOrSixteenIs400() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        ask(file, c.teacher, "{\"count\":2}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        ask(file, c.teacher, "{\"count\":16}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", c.teacher))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void emptyTypesIs400() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        ask(file, c.teacher, "{\"types\":[]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        ask(file, c.teacher, "{\"types\":[null]}").andExpect(status().isBadRequest());
    }

    @Test
    void unknownTypeOrDifficultyIs400() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        ask(file, c.teacher, "{\"types\":[\"ESSAY\"]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        ask(file, c.teacher, "{\"difficulty\":\"IMPOSSIBLE\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void aSecondRequestWhileOneIsPendingIs409() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Hold hold = new Hold();
        try {
            c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
            hold.started();

            c.generate("exercise-sets", file, c.teacher)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GENERATION_PENDING"))
                    .andExpect(jsonPath("$.message").value(
                            "You already have a request running for this PDF. Wait for it to finish."));
            mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", c.teacher))
                    .andExpect(jsonPath("$.length()").value(1));
        } finally {
            hold.release();
        }
    }

    @Test
    void aSecondSummaryWhileOneIsPendingIs409() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Hold hold = new Hold();
        try {
            c.generate("summaries", file, c.student).andExpect(status().isAccepted());
            hold.started();

            c.generate("summaries", file, c.student)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GENERATION_PENDING"));
        } finally {
            hold.release();
        }
    }

    @Test
    void aPendingSummaryDoesNotBlockAnExerciseSet() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Hold hold = new Hold();
        try {
            c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());
            hold.started();

            c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
        } finally {
            hold.release();
        }
        awaitDone("exercise-sets", file, c.teacher);
    }

    @Test
    void anotherMembersPendingRequestDoesNotBlockMine() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Hold hold = new Hold();
        try {
            c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
            hold.started();

            c.generate("exercise-sets", file, c.student).andExpect(status().isAccepted());
        } finally {
            hold.release();
        }
        awaitDone("exercise-sets", file, c.student);
    }

    @Test
    void afterTheFirstFinishesANewRequestIsAccepted() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
        awaitDone("exercise-sets", file, c.teacher);

        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
    }

    @Test
    void aFailedRequestDoesNotBlockTheNextOne() throws Exception {
        Course c = course();
        String file = c.upload(pdfBytes(null));
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
        awaitStatus("exercise-sets", file, c.teacher, "FAILED");

        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
    }

    @Test
    void aModelFailureThatIsNotAnExceptionFailsTheSetAndDoesNotBlockTheNextRequest() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        doThrow(new StackOverflowError("deep reply")).when(textGeneration).generateExercises(anyString(), any());
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("exercise-sets", file, c.teacher, "FAILED");

        assertEquals("GENERATION_FAILED", JsonPath.read(list, "$[0].failureCode"));
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
    }

    @Test
    void anOldSetReadsAsOpenWithDefaults() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        ExerciseSet old = new ExerciseSet();
        old.setFile(files.findById(Long.valueOf(file)).orElseThrow());
        old.setAuthor(newUser("Before"));
        old.setPublished(true);
        old.setStatus(Status.DONE);
        old.setExercisesJson("[{\"question\":\"q\",\"answer\":\"a\"}]");
        exerciseSets.save(old);

        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", c.teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].types.length()").value(1))
                .andExpect(jsonPath("$[0].types[0]").value("OPEN"))
                .andExpect(jsonPath("$[0].difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$[0].count").value(1))
                .andExpect(jsonPath("$[0].exercises[0].type").value("OPEN"))
                .andExpect(jsonPath("$[0].exercises[0].question").value("q"))
                .andExpect(jsonPath("$[0].exercises[0].options").isEmpty());
    }
}
