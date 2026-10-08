package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.generation.ChatMessage;
import com.Classroom_ai.Classroom.generation.FakeTextGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A request to the AI tools can stay open for minutes, most of it queued behind other work, so waiting for the model
 * must not hold a database connection. The pool is made small here so that a connection held per request shows at
 * once.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=3",
        "spring.datasource.hikari.connection-timeout=1500"
})
@AutoConfigureMockMvc
@Import(ToolsConnectionsTest.Waiting.class)
class ToolsConnectionsTest extends ToolsTestSupport {

    /** A model that does not answer until the test lets it. */
    static class WaitingTextGeneration extends FakeTextGeneration {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger asked = new AtomicInteger();

        private void waitForTheTest() {
            asked.incrementAndGet();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public String summarize(String text) {
            waitForTheTest();
            return super.summarize(text);
        }

        @Override
        public void chat(List<ChatMessage> history, Consumer<String> onDelta) {
            waitForTheTest();
            super.chat(history, onDelta);
        }
    }

    @TestConfiguration
    static class Waiting {
        @Bean
        @Primary
        WaitingTextGeneration waitingTextGeneration() {
            return new WaitingTextGeneration();
        }
    }

    @Autowired WaitingTextGeneration model;

    private void assertTheDatabaseIsStillFree(String auth) throws Exception {
        mvc.perform(get("/api/tools/trial").with(from(newIp())).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedIn").value(true));
    }

    @Test
    void moreOpenChatsThanThereAreConnectionsStillLeaveTheDatabaseFree() throws Exception {
        String auth = bearer(newUser("Signed"));
        List<MvcResult> open = new ArrayList<>();
        try {
            for (int i = 0; i < 6; i++) {
                open.add(mvc.perform(post("/api/tools/chat").with(from(newIp())).header("Authorization", auth)
                                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                                .content("{\"messages\":[{\"role\":\"user\",\"content\":\"Hello " + i + "\"}]}"))
                        .andExpect(request().asyncStarted()).andReturn());
            }

            assertTheDatabaseIsStillFree(auth);
        } finally {
            model.release.countDown();
            for (MvcResult chat : open) {
                chat.getRequest().getAsyncContext().complete();
            }
        }
    }

    @Test
    void moreWaitingSummariesThanThereAreConnectionsStillLeaveTheDatabaseFree() throws Exception {
        String auth = bearer(newUser("Signed"));
        ExecutorService callers = Executors.newFixedThreadPool(6);
        List<Future<Integer>> waiting = new ArrayList<>();
        try {
            for (int i = 0; i < 6; i++) {
                String body = "{\"text\":\"Photosynthesis turns light into energy " + i + ".\"}";
                waiting.add(callers.submit(() -> mvc.perform(post("/api/tools/summaries").with(from(newIp()))
                                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus()));
            }
            await().atMost(Duration.ofSeconds(10)).until(() -> model.asked.get() >= 1);
            Thread.sleep(500);

            assertTheDatabaseIsStillFree(auth);
        } finally {
            model.release.countDown();
        }
        for (Future<Integer> summary : waiting) {
            assertThat(summary.get()).isEqualTo(200);
        }
        callers.shutdown();
    }
}
