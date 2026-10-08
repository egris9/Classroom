package com.Classroom_ai.Classroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With the model server stopped, the tools answer with the reason, and a visitor's one try is still spent. */
@SpringBootTest(properties = {
        "generation.adapter=local",
        "generation.base-url=http://127.0.0.1:1",
        "generation.model=any",
        "generation.timeout=2s"
})
@AutoConfigureMockMvc
class ToolsModelDownTest extends ToolsTestSupport {

    @Autowired ObjectMapper mapper;

    private ResultActions summarise(String ip) throws Exception {
        return mvc.perform(post("/api/tools/summaries").with(from(ip)).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("text", "Photosynthesis turns light into energy."))));
    }

    @Test
    void modelDownGets422WithModelUnavailable() throws Exception {
        summarise(newIp()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void modelDownEndsTheChatStreamWithAnErrorEventCodeModelUnavailable() throws Exception {
        MvcResult result = mvc.perform(post("/api/tools/chat").with(from(newIp()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]}"))
                .andExpect(status().isOk()).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result.getAsyncResult(10_000);
        }

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("event:error").contains("\"code\":\"MODEL_UNAVAILABLE\"")
                .doesNotContain("event:delta").doesNotContain("event:done");
    }

    @Test
    void anonymousFailureStillCountsAsTheUse() throws Exception {
        String ip = newIp();
        summarise(ip).andExpect(status().isUnprocessableEntity());

        summarise(ip).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TRIAL_USED"));
    }
}
