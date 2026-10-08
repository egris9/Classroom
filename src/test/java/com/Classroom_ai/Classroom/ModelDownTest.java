package com.Classroom_ai.Classroom;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** With the model server stopped, a request ends FAILED with a code and a message the user can read. */
@SpringBootTest(properties = {
        "generation.adapter=local",
        "generation.base-url=http://127.0.0.1:1",
        "generation.model=any",
        "generation.timeout=2s"
})
@AutoConfigureMockMvc
class ModelDownTest extends GenerationTestSupport {

    @Test
    void a_summary_fails_with_MODEL_UNAVAILABLE_and_a_message() throws Exception {
        Course c = course();
        String file = c.uploadText("Photosynthesis turns light into chemical energy.");

        c.generate("summaries", file, c.teacher);

        String list = awaitStatus("summaries", file, c.teacher, "FAILED");
        assertEquals("MODEL_UNAVAILABLE", JsonPath.read(list, "$[0].failureCode"));
        assertFalse(JsonPath.<String>read(list, "$[0].failureMessage").isBlank());
    }

    @Test
    void an_exercise_set_fails_with_MODEL_UNAVAILABLE_and_a_message() throws Exception {
        Course c = course();
        String file = c.uploadText("Photosynthesis turns light into chemical energy.");

        c.generate("exercise-sets", file, c.teacher);

        String list = awaitStatus("exercise-sets", file, c.teacher, "FAILED");
        assertEquals("MODEL_UNAVAILABLE", JsonPath.read(list, "$[0].failureCode"));
        assertFalse(JsonPath.<String>read(list, "$[0].failureMessage").isBlank());
    }
}
