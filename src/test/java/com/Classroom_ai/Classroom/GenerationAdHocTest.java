package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.generation.GenerationFailure;
import com.Classroom_ai.Classroom.generation.GenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ad-hoc work shares the one generation thread with course jobs, and a caller waits only so long for its turn. */
@SpringBootTest(properties = "generation.timeout=1s")
@AutoConfigureMockMvc
class GenerationAdHocTest extends ToolsTestSupport {

    @Autowired GenerationService generation;
    @Autowired ObjectMapper mapper;

    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void freeTheGenerationThread() {
        release.countDown();
    }

    /** A job that holds the generation thread until the test ends. */
    private void occupyTheGenerationThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        generation.startAdHoc(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void runAdHocReturnsWhatTheWorkReturns() {
        assertThat(generation.runAdHoc(() -> "done")).isEqualTo("done");
    }

    @Test
    void runAdHocRethrowsTheWorksGenerationFailureWithItsCode() {
        assertThatThrownBy(() -> generation.runAdHoc(() -> {
            throw new GenerationFailure("BAD_OUTPUT", "The model made a mess.");
        })).isInstanceOfSatisfying(GenerationFailure.class, f -> {
            assertThat(f.code()).isEqualTo("BAD_OUTPUT");
            assertThat(f.getMessage()).isEqualTo("The model made a mess.");
        });
    }

    @Test
    void runAdHocQueuesBehindARunningJobAndGivesUpAfterTheTimeoutWithoutEverRunningTheWork() throws Exception {
        occupyTheGenerationThread();
        AtomicBoolean ran = new AtomicBoolean();
        long start = System.nanoTime();

        assertThatThrownBy(() -> generation.runAdHoc(() -> ran.getAndSet(true)))
                .isInstanceOfSatisfying(GenerationFailure.class, f -> assertThat(f.code()).isEqualTo("MODEL_TIMEOUT"));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isBetween(Duration.ofMillis(800), Duration.ofSeconds(4));
        release.countDown();
        assertThat(generation.runAdHoc(() -> "next in line")).isEqualTo("next in line");
        assertThat(ran).isFalse();
    }

    @Test
    void aToolsCallQueuedBehindALongJobGets504WithModelTimeout() throws Exception {
        occupyTheGenerationThread();

        mvc.perform(post("/api/tools/summaries").with(from(newIp())).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("text", "Photosynthesis turns light into energy."))))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("MODEL_TIMEOUT"));
    }
}
