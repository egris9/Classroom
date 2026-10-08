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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A chat reply can stay open for minutes, most of it queued behind other work, so an open chat must not hold a
 * database connection. The pool is made small here so that a connection held per chat shows at once.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=3",
        "spring.datasource.hikari.connection-timeout=1500"
})
@AutoConfigureMockMvc
@Import(ChatConnectionsTest.Waiting.class)
class ChatConnectionsTest extends ToolsTestSupport {

    /** A model that does not answer until the test lets it. */
    static class WaitingTextGeneration extends FakeTextGeneration {
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public void chat(List<ChatMessage> history, Consumer<String> onDelta) {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
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

            mvc.perform(get("/api/tools/trial").with(from(newIp())).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.signedIn").value(true));
        } finally {
            model.release.countDown();
            for (MvcResult chat : open) {
                chat.getRequest().getAsyncContext().complete();
            }
        }
    }
}
