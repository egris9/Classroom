package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.generation.Status;
import com.Classroom_ai.Classroom.generation.Summary;
import com.Classroom_ai.Classroom.generation.SummaryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A Summary is asked for at a level, from short to extra long; a request that names none gets a medium one. */
@SpringBootTest
@AutoConfigureMockMvc
class SummaryLevelTest extends GenerationTestSupport {

    /** Longer than the longest quote of the demo adapter, so each level gives a summary of its own length. */
    private static final String LESSON =
            "Photosynthesis turns light into chemical energy in the chloroplasts. ".repeat(9).strip();

    @Autowired SummaryRepository summaries;
    @Autowired CourseFileRepository files;

    private ResultActions summarise(String file, String auth, String body) throws Exception {
        return mvc.perform(post("/api/files/" + file + "/summaries").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void summaryWithNoBodyIsMedium() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("summaries", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.level").value("MEDIUM"));

        assertEquals("MEDIUM", JsonPath.read(awaitDone("summaries", file, c.teacher), "$[0].level"));
    }

    @Test
    void summaryWithABodyThatNamesNoLevelIsMedium() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        summarise(file, c.teacher, "{}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.level").value("MEDIUM"));
        summarise(file, c.teacher, "{\"level\":null}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.level").value("MEDIUM"));
    }

    @Test
    void summaryCarriesTheLevelAskedFor() throws Exception {
        Course c = course();
        List<Integer> lengths = new ArrayList<>();

        for (String level : List.of("SHORT", "MEDIUM", "LONG", "EXTRA_LONG")) {
            String file = c.uploadText(LESSON);

            summarise(file, c.teacher, "{\"level\":\"" + level + "\"}")
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.level").value(level));

            String list = awaitDone("summaries", file, c.teacher);
            assertEquals(level, JsonPath.read(list, "$[0].level"));
            lengths.add(((String) JsonPath.read(list, "$[0].text")).length());
        }

        assertThat(lengths).as("the model was asked at each level, so each summary is longer than the last")
                .isSorted().doesNotHaveDuplicates();
    }

    @Test
    void unknownLevelIs400() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        summarise(file, c.teacher, "{\"level\":\"HUGE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.teacher))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void aStudentMayPickALevel() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        summarise(file, c.student, "{\"level\":\"LONG\"}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.level").value("LONG"));

        String list = awaitDone("summaries", file, c.student);
        assertEquals("LONG", JsonPath.read(list, "$[0].level"));
        assertEquals(false, JsonPath.read(list, "$[0].published"));
    }

    @Test
    void anOldRowWithNoLevelReadsAsMedium() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        Summary old = new Summary();
        old.setFile(files.findById(Long.valueOf(file)).orElseThrow());
        old.setAuthor(newUser("Before"));
        old.setPublished(true);
        old.setStatus(Status.DONE);
        old.setContent("A summary written before summaries had a level.");
        summaries.save(old);

        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("A summary written before summaries had a level."))
                .andExpect(jsonPath("$[0].level").value("MEDIUM"));
    }
}
